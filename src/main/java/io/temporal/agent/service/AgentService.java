package io.temporal.agent.service;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.agent.config.TemporalProperties;
import io.temporal.agent.goals.GoalRegistry;
import io.temporal.agent.model.conversation.ConversationHistory;
import io.temporal.agent.model.tools.AgentGoal;
import io.temporal.agent.model.workflow.AgentGoalWorkflowParams;
import io.temporal.agent.model.workflow.CombinedInput;
import io.temporal.agent.model.workflow.ToolDecision;
import io.temporal.agent.workflow.AgentGoalWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowQueryRejectedException;
import io.temporal.client.WorkflowStub;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
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
        AgentGoal goal = resolveInitialGoal(goalId);
        AgentGoalWorkflowParams params = new AgentGoalWorkflowParams();
        CombinedInput input = new CombinedInput(params, goal);

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId != null ? workflowId : UUID.randomUUID().toString())
                .setTaskQueue(properties.taskQueue())
                .build();

        AgentGoalWorkflow workflow = workflowClient.newWorkflowStub(AgentGoalWorkflow.class, options);
        WorkflowStub untyped = WorkflowStub.fromTyped(workflow);
        WorkflowClient.start(workflow::run, input);
        return untyped.getExecution().getWorkflowId();
    }

    public void sendPrompt(String workflowId, String prompt) {
        withWorkflow(workflowId, workflow -> {
            workflow.submitUserPrompt(prompt);
            return null;
        });
    }

    public void confirmTool(String workflowId) {
        withWorkflow(workflowId, workflow -> {
            workflow.confirmToolExecution(true);
            return null;
        });
    }

    public void endConversation(String workflowId) {
        withWorkflow(workflowId, workflow -> {
            workflow.endChat();
            return null;
        });
    }

    public void selectGoal(String workflowId, String goalId) {
        AgentGoal goal = resolveGoal(goalId);
        withWorkflow(workflowId, workflow -> {
            workflow.selectGoal(goal);
            return null;
        });
    }

    public ConversationHistory getHistory(String workflowId) {
        return withWorkflow(workflowId, AgentGoalWorkflow::getConversationHistory);
    }

    public ToolDecision getToolDecision(String workflowId) {
        return withWorkflow(workflowId, AgentGoalWorkflow::getToolDecision);
    }

    public AgentGoal getCurrentGoal(String workflowId) {
        return withWorkflow(workflowId, AgentGoalWorkflow::getCurrentGoal);
    }

    public List<AgentGoal> listGoals() {
        return goalRegistry.listGoals();
    }

    private AgentGoal resolveGoal(String goalId) {
        AgentGoal goal = goalRegistry.findGoal(goalId);
        if (goal == null) {
            throw new IllegalArgumentException("Unknown goal: " + goalId);
        }
        return goal;
    }

    private AgentGoal resolveInitialGoal(String goalId) {
        if (goalId == null || goalId.isBlank()) {
            AgentGoal selectionGoal = goalRegistry.findGoal("goal_choose_agent_type");
            if (selectionGoal != null) {
                return selectionGoal;
            }
            return Optional.ofNullable(goalRegistry.listGoals().stream().findFirst().orElse(null))
                    .orElseThrow(() -> new IllegalStateException("No goals configured"));
        }
        return resolveGoal(goalId);
    }

    private <T> T withWorkflow(String workflowId, Function<AgentGoalWorkflow, T> action) {
        try {
            AgentGoalWorkflow workflow = workflowClient.newWorkflowStub(AgentGoalWorkflow.class, workflowId);
            return action.apply(workflow);
        } catch (RuntimeException ex) {
            throw translateTemporalException(workflowId, ex);
        }
    }

    private RuntimeException translateTemporalException(String workflowId, RuntimeException ex) {
        if (ex instanceof ConversationNotFoundException) {
            return ex;
        }
        if (ex instanceof WorkflowNotFoundException || ex instanceof WorkflowQueryRejectedException) {
            return new ConversationNotFoundException(workflowId, ex);
        }

        StatusRuntimeException statusException = findStatusRuntimeException(ex);
        if (statusException != null) {
            Status.Code code = statusException.getStatus().getCode();
            if (Status.Code.NOT_FOUND.equals(code) || Status.Code.FAILED_PRECONDITION.equals(code)) {
                return new ConversationNotFoundException(workflowId, ex);
            }
        }
        return ex;
    }

    private StatusRuntimeException findStatusRuntimeException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof StatusRuntimeException statusRuntimeException) {
                return statusRuntimeException;
            }
            current = current.getCause();
        }
        return null;
    }
}
