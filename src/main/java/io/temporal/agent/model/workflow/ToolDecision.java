package io.temporal.agent.model.workflow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ToolDecision implements Serializable {

    private NextStep next;
    private String tool;
    private Map<String, Object> args = new HashMap<>();
    private String response;
    private boolean forceConfirm = true;

    public ToolDecision() {
    }

    @SuppressWarnings("unchecked")
    public static ToolDecision fromRawMap(Map<String, Object> raw) {
        ToolDecision decision = new ToolDecision();
        if (raw == null) {
            return decision;
        }
        decision.next = NextStep.fromValue((String) raw.get("next"));
        decision.tool = raw.get("tool") != null ? raw.get("tool").toString() : null;
        Object args = raw.get("args");
        if (args instanceof Map<?, ?> map) {
            map.forEach((key, value) -> decision.args.put(String.valueOf(key), value));
        }
        Object response = raw.get("response");
        if (response != null) {
            decision.response = response.toString();
        }
        Object force = raw.get("force_confirm");
        if (force instanceof Boolean bool) {
            decision.forceConfirm = bool;
        }
        return decision;
    }

    @JsonProperty("next")
    public String getNextValue() {
        return next != null ? next.getJsonValue() : NextStep.QUESTION.getJsonValue();
    }

    @JsonIgnore
    public NextStep getNext() {
        return next;
    }

    public void setNext(NextStep next) {
        this.next = next;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args != null ? new HashMap<>(args) : new HashMap<>();
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    @JsonProperty("force_confirm")
    public boolean isForceConfirm() {
        return forceConfirm;
    }

    public void setForceConfirm(boolean forceConfirm) {
        this.forceConfirm = forceConfirm;
    }

    public Map<String, Object> toRawMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("next", getNextValue());
        map.put("tool", tool);
        map.put("args", new HashMap<>(args));
        map.put("response", response);
        map.put("force_confirm", forceConfirm);
        return map;
    }

    public boolean hasArgs() {
        return args != null && !args.isEmpty();
    }

    public void ensureForceConfirm(boolean value) {
        this.forceConfirm = value;
    }

    public Map<String, Object> getArgsOrEmpty() {
        return args != null ? args : Collections.emptyMap();
    }
}
