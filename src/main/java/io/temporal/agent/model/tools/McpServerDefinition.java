package io.temporal.agent.model.tools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class McpServerDefinition implements Serializable {

    private String name;
    private String command;
    private List<String> args = new ArrayList<>();
    private Map<String, String> env = new HashMap<>();
    private String connectionType = "stdio";
    private List<String> includedTools;

    public McpServerDefinition() {
    }

    public McpServerDefinition(String name, String command, List<String> args, Map<String, String> env) {
        this.name = name;
        this.command = command;
        if (args != null) {
            this.args = new ArrayList<>(args);
        }
        if (env != null) {
            this.env = new HashMap<>(env);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args != null ? new ArrayList<>(args) : new ArrayList<>();
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env != null ? new HashMap<>(env) : new HashMap<>();
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public List<String> getIncludedTools() {
        return includedTools;
    }

    public void setIncludedTools(List<String> includedTools) {
        this.includedTools = includedTools;
    }
}
