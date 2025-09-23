package io.temporal.agent.workflow;

import io.temporal.agent.activities.ToolActivities;
import io.temporal.agent.model.conversation.ConversationHistory;
import io.temporal.agent.model.tools.AgentGoal;
import io.temporal.agent.model.workflow.AgentGoalWorkflowParams;
import io.temporal.agent.model.workflow.CombinedInput;
import io.temporal.agent.model.workflow.EnvLookupInput;
import io.temporal.agent.model.workflow.EnvLookupOutput;
import io.temporal.agent.model.workflow.NextStep;
import io.temporal.agent.model.workflow.ToolDecision;
import io.temporal.agent.model.workflow.ToolPromptInput;
import io.temporal.agent.model.workflow.ValidationInput;
import io.temporal.agent.model.workflow.ValidationResult;
import io.temporal.agent.prompt.AgentPromptGenerator;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AgentGoalWorkflowImpl implements AgentGoalWorkflow {

    private final ToolActivities llmActivities;
    private final ToolActivities toolActivities;

    private final ConversationHistory conversationHistory = new ConversationHistory();
    private final Deque<String> promptQueue = new ArrayDeque<>();
    private final List<Map<String, Object>> toolResults = new ArrayList<>();

    private String conversationSummary;
    private boolean chatEnded;
    private boolean confirmed;
    private boolean waitingForConfirm;
    private ToolDecision toolDecision;
    private AgentGoal goal;
    private boolean showToolArgsConfirmation = true;
    private boolean multiGoalMode;
    private Map<String, Object> mcpToolsInfo;

    public AgentGoalWorkflowImpl() {
        ActivityOptions llmOptions = ActivityOptions.newBuilder()
                .setScheduleToCloseTimeout(Duration.ofMinutes(5))
                .setStartToCloseTimeout(Duration.ofSeconds(40))
                .build();
        ActivityOptions toolOptions = ActivityOptions.newBuilder()
                .setScheduleToCloseTimeout(Duration.ofMinutes(5))
                .setStartToCloseTimeout(Duration.ofSeconds(20))
                .build();
        this.llmActivities = Workflow.newActivityStub(ToolActivities.class, llmOptions);
        this.toolActivities = Workflow.newActivityStub(ToolActivities.class, toolOptions);
    }

    @Override
    public String run(CombinedInput input) {
        this.goal = ensureGoal(input != null ? input.getAgentGoal() : null);
        AgentGoalWorkflowParams params = input != null ? input.getToolParams() : null;

        lookupWorkflowEnvSettings();
        this.mcpToolsInfo = null;
        if (this.goal.getMcpServerDefinition() != null) {
            this.mcpToolsInfo = llmActivities.listMcpTools(this.goal.getMcpServerDefinition(), this.goal.getMcpServerDefinition().getIncludedTools());
        }

        if (params != null) {
            if (params.getConversationSummary() != null) {
                this.conversationSummary = params.getConversationSummary();
                conversationHistory.addMessage("conversation_summary", params.getConversationSummary());
            }
            if (params.getPromptQueue() != null) {
                promptQueue.addAll(params.getPromptQueue());
            }
        }

        enqueueStarterPrompt();

        String currentTool = null;
        while (true) {
            Workflow.await(() -> !promptQueue.isEmpty() || chatEnded || (confirmed && waitingForConfirm));

            if (chatEnded) {
                Workflow.getLogger(AgentGoalWorkflowImpl.class).info("Chat ended, returning conversation history");
                return conversationHistory.getMessages().toString();
            }

            if (confirmed && waitingForConfirm && currentTool != null && toolDecision != null) {
                waitingForConfirm = executeTool(currentTool);
                if (!waitingForConfirm) {
                    currentTool = null;
                }
                continue;
            }

            if (!promptQueue.isEmpty()) {
                String prompt = promptQueue.pollFirst();
                if (prompt == null) {
                    continue;
                }

                if (isUserPrompt(prompt)) {
                    conversationHistory.addMessage("user", prompt);
                    if (this.goal != null) {
                        ValidationResult validation = llmActivities.agentValidatePrompt(
                                new ValidationInput(prompt, conversationHistory, this.goal));
                        if (!validation.isValidationResult()) {
                            conversationHistory.addMessage("agent", validation.getValidationFailedReason());
                            continue;
                        }
                    }
                }

                String context = AgentPromptGenerator.generateGenAiPrompt(this.goal, conversationHistory, multiGoalMode, toolDecision, mcpToolsInfo);
                Map<String, Object> rawDecision = llmActivities.agentToolPlanner(new ToolPromptInput(prompt, context));
                this.toolDecision = ToolDecision.fromRawMap(rawDecision);
                this.toolDecision.ensureForceConfirm(showToolArgsConfirmation);
                NextStep nextStep = this.toolDecision.getNext();
                currentTool = this.toolDecision.getTool();

                if (nextStep == NextStep.CONFIRM && currentTool != null) {
                    List<String> missingArgs = findMissingArgs(this.toolDecision.getArgsOrEmpty());
                    if (!missingArgs.isEmpty()) {
                        promptQueue.add(AgentPromptGenerator.generateMissingArgsPrompt(currentTool, missingArgs));
                        continue;
                    }
                    waitingForConfirm = true;
                    if (!showToolArgsConfirmation) {
                        confirmed = true;
                    } else {
                        confirmed = false;
                    }
                } else if (nextStep == NextStep.PICK_NEW_GOAL) {
                    boolean alreadySelectingGoal = isGoalSelection(this.goal);
                    this.goal = ensureGoal(null);
                    this.mcpToolsInfo = null;
                    if (!alreadySelectingGoal) {
                        enqueueStarterPrompt();
                    }
                    waitingForConfirm = false;
                    confirmed = false;
                    currentTool = null;
                } else if (nextStep == NextStep.DONE) {
                    conversationHistory.addMessage("agent", this.toolDecision.toRawMap());
                    return conversationHistory.getMessages().toString();
                }

                conversationHistory.addMessage("agent", this.toolDecision.toRawMap());
            }
        }
    }

    @Override
    public void submitUserPrompt(String prompt) {
        Workflow.getLogger(AgentGoalWorkflowImpl.class).info("Received user prompt: {}", prompt);
        if (chatEnded) {
            return;
        }
        promptQueue.add(prompt);
    }

    @Override
    public void confirmToolExecution(boolean confirmed) {
        Workflow.getLogger(AgentGoalWorkflowImpl.class).info("User confirmation received: {}", confirmed);
        if (confirmed) {
            this.confirmed = true;
        }
    }

    @Override
    public void endChat() {
        Workflow.getLogger(AgentGoalWorkflowImpl.class).info("Chat end signal received");
        this.chatEnded = true;
    }

    @Override
    public void selectGoal(AgentGoal goal) {
        Workflow.getLogger(AgentGoalWorkflowImpl.class).info("Selecting goal {}", goal != null ? goal.getId() : "null");
        this.goal = ensureGoal(goal);
        this.mcpToolsInfo = null;
        if (this.goal.getMcpServerDefinition() != null) {
            this.mcpToolsInfo = llmActivities.listMcpTools(this.goal.getMcpServerDefinition(), this.goal.getMcpServerDefinition().getIncludedTools());
        }
        enqueueStarterPrompt();
    }

    @Override
    public ConversationHistory getConversationHistory() {
        return conversationHistory;
    }

    @Override
    public ToolDecision getToolDecision() {
        return toolDecision;
    }

    @Override
    public AgentGoal getCurrentGoal() {
        return goal;
    }

    @Override
    public boolean isChatEnded() {
        return chatEnded;
    }

    private void lookupWorkflowEnvSettings() {
        EnvLookupOutput env = llmActivities.getWorkflowEnvVars(new EnvLookupInput("SHOW_CONFIRM", true));
        this.showToolArgsConfirmation = env.isShowConfirm();
        this.multiGoalMode = env.isMultiGoalMode();
    }

    private void enqueueStarterPrompt() {
        if (goal == null) {
            return;
        }

        String starterPrompt = goal.getStarterPrompt();
        if (starterPrompt == null || starterPrompt.isBlank()) {
            return;
        }
        String sanitizedPrompt = starterPrompt.startsWith("###")
                ? starterPrompt
                : "### " + starterPrompt.trim();
        promptQueue.add(sanitizedPrompt);
    }

    private boolean isUserPrompt(String prompt) {
        return prompt != null && !prompt.startsWith("###");
    }

    private boolean executeTool(String currentTool) {
        Workflow.getLogger(AgentGoalWorkflowImpl.class).info("Executing tool {}", currentTool);
        confirmed = false;
        Map<String, Object> confirmedToolData = new HashMap<>(toolDecision.toRawMap());
        confirmedToolData.put("next", "user_confirmed_tool_run");
        conversationHistory.addMessage("user_confirmed_tool_run", confirmedToolData);

        Map<String, Object> result = toolActivities.runTool(currentTool, toolDecision.getArgsOrEmpty());
        toolResults.add(result);
        conversationHistory.addMessage("tool_result", result);
        promptQueue.add(AgentPromptGenerator.generateToolCompletionPrompt(currentTool, result));
        return false;
    }

    private AgentGoal ensureGoal(AgentGoal candidate) {
        return candidate != null ? candidate : createGoalSelectionGoal();
    }

    private boolean isGoalSelection(AgentGoal candidate) {
        return candidate != null && "goal_choose_agent_type".equals(candidate.getId());
    }
    private AgentGoal createGoalSelectionGoal() {
        AgentGoal selection = new AgentGoal();
        selection.setId("goal_choose_agent_type");
        selection.setCategoryTag("core");
        selection.setAgentName("Select Agent Type");
        selection.setAgentFriendlyDescription("Help the user choose which agent to interact with.");
        selection.setDescription(String.join(" ",
                "Act as an agent concierge who learns what the user wants to achieve and pairs them with the best catalog goal.",
                "Ask about their objective, timing, and any constraints, then recommend the most relevant agent.",
                "Offer to switch them into that agent once they agree.",
                "Keep your tone warm, concise, and focused on moving them forward."));
        selection.setStarterPrompt(String.join(" ",
                "Warmly welcome the user, explain that you can match them with the right assistant from our catalog,",
                "and ask them to share what they need help accomplishing."));
        selection.setExampleConversationHistory(String.join("\n",
                "user: I want help tracking an order",
                "agent: Happy to help! I can bring in our order tracking assistant. Are you trying to check a specific order?",
                "user: Yes, order 102.",
                "agent: Perfect. I'll hand things off to the order status assistant so we can look that up together."));
        return selection;
    }

    private List<String> findMissingArgs(Map<String, Object> args) {
        return args.entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
