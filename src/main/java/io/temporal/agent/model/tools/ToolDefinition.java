package io.temporal.agent.model.tools;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ToolDefinition implements Serializable {

    private String name;
    private String description;
    private List<ToolArgument> arguments = new ArrayList<>();
    @JsonIgnore
    private transient ToolHandler handler;

    public ToolDefinition() {
    }

    public ToolDefinition(String name, String description, List<ToolArgument> arguments) {
        this.name = name;
        this.description = description;
        if (arguments != null) {
            this.arguments = new ArrayList<>(arguments);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ToolArgument> getArguments() {
        return arguments;
    }

    public void setArguments(List<ToolArgument> arguments) {
        this.arguments = arguments != null ? new ArrayList<>(arguments) : new ArrayList<>();
    }

    public ToolHandler getHandler() {
        return handler;
    }

    public void setHandler(ToolHandler handler) {
        this.handler = handler;
    }
}
