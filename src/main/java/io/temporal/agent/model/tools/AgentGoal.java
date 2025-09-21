package io.temporal.agent.model.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AgentGoal implements Serializable {

    private String id;
    private String categoryTag;
    private String agentName;
    private String agentFriendlyDescription;
    private List<ToolDefinition> tools = new ArrayList<>();
    private String description;
    private String starterPrompt;
    private String exampleConversationHistory;
    private McpServerDefinition mcpServerDefinition;

    public AgentGoal() {
    }

    public AgentGoal(String id,
                      String categoryTag,
                      String agentName,
                      String agentFriendlyDescription,
                      List<ToolDefinition> tools,
                      String description,
                      String starterPrompt,
                      String exampleConversationHistory) {
        this.id = id;
        this.categoryTag = categoryTag;
        this.agentName = agentName;
        this.agentFriendlyDescription = agentFriendlyDescription;
        if (tools != null) {
            this.tools = new ArrayList<>(tools);
        }
        this.description = description;
        this.starterPrompt = starterPrompt;
        this.exampleConversationHistory = exampleConversationHistory;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoryTag() {
        return categoryTag;
    }

    public void setCategoryTag(String categoryTag) {
        this.categoryTag = categoryTag;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentFriendlyDescription() {
        return agentFriendlyDescription;
    }

    public void setAgentFriendlyDescription(String agentFriendlyDescription) {
        this.agentFriendlyDescription = agentFriendlyDescription;
    }

    public List<ToolDefinition> getTools() {
        return tools;
    }

    public void setTools(List<ToolDefinition> tools) {
        this.tools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStarterPrompt() {
        return starterPrompt;
    }

    public void setStarterPrompt(String starterPrompt) {
        this.starterPrompt = starterPrompt;
    }

    public String getExampleConversationHistory() {
        return exampleConversationHistory;
    }

    public void setExampleConversationHistory(String exampleConversationHistory) {
        this.exampleConversationHistory = exampleConversationHistory;
    }

    public McpServerDefinition getMcpServerDefinition() {
        return mcpServerDefinition;
    }

    public void setMcpServerDefinition(McpServerDefinition mcpServerDefinition) {
        this.mcpServerDefinition = mcpServerDefinition;
    }
}
