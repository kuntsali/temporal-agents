package io.temporal.agent.model.conversation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConversationMessage(
        @JsonProperty("type") String type,
        @JsonProperty("response") Object response) {

    public static ConversationMessage of(String type, Object response) {
        return new ConversationMessage(type, response);
    }
}
