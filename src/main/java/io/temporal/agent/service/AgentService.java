package io.temporal.agent.service;

import io.temporal.agent.config.TemporalProperties;
import io.temporal.agent.goals.GoalRegistry;
import io.temporal.agent.model.conversation.ConversationHistory;
import io.temporal.agent.model.tools.AgentGoal;
import io.temporal.agent.model.workflow.AgentGoalWorkflowParams;
import io.temporal.agent.model.workflow.CombinedInput;
import io.temporal.agent.model.workflow.ToolDecision;
import io.temporal.agent.workflow.AgentGoalWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private final WorkflowClient workflowClient;
    private final TemporalProperties properties;
    private final GoalRegistry goalRegistry;

    public AgentService(WorkflowClient workflowClient, TemporalProperties properties, GoalRegistry goalRegistry) {
        this.workflowClient = workflowClient;
        this.properties = properties;
        this.goalRegistry = goalRegistry;
    }

    public String startConversation(String workflowId, String goalId) {
        AgentGoal goal = resolveGoal(goalId);
        AgentGoalWorkflowParams params = new AgentGoalWorkflowParams();
        CombinedInput input = new CombinedInput(params, goal);

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId != null ? workflowId : UUID.randomUUID().toString())
                .setTaskQueue(properties.getTaskQueue())
                .build();

        AgentGoalWorkflow workflow = workflowClient.newWorkflowStub(AgentGoalWorkflow.class, options);
        WorkflowStub untyped = WorkflowStub.fromTyped(workflow);
        WorkflowClient.start(workflow::run, input);
        return untyped.getExecution().getWorkflowId();
    }

    public void sendPrompt(String workflowId, String prompt) {
        AgentGoalWorkflow workflow = workflowClient.newWorkflowStub(AgentGoalWorkflow.class, workflowId);
        workflow.submitUserPrompt(prompt);
    }

    public void confirmTool(String workflowId) {
        AgentGoalWorkflow workflow = workflowClient.newWorkflowStub(AgentGoalWorkflow.class, workflowId);
        workflow.confirmToolExecution(true);
    }

    public void endConversation(String workflowId) {
        AgentGoalWorkflow workflow = workflowClient.newWorkflowStub(AgentGoalWorkflow.class, workflowId);
        workflow.endChat();
    }

    public void selectGoal(String workflowId, String goalId) {
        AgentGoal goal = resolveGoal(goalId);
        AgentGoalWorkflow workflow = workflowClient.newWorkflowStub(AgentGoalWorkflow.class, workflowId);
        workflow.selectGoal(goal);
    }

    public ConversationHistory getHistory(String workflowId) {
        AgentGoalWorkflow workflow = workflowClient.newWorkflowStub(AgentGoalWorkflow.class, workflowId);
        return workflow.getConversationHistory();
    }

    public ToolDecision getToolDecision(String workflowId) {
        AgentGoalWorkflow workflow = workflowClient.newWorkflowStub(AgentGoalWorkflow.class, workflowId);
        return workflow.getToolDecision();
    }

    public AgentGoal getCurrentGoal(String workflowId) {
        AgentGoalWorkflow workflow = workflowClient.newWorkflowStub(AgentGoalWorkflow.class, workflowId);
        return workflow.getCurrentGoal();
    }

    public List<AgentGoal> listGoals() {
        return goalRegistry.listGoals();
    }

    private AgentGoal resolveGoal(String goalId) {
        if (goalId == null || goalId.isBlank()) {
            return Optional.ofNullable(goalRegistry.listGoals().stream().findFirst().orElse(null))
                    .orElseThrow(() -> new IllegalArgumentException("No goals configured"));
        }
        AgentGoal goal = goalRegistry.findGoal(goalId);
        if (goal == null) {
            throw new IllegalArgumentException("Unknown goal: " + goalId);
        }
        return goal;
    }
}
