package io.temporal.agent.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import io.temporal.agent.activities.ToolActivities;
import io.temporal.agent.goals.GoalRegistry;
import io.temporal.agent.model.conversation.ConversationHistory;
import io.temporal.agent.model.conversation.ConversationMessage;
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
import io.temporal.agent.tools.EcommerceToolsConfiguration;
import io.temporal.agent.tools.ToolRegistry;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentWorkflowE2ETest {

    private static final String TASK_QUEUE = "agent-e2e-test";

    @Test
    void listOrdersFlowCompletesAfterToolRun() {
        ToolRegistry toolRegistry = new ToolRegistry();
        new EcommerceToolsConfiguration(toolRegistry);
        GoalRegistry goalRegistry = new GoalRegistry(toolRegistry);

        StubToolActivities activities = new StubToolActivities(toolRegistry);
        activities.enqueuePlannerResponse(plannerResponse(NextStep.QUESTION, null, null,
                "Hello! I can help with your orders."));
        Map<String, Object> listArgs = new HashMap<>();
        listArgs.put("email", "matt.murdock@nelsonmurdock.com");
        listArgs.put("limit", 2);
        activities.enqueuePlannerResponse(plannerResponse(NextStep.CONFIRM, "ListOrders", listArgs,
                "Great, I'll fetch your recent orders."));
        activities.enqueuePlannerResponse(plannerResponse(NextStep.DONE, null, null,
                "I've shared the latest results."));

        try (TestWorkflowEnvironment environment = TestWorkflowEnvironment.newInstance()) {
            Worker worker = environment.newWorker(TASK_QUEUE);
            worker.registerWorkflowImplementationTypes(AgentGoalWorkflowImpl.class);
            worker.registerActivitiesImplementations(activities);
            environment.start();

            WorkflowClient client = environment.getWorkflowClient();
            AgentGoalWorkflow workflow = client.newWorkflowStub(AgentGoalWorkflow.class,
                    WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
            AgentGoal goal = goalRegistry.findGoal("goal_ecomm_list_orders");
            CombinedInput input = new CombinedInput(new AgentGoalWorkflowParams(), goal);
            WorkflowClient.start(workflow::run, input);
            WorkflowStub stub = WorkflowStub.fromTyped(workflow);

            environment.sleep(Duration.ofSeconds(1));

            workflow.submitUserPrompt("Please list recent orders for matt.murdock@nelsonmurdock.com with a limit of 2");
            environment.sleep(Duration.ofSeconds(1));

            ToolDecision decision = workflow.getToolDecision();
            assertThat(decision).isNotNull();
            assertThat(decision.getTool()).isEqualTo("ListOrders");
            assertThat(decision.getArgs()).containsEntry("email", "matt.murdock@nelsonmurdock.com");

            workflow.confirmToolExecution(true);

            String result = stub.getResult(String.class);
            assertThat(result).contains("ListOrders");
            assertThat(result).contains("orders");
        }
    }

    @Test
    void missingArgsPromptsUserBeforeToolRun() {
        ToolRegistry toolRegistry = new ToolRegistry();
        new EcommerceToolsConfiguration(toolRegistry);
        GoalRegistry goalRegistry = new GoalRegistry(toolRegistry);

        StubToolActivities activities = new StubToolActivities(toolRegistry);
        activities.enqueuePlannerResponse(plannerResponse(NextStep.QUESTION, null, null,
                "Hi there! Let's get started."));
        Map<String, Object> incompleteArgs = new HashMap<>();
        incompleteArgs.put("email", null);
        incompleteArgs.put("limit", null);
        activities.enqueuePlannerResponse(plannerResponse(NextStep.CONFIRM, "ListOrders", incompleteArgs,
                "I'll look up those orders."));
        activities.enqueuePlannerResponse(plannerResponse(NextStep.QUESTION, null, null,
                "Could you share the email on the account?"));
        Map<String, Object> completeArgs = new HashMap<>();
        completeArgs.put("email", "matt.murdock@nelsonmurdock.com");
        completeArgs.put("limit", 1);
        activities.enqueuePlannerResponse(plannerResponse(NextStep.CONFIRM, "ListOrders", completeArgs,
                "Thanks! Fetching the latest order now."));
        activities.enqueuePlannerResponse(plannerResponse(NextStep.DONE, null, null,
                "That's everything I found."));

        try (TestWorkflowEnvironment environment = TestWorkflowEnvironment.newInstance()) {
            Worker worker = environment.newWorker(TASK_QUEUE);
            worker.registerWorkflowImplementationTypes(AgentGoalWorkflowImpl.class);
            worker.registerActivitiesImplementations(activities);
            environment.start();

            WorkflowClient client = environment.getWorkflowClient();
            AgentGoalWorkflow workflow = client.newWorkflowStub(AgentGoalWorkflow.class,
                    WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
            AgentGoal goal = goalRegistry.findGoal("goal_ecomm_list_orders");
            CombinedInput input = new CombinedInput(new AgentGoalWorkflowParams(), goal);
            WorkflowClient.start(workflow::run, input);
            WorkflowStub stub = WorkflowStub.fromTyped(workflow);

            environment.sleep(Duration.ofSeconds(1));

            workflow.submitUserPrompt("I need help listing orders");
            environment.sleep(Duration.ofSeconds(1));

            assertThat(activities.getSeenPrompts())
                    .anyMatch(prompt -> prompt.contains("missing required args"));

            workflow.submitUserPrompt("The email is matt.murdock@nelsonmurdock.com and just show one order");
            environment.sleep(Duration.ofSeconds(1));

            ToolDecision decision = workflow.getToolDecision();
            assertThat(decision).isNotNull();
            assertThat(decision.getTool()).isEqualTo("ListOrders");
            assertThat(decision.getArgs()).containsEntry("limit", 1);

            workflow.confirmToolExecution(true);

            String result = stub.getResult(String.class);
            assertThat(result).contains("ListOrders");
            assertThat(activities.getSeenPrompts())
                    .anyMatch(prompt -> prompt.startsWith("### The 'ListOrders' tool completed"));
        }
    }

    @Test
    void pickNewGoalResetsToSelectionGoalWithoutDuplicateMessages() {
        ToolRegistry toolRegistry = new ToolRegistry();
        new EcommerceToolsConfiguration(toolRegistry);
        GoalRegistry goalRegistry = new GoalRegistry(toolRegistry);

        StubToolActivities activities = new StubToolActivities(toolRegistry);
        activities.enqueuePlannerResponse(plannerResponse(NextStep.QUESTION, null, null,
                "Hello! I'm ready to help."));
        String pickResponse = "Let's choose the best agent for this request.";
        activities.enqueuePlannerResponse(plannerResponse(NextStep.PICK_NEW_GOAL, null, null, pickResponse));
        activities.enqueuePlannerResponse(plannerResponse(NextStep.QUESTION, null, null,
                "Great! Tell me a bit about what you need so I can suggest an agent."));
        activities.enqueuePlannerResponse(plannerResponse(NextStep.QUESTION, null, null,
                "Here are some ideas to get us started."));
        activities.setFailOnNullGoalValidation(true);

        try (TestWorkflowEnvironment environment = TestWorkflowEnvironment.newInstance()) {
            Worker worker = environment.newWorker(TASK_QUEUE);
            worker.registerWorkflowImplementationTypes(AgentGoalWorkflowImpl.class);
            worker.registerActivitiesImplementations(activities);
            environment.start();

            WorkflowClient client = environment.getWorkflowClient();
            AgentGoalWorkflow workflow = client.newWorkflowStub(AgentGoalWorkflow.class,
                    WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
            AgentGoal goal = goalRegistry.findGoal("goal_ecomm_list_orders");
            CombinedInput input = new CombinedInput(new AgentGoalWorkflowParams(), goal);
            WorkflowClient.start(workflow::run, input);

            environment.sleep(Duration.ofSeconds(1));

            workflow.submitUserPrompt("Help me choose a different goal");
            environment.sleep(Duration.ofSeconds(1));

            workflow.submitUserPrompt("What options do I have?");
            environment.sleep(Duration.ofSeconds(1));

            ConversationHistory history = workflow.getConversationHistory();
            long pickMessages = history.getMessages().stream()
                    .filter(msg -> "agent".equals(msg.type()))
                    .map(ConversationMessage::response)
                    .filter(resp -> resp instanceof Map<?, ?> map && pickResponse.equals(map.get("response")))
                    .count();
            assertThat(pickMessages).isEqualTo(1L);

            AgentGoal currentGoal = workflow.getCurrentGoal();
            assertThat(currentGoal).isNotNull();
            assertThat(currentGoal.getId()).isEqualTo("goal_choose_agent_type");
        }
    }

    private static Map<String, Object> plannerResponse(NextStep step, String tool, Map<String, Object> args, String response) {
        Map<String, Object> map = new HashMap<>();
        map.put("next", step.getJsonValue());
        map.put("tool", tool);
        if (args != null) {
            map.put("args", new HashMap<>(args));
        }
        map.put("response", response);
        return map;
    }

    private static final class StubToolActivities implements ToolActivities {

        private final Deque<Map<String, Object>> plannerResponses = new ArrayDeque<>();
        private final List<String> seenPrompts = new ArrayList<>();
        private final ToolRegistry toolRegistry;
        private boolean failOnNullGoalValidation;

        private StubToolActivities(ToolRegistry toolRegistry) {
            this.toolRegistry = toolRegistry;
        }

        void enqueuePlannerResponse(Map<String, Object> response) {
            plannerResponses.add(response);
        }

        List<String> getSeenPrompts() {
            return seenPrompts;
        }

        void setFailOnNullGoalValidation(boolean failOnNullGoalValidation) {
            this.failOnNullGoalValidation = failOnNullGoalValidation;
        }

        @Override
        public ValidationResult agentValidatePrompt(ValidationInput input) {
            if (failOnNullGoalValidation && input.getAgentGoal() == null) {
                throw new IllegalStateException("Validation invoked without a goal");
            }
            return new ValidationResult(true, Map.of());
        }

        @Override
        public Map<String, Object> agentToolPlanner(ToolPromptInput input) {
            seenPrompts.add(input.getPrompt());
            Map<String, Object> response = plannerResponses.pollFirst();
            if (response == null) {
                throw new IllegalStateException("No planner response configured for prompt: " + input.getPrompt());
            }
            return response;
        }

        @Override
        public EnvLookupOutput getWorkflowEnvVars(EnvLookupInput input) {
            return new EnvLookupOutput(true, false);
        }

        @Override
        public Map<String, Object> runTool(String toolName, Map<String, Object> args) {
            return toolRegistry.execute(toolName, args);
        }

        @Override
        public Map<String, Object> mcpToolActivity(String toolName, Map<String, Object> args) {
            Map<String, Object> response = new HashMap<>();
            response.put("tool", toolName);
            response.put("success", false);
            response.put("error", "MCP integration not available in tests");
            return response;
        }

        @Override
        public Map<String, Object> listMcpTools(io.temporal.agent.model.tools.McpServerDefinition definition, List<String> includedTools) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            return response;
        }
    }
}
