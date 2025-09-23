package io.temporal.agent.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(String workflowId) {
        super(buildMessage(workflowId));
    }

    public ConversationNotFoundException(String workflowId, Throwable cause) {
        super(buildMessage(workflowId), cause);
    }

    private static String buildMessage(String workflowId) {
        if (workflowId == null || workflowId.isBlank()) {
            return "Conversation is no longer available";
        }
        return String.format("Conversation for workflow %s is no longer available", workflowId);
    }
}
