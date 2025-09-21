package io.temporal.agent.model.workflow;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

public class AgentGoalWorkflowParams implements Serializable {

    private String conversationSummary;
    private Deque<String> promptQueue = new ArrayDeque<>();

    public AgentGoalWorkflowParams() {
    }

    public AgentGoalWorkflowParams(String conversationSummary, Deque<String> promptQueue) {
        this.conversationSummary = conversationSummary;
        if (promptQueue != null) {
            this.promptQueue = new ArrayDeque<>(promptQueue);
        }
    }

    public String getConversationSummary() {
        return conversationSummary;
    }

    public void setConversationSummary(String conversationSummary) {
        this.conversationSummary = conversationSummary;
    }

    public Deque<String> getPromptQueue() {
        return promptQueue;
    }

    public void setPromptQueue(Deque<String> promptQueue) {
        this.promptQueue = new ArrayDeque<>(promptQueue);
    }
}
