package io.temporal.agent.controller.dto;

public class StartConversationResponse {

    private String workflowId;

    public StartConversationResponse() {
    }

    public StartConversationResponse(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }
}
