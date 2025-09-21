package io.temporal.agent.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.agent.model.tools.McpServerDefinition;
import io.temporal.agent.model.workflow.EnvLookupInput;
import io.temporal.agent.model.workflow.EnvLookupOutput;
import io.temporal.agent.model.workflow.ToolPromptInput;
import io.temporal.agent.model.workflow.ValidationInput;
import io.temporal.agent.model.workflow.ValidationResult;
import java.util.List;
import java.util.Map;

@ActivityInterface
public interface ToolActivities {

    ValidationResult agentValidatePrompt(ValidationInput input);

    Map<String, Object> agentToolPlanner(ToolPromptInput input);

    EnvLookupOutput getWorkflowEnvVars(EnvLookupInput input);

    Map<String, Object> runTool(String toolName, Map<String, Object> args);

    Map<String, Object> mcpToolActivity(String toolName, Map<String, Object> args);

    Map<String, Object> listMcpTools(McpServerDefinition definition, List<String> includedTools);
}
