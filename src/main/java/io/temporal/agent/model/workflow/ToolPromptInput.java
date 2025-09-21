package io.temporal.agent.model.workflow;

import java.io.Serializable;

public class ToolPromptInput implements Serializable {

    private String prompt;
    private String contextInstructions;

    public ToolPromptInput() {
    }

    public ToolPromptInput(String prompt, String contextInstructions) {
        this.prompt = prompt;
        this.contextInstructions = contextInstructions;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getContextInstructions() {
        return contextInstructions;
    }

    public void setContextInstructions(String contextInstructions) {
        this.contextInstructions = contextInstructions;
    }
}
