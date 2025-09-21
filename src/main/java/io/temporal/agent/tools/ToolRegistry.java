package io.temporal.agent.tools;

import io.temporal.agent.model.tools.ToolDefinition;
import io.temporal.agent.model.tools.ToolHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

    private final Map<String, ToolDefinition> definitions = new ConcurrentHashMap<>();

    public void register(ToolDefinition definition, ToolHandler handler) {
        definition.setHandler(handler);
        definitions.put(definition.getName(), definition);
    }

    public ToolDefinition get(String name) {
        return definitions.get(name);
    }

    public Map<String, Object> execute(String name, Map<String, Object> arguments) {
        ToolDefinition definition = get(name);
        if (definition == null || definition.getHandler() == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        Map<String, Object> safeArgs = arguments != null ? arguments : Collections.emptyMap();
        return definition.getHandler().execute(safeArgs);
    }

    public Collection<ToolDefinition> list() {
        return definitions.values();
    }
}
