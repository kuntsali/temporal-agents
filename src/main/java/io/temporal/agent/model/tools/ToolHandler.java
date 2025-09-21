package io.temporal.agent.model.tools;

import java.util.Map;

@FunctionalInterface
public interface ToolHandler {
    Map<String, Object> execute(Map<String, Object> arguments);
}
