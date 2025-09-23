package io.temporal.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.agent.config.TemporalProperties;
import io.temporal.agent.goals.GoalRegistry;
import io.temporal.agent.model.tools.AgentGoal;
import io.temporal.agent.workflow.AgentGoalWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowQueryRejectedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private WorkflowClient workflowClient;

    @Mock
    private TemporalProperties temporalProperties;

    @Mock
    private GoalRegistry goalRegistry;

    @Mock
    private AgentGoalWorkflow workflowStub;

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(workflowClient, temporalProperties, goalRegistry);
    }

    @Test
    void getHistoryWrapsFailedPreconditionAsConversationNotFound() {
        String workflowId = "wf-id";
        when(workflowClient.newWorkflowStub(eq(AgentGoalWorkflow.class), eq(workflowId))).thenReturn(workflowStub);
        StatusRuntimeException statusException = Status.FAILED_PRECONDITION.asRuntimeException();
        when(workflowStub.getConversationHistory()).thenThrow(statusException);

        assertThatThrownBy(() -> agentService.getHistory(workflowId))
                .isInstanceOf(ConversationNotFoundException.class)
                .hasMessageContaining(workflowId);
    }

    @Test
    void getHistoryWrapsWorkflowQueryRejectedException() {
        String workflowId = "wf-id";
        when(workflowClient.newWorkflowStub(eq(AgentGoalWorkflow.class), eq(workflowId))).thenReturn(workflowStub);
        WorkflowExecution execution = WorkflowExecution.newBuilder().setWorkflowId(workflowId).build();
        WorkflowQueryRejectedException exception =
                new WorkflowQueryRejectedException(execution, "conversationHistory", Status.ABORTED.asRuntimeException());
        when(workflowStub.getConversationHistory()).thenThrow(exception);

        assertThatThrownBy(() -> agentService.getHistory(workflowId))
                .isInstanceOf(ConversationNotFoundException.class)
                .hasMessageContaining(workflowId);
    }

    @Test
    void sendPromptWrapsWorkflowNotFoundException() {
        String workflowId = "wf-id";
        when(workflowClient.newWorkflowStub(eq(AgentGoalWorkflow.class), eq(workflowId))).thenReturn(workflowStub);
        WorkflowExecution execution = WorkflowExecution.newBuilder().setWorkflowId(workflowId).build();
        doThrow(new WorkflowNotFoundException(execution, "AgentGoalWorkflow", Status.NOT_FOUND.asRuntimeException()))
                .when(workflowStub)
                .submitUserPrompt("hello");

        assertThatThrownBy(() -> agentService.sendPrompt(workflowId, "hello"))
                .isInstanceOf(ConversationNotFoundException.class)
                .hasMessageContaining(workflowId);
    }

    @Test
    void resolveInitialGoalDefaultsToSelectionGoalWhenAvailable() throws Exception {
        AgentGoal selectionGoal = new AgentGoal();
        selectionGoal.setId("goal_choose_agent_type");
        when(goalRegistry.findGoal("goal_choose_agent_type")).thenReturn(selectionGoal);

        AgentGoal resolved = invokeResolveInitialGoal(null);

        assertThat(resolved).isSameAs(selectionGoal);
    }

    @Test
    void resolveInitialGoalFallsBackToFirstGoalWhenSelectionMissing() throws Exception {
        AgentGoal fallbackGoal = new AgentGoal();
        fallbackGoal.setId("fallback");
        when(goalRegistry.findGoal("goal_choose_agent_type")).thenReturn(null);
        when(goalRegistry.listGoals()).thenReturn(List.of(fallbackGoal));

        AgentGoal resolved = invokeResolveInitialGoal(null);

        assertThat(resolved).isSameAs(fallbackGoal);
    }

    @Test
    void resolveInitialGoalThrowsWhenNoGoalsConfigured() {
        when(goalRegistry.findGoal("goal_choose_agent_type")).thenReturn(null);
        when(goalRegistry.listGoals()).thenReturn(List.of());

        assertThatThrownBy(() -> invokeResolveInitialGoal(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No goals configured");
    }

    private AgentGoal invokeResolveInitialGoal(String goalId) throws Exception {
        Method method = AgentService.class.getDeclaredMethod("resolveInitialGoal", String.class);
        method.setAccessible(true);
        try {
            return (AgentGoal) method.invoke(agentService, goalId);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw ex;
        }
    }
}
