package io.temporal.agent.workflow;

import io.temporal.agent.model.conversation.ConversationHistory;
import io.temporal.agent.model.tools.AgentGoal;
import io.temporal.agent.model.workflow.CombinedInput;
import io.temporal.agent.model.workflow.ToolDecision;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface AgentGoalWorkflow {

    @WorkflowMethod
    String run(CombinedInput input);

    @SignalMethod
    void submitUserPrompt(String prompt);

    @SignalMethod
    void confirmToolExecution(boolean confirmed);

    @SignalMethod
    void endChat();

    @SignalMethod
    void selectGoal(AgentGoal goal);

    @QueryMethod(name = "conversationHistory")
    ConversationHistory getConversationHistory();

    @QueryMethod(name = "toolState")
    ToolDecision getToolDecision();

    @QueryMethod(name = "currentGoal")
    AgentGoal getCurrentGoal();

    @QueryMethod(name = "chatEnded")
    boolean isChatEnded();
}
