package io.temporal.agent.model.conversation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ConversationHistory {

    private final List<ConversationMessage> messages = new ArrayList<>();

    public ConversationHistory() {
    }

    @JsonProperty("messages")
    public List<ConversationMessage> getMessages() {
        return messages;
    }

    public void addMessage(String type, Object response) {
        messages.add(ConversationMessage.of(type, response));
    }

    public void clear() {
        messages.clear();
    }

    @JsonIgnore
    public boolean isEmpty() {
        return messages.isEmpty();
    }
}
