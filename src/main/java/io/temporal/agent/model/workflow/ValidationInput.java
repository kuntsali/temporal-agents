package io.temporal.agent.model.workflow;

import io.temporal.agent.model.conversation.ConversationHistory;
import io.temporal.agent.model.tools.AgentGoal;
import java.io.Serializable;

public class ValidationInput implements Serializable {

    private String prompt;
    private ConversationHistory conversationHistory;
    private AgentGoal agentGoal;

    public ValidationInput() {
    }

    public ValidationInput(String prompt, ConversationHistory conversationHistory, AgentGoal agentGoal) {
        this.prompt = prompt;
        this.conversationHistory = conversationHistory;
        this.agentGoal = agentGoal;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public ConversationHistory getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(ConversationHistory conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    public AgentGoal getAgentGoal() {
        return agentGoal;
    }

    public void setAgentGoal(AgentGoal agentGoal) {
        this.agentGoal = agentGoal;
    }
}
