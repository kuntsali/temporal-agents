package io.temporal.agent.activities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.agent.model.tools.AgentGoal;
import io.temporal.agent.model.tools.McpServerDefinition;
import io.temporal.agent.model.tools.ToolDefinition;
import io.temporal.agent.model.workflow.EnvLookupInput;
import io.temporal.agent.model.workflow.EnvLookupOutput;
import io.temporal.agent.model.workflow.ToolPromptInput;
import io.temporal.agent.model.workflow.ValidationInput;
import io.temporal.agent.model.workflow.ValidationResult;
import io.temporal.agent.tools.ToolRegistry;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

@Component
public class ToolActivitiesImpl implements ToolActivities {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolActivitiesImpl.class);

    private final ChatClient chatClient;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public ToolActivitiesImpl(ChatClient chatClient, ToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public ValidationResult agentValidatePrompt(ValidationInput input) {
        AgentGoal goal = input.getAgentGoal();
        StringBuilder context = new StringBuilder();
        context.append("The agent goal and tools are as follows:\n");
        context.append("Description: ").append(goal.getDescription()).append('\n');
        context.append("Available Tools:\n");
        for (ToolDefinition tool : goal.getTools()) {
            context.append("Tool: ").append(tool.getName()).append('\n');
            context.append("Description: ").append(tool.getDescription()).append('\n');
            context.append("Arguments: ");
            if (tool.getArguments() != null) {
                tool.getArguments().forEach(arg ->
                        context.append(arg.getName()).append(" (").append(arg.getType()).append(") "));
            }
            context.append('\n');
        }
        try {
            context.append("The conversation history to date is:\n");
            context.append(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(input.getConversationHistory()));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to serialize conversation history", e);
        }

        String validationPrompt = "The user's prompt is: \"" + input.getPrompt() + "\"\n"
                + "Please validate if this prompt makes sense given the agent goal and conversation history.\n"
                + "If the prompt makes sense toward the goal then validationResult should be true.\n"
                + "If the prompt is wildly nonsensical or makes no sense toward the goal and current conversation history then validationResult should be false.\n"
                + "Return ONLY a JSON object with fields validationResult and validationFailedReason.\n"
                + "If validationResult is false, validationFailedReason should be a JSON object with next='question' and response=<guidance>.";

        Map<String, Object> response = agentToolPlanner(new ToolPromptInput(validationPrompt, context.toString()));
        boolean result = Boolean.TRUE.equals(response.get("validationResult"));
        Map<String, Object> failureReason = extractMap(response.get("validationFailedReason"));
        return new ValidationResult(result, failureReason);
    }

    @Override
    public Map<String, Object> agentToolPlanner(ToolPromptInput input) {
        Prompt prompt = new Prompt(
                new SystemMessage(input.getContextInstructions() + " The current date is " + LocalDate.now().format(DateTimeFormatter.ISO_DATE)),
                new UserMessage(input.getPrompt())
        );
        String content = chatClient.prompt(prompt).call().content();
        String sanitized = sanitize(content);
        return parseJson(sanitized);
    }

    @Override
    public EnvLookupOutput getWorkflowEnvVars(EnvLookupInput input) {
        boolean showConfirm = Boolean.parseBoolean(System.getenv().getOrDefault(input.getShowConfirmEnvVarName(), String.valueOf(input.isShowConfirmDefault())));
        boolean multiGoalMode = "goal_choose_agent_type".equalsIgnoreCase(System.getenv().getOrDefault("AGENT_GOAL", ""));
        return new EnvLookupOutput(showConfirm, multiGoalMode);
    }

    @Override
    public Map<String, Object> runTool(String toolName, Map<String, Object> args) {
        LOGGER.info("Executing tool {} with args {}", toolName, args);
        return toolRegistry.execute(toolName, args);
    }

    @Override
    public Map<String, Object> mcpToolActivity(String toolName, Map<String, Object> args) {
        LOGGER.warn("MCP tool {} requested but MCP integration is not configured in the Java demo", toolName);
        Map<String, Object> response = new HashMap<>();
        response.put("tool", toolName);
        response.put("success", false);
        response.put("error", "MCP integration is not implemented in this demo");
        return response;
    }

    @Override
    public Map<String, Object> listMcpTools(McpServerDefinition definition, List<String> includedTools) {
        Map<String, Object> response = new HashMap<>();
        response.put("server_name", definition != null ? definition.getName() : "default");
        response.put("success", false);
        response.put("error", "MCP integration is not implemented in this demo");
        return response;
    }

    private Map<String, Object> parseJson(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            JsonNode node = objectMapper.readTree(content);
            return objectMapper.convertValue(node, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON returned from model: " + content, e);
        }
    }

    private String sanitize(String content) {
        if (content == null) {
            return null;
        }
        String sanitized = content.trim();
        if (sanitized.startsWith("```")) {
            int firstLine = sanitized.indexOf('\n');
            if (firstLine > 0) {
                sanitized = sanitized.substring(firstLine + 1);
            }
        }
        if (sanitized.endsWith("```")) {
            sanitized = sanitized.substring(0, sanitized.length() - 3);
        }
        return sanitized.trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();
            map.forEach((k, v) -> result.put(Objects.toString(k), v));
            return result;
        }
        return Collections.emptyMap();
    }
}
