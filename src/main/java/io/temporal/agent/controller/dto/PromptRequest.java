package io.temporal.agent.controller.dto;

import jakarta.validation.constraints.NotBlank;

public class PromptRequest {

    @NotBlank
    private String prompt;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
