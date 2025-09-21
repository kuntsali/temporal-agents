package io.temporal.agent.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import io.temporal.agent.model.conversation.ConversationHistory;
import io.temporal.agent.model.tools.AgentGoal;
import io.temporal.agent.model.tools.ToolArgument;
import io.temporal.agent.model.tools.ToolDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentPromptGeneratorTest {

    @Test
    void generatesPromptWithToolInformation() {
        AgentGoal goal = new AgentGoal();
        goal.setAgentName("Test Agent");
        goal.setDescription("Test description");
        ToolDefinition tool = new ToolDefinition();
        tool.setName("ExampleTool");
        tool.setDescription("Runs an example action");
        tool.setArguments(List.of(new ToolArgument("order_id", "string", "Id of the order")));
        goal.setTools(List.of(tool));

        ConversationHistory history = new ConversationHistory();
        history.addMessage("user", "Hello");

        String prompt = AgentPromptGenerator.generateGenAiPrompt(goal, history, false, null, null);
        assertThat(prompt).contains("ExampleTool");
        assertThat(prompt).contains("MANDATORY: Your response must be ONLY valid JSON with NO additional text.");
    }
}
