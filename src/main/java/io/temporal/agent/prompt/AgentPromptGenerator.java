package io.temporal.agent.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.agent.model.conversation.ConversationHistory;
import io.temporal.agent.model.tools.AgentGoal;
import io.temporal.agent.model.tools.ToolArgument;
import io.temporal.agent.model.tools.ToolDefinition;
import io.temporal.agent.model.workflow.ToolDecision;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public final class AgentPromptGenerator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private AgentPromptGenerator() {
    }

    public static String generateGenAiPrompt(
            AgentGoal goal,
            ConversationHistory history,
            boolean multiGoalMode,
            ToolDecision rawJson,
            Map<String, Object> mcpToolsInfo) {
        List<String> lines = new ArrayList<>();
        lines.add("You are an AI agent that helps fill required arguments for the tools described below. "
                + "CRITICAL: You must respond with ONLY valid JSON using the exact schema provided. "
                + "DO NOT include any text before or after the JSON. Your entire response must be parseable JSON.");

        lines.add("=== Conversation History ===");
        lines.add("This is the ongoing history to determine which tool and arguments to gather:");
        lines.add("*BEGIN CONVERSATION HISTORY*");
        lines.add(prettyPrint(history));
        lines.add("*END CONVERSATION HISTORY*");
        lines.add("REMINDER: You can use the conversation history to infer arguments for the tools.");

        if (goal.getExampleConversationHistory() != null && !goal.getExampleConversationHistory().isBlank()) {
            lines.add("=== Example Conversation With These Tools ===");
            lines.add("Use this example to understand how tools are invoked and arguments are gathered.");
            lines.add("BEGIN EXAMPLE");
            lines.add(goal.getExampleConversationHistory());
            lines.add("END EXAMPLE");
        }

        if (goal.getMcpServerDefinition() != null) {
            lines.add("=== MCP Server Information ===");
            lines.add("Connected to MCP Server: " + goal.getMcpServerDefinition().getName());
            if (mcpToolsInfo != null && Boolean.TRUE.equals(mcpToolsInfo.get("success"))) {
                Object tools = mcpToolsInfo.get("tools");
                if (tools instanceof Map<?, ?> toolMap) {
                    lines.add("MCP Tools loaded from "
                            + mcpToolsInfo.getOrDefault("server_name", "unknown")
                            + " (" + toolMap.size() + " tools):");
                    toolMap.forEach((name, value) -> {
                        if (value instanceof Map<?, ?> toolDetails) {
                            Object description = toolDetails.get("description");
                            lines.add("  - " + name + ": " + Objects.toString(description, ""));
                        }
                    });
                }
            } else {
                lines.add("Additional tools available via MCP integration.");
            }
        }

        lines.add("=== Tools Definitions ===");
        lines.add("There are " + goal.getTools().size() + " available tools:");
        lines.add(goal.getTools().stream().map(ToolDefinition::getName).reduce((a, b) -> a + ", " + b).orElse("none"));
        lines.add("Goal: " + Objects.toString(goal.getDescription(), ""));
        lines.add("CRITICAL: You MUST follow the complete sequence described in the Goal above. Do NOT skip steps or assume the goal is complete until ALL steps are done.");
        lines.add("Gather the necessary information for each tool in the sequence described above.");
        lines.add("Only ask for arguments listed below. Do not add extra arguments.");

        for (ToolDefinition tool : goal.getTools()) {
            lines.add("Tool name: " + tool.getName());
            lines.add("  Description: " + tool.getDescription());
            lines.add("  Required args:");
            for (ToolArgument arg : tool.getArguments()) {
                lines.add("    - " + arg.getName() + " (" + arg.getType() + "): " + arg.getDescription());
            }
        }

        lines.add("When all required args for a tool are known, you can propose next='confirm' to run it.");

        lines.add("=== CRITICAL: JSON-ONLY RESPONSE FORMAT ===");
        lines.add("MANDATORY: Your response must be ONLY valid JSON with NO additional text.\n"
                + "NO explanations, NO comments, NO text before or after the JSON.\n"
                + "Your entire response must start with '{' and end with '}'.\n\n"
                + "Required JSON format:\n"
                + "{\n"
                + "  \"response\": \"<plain text>\",\n"
                + "  \"next\": \"<question|confirm|pick-new-goal|done>\",\n"
                + "  \"tool\": \"<tool_name or null>\",\n"
                + "  \"args\": {\n"
                + "    \"<arg1>\": \"<value1 or null>\",\n"
                + "    \"<arg2>\": \"<value2 or null>\"\n"
                + "  }\n"
                + "}");

        lines.add("DECISION LOGIC (follow this exact order):\n"
                + "1) Do I need to run a tool next?\n"
                + "   - If your response says 'let's get/proceed/check/add/create/finalize...' -> YES, you need a tool\n"
                + "   - If you're announcing what you're about to do -> YES, you need a tool\n"
                + "   - If no more steps needed for current goal -> NO, go to step 3\n\n"
                + "2) If YES to step 1: Do I have all required arguments?\n"
                + "   - Check tool definition for required args\n"
                + "   - Can I fill missing args from conversation history?\n"
                + "   - Can I use sensible defaults (limit=100, etc.)?\n"
                + "   - If ALL args available/inferrable -> set next='confirm', specify tool and args\n"
                + "   - If missing required args -> set next='question', ask for missing args, tool=null\n\n"
                + "3) If NO to step 1: Is the entire goal complete?\n"
                + "   - Check Goal description in system prompt - are ALL steps done?\n"
                + "   - Check recent conversation for completion indicators ('finalized', 'complete', etc.)\n"
                + "   - If complete -> " + generateToolchainCompleteGuidance() + "\n"
                + "   - If not complete -> identify next needed tool, go to step 2\n\n"
                + "CRITICAL RULES:\n"
                + "• RESPOND WITH JSON ONLY - NO TEXT BEFORE OR AFTER THE JSON OBJECT\n"
                + "• Your response must start with '{' and end with '}' - nothing else\n"
                + "• NEVER set next='question' without asking an actual question in your response\n"
                + "• NEVER set tool=null when you're announcing you'll run a specific tool\n"
                + "• Use conversation history to infer arguments (customer IDs, product IDs, etc.)\n"
                + "• Use sensible defaults rather than asking users for technical parameters\n"
                + "• Carry forward arguments between tools (same customer, same invoice, etc.)\n"
                + "• If force_confirm='False' in history, be declarative, don't ask permission\n\n"
                + "EXAMPLES:\n"
                + "WRONG: response='let's get pricing', next='question', tool=null\n"
                + "RIGHT: response='let's get pricing', next='confirm', tool='list_prices'\n"
                + "WRONG: response='adding pizza', next='question', tool='create_invoice_item'\n"
                + "RIGHT: response='adding pizza', next='confirm', tool='create_invoice_item'");

        lines.add("=== FINAL REMINDER ===");
        if (rawJson != null) {
            lines.add("Validate the provided JSON and return ONLY corrected JSON.");
        } else {
            lines.add("Return ONLY a valid JSON response. Start with '{' and end with '}'.");
        }

        return String.join("\n", lines);
    }

    public static String generateMissingArgsPrompt(String toolName, List<String> missing) {
        StringJoiner joiner = new StringJoiner(", ");
        missing.forEach(joiner::add);
        return "### The tool '" + toolName + "' is missing required args: " + joiner + ". "
                + "Ask the user in plain language for those arguments, referencing the specific fields needed. "
                + "Respond with JSON using the schema {\"response\": \"<text>\", \"next\": \"question\", \"tool\": null, \"args\": { ... }}.";
    }

    public static String generateToolCompletionPrompt(String toolName, Map<String, Object> result) {
        return "### The '" + toolName + "' tool completed successfully with " + prettyPrint(result) + ". "
                + "INSTRUCTIONS: Parse this tool result as plain text, and use the system prompt containing the list of tools in sequence and the conversation history (and previous tool_results) to figure out next steps, if any. "
                + "You will need to use the tool_results to auto-fill arguments for subsequent tools and also to figure out if all tools have been run. "
                + "{" + "\"next\": \"<question|confirm|pick-new-goal|done>\", \"tool\": \"<tool_name or null>\", \"args\": {\"<arg1>\": \"<value1 or null>\", \"<arg2>\": \"<value2 or null>\"}, \"response\": \"<plain text>\"}";
    }

    private static String prettyPrint(Object value) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private static String generateToolchainCompleteGuidance() {
        return "If all required steps are complete, set next='done', tool=null, and provide a concise closing response summarizing what happened.";
    }
}
