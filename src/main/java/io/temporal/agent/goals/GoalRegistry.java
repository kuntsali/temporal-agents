package io.temporal.agent.goals;

import io.temporal.agent.model.tools.AgentGoal;
import io.temporal.agent.model.tools.ToolDefinition;
import io.temporal.agent.tools.ToolRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GoalRegistry {

    private final Map<String, AgentGoal> goalsById = new HashMap<>();

    public GoalRegistry(ToolRegistry toolRegistry) {
        register(buildEcommerceOrderStatus(toolRegistry));
        register(buildEcommerceListOrders(toolRegistry));
        register(buildAgentSelectionGoal(toolRegistry));
        register(buildPandadocAutomationGoal(toolRegistry));
    }

    private void register(AgentGoal goal) {
        if (goal != null) {
            goalsById.put(goal.getId(), goal);
        }
    }

    public List<AgentGoal> listGoals() {
        return Collections.unmodifiableList(new ArrayList<>(goalsById.values()));
    }

    public AgentGoal findGoal(String id) {
        return goalsById.get(id);
    }

    private AgentGoal buildEcommerceOrderStatus(ToolRegistry toolRegistry) {
        AgentGoal goal = new AgentGoal();
        goal.setId("goal_ecomm_order_status");
        goal.setCategoryTag("ecommerce");
        goal.setAgentName("Check Order Status");
        goal.setAgentFriendlyDescription("Check the status of a customer's order and optionally track the package.");
        goal.setDescription("The user wants to know the status of a specific order. Collect the order id, run GetOrder, and if the status is shipped or delivered offer TrackPackage.");
        goal.setStarterPrompt("Welcome me, explain what you can do, then ask for the order details you need.");
        goal.setExampleConversationHistory(String.join("\n",
                "user: I'd like to know the status of my order",
                "agent: Certainly! Can you share your order number?",
                "user: It's 102",
                "user_confirmed_tool_run: <user clicks confirm on GetOrder>",
                "tool_result: {... order information ...}",
                "agent: Your order was shipped and includes tracking id 039813852990618. Do you want to see tracking updates?"));
        List<ToolDefinition> tools = List.of(
                cloneDefinition(toolRegistry.get("GetOrder")),
                cloneDefinition(toolRegistry.get("TrackPackage")));
        goal.setTools(tools);
        return goal;
    }

    private AgentGoal buildEcommerceListOrders(ToolRegistry toolRegistry) {
        AgentGoal goal = new AgentGoal();
        goal.setId("goal_ecomm_list_orders");
        goal.setCategoryTag("ecommerce");
        goal.setAgentName("List All Orders");
        goal.setAgentFriendlyDescription("List a customer's orders and offer to drill into details.");
        goal.setDescription("Collect the customer's email address, run ListOrders, and optionally follow up with GetOrder and TrackPackage for specific orders if the user asks.");
        goal.setStarterPrompt("Introduce yourself and ask which customer you should look up.");
        goal.setExampleConversationHistory(String.join("\n",
                "user: I'd like to see my orders",
                "agent: Happy to help. What's the email address on the account?",
                "user: matt.murdock@nelsonmurdock.com",
                "user_confirmed_tool_run: <user clicks confirm on ListOrders>",
                "tool_result: {... list of orders ...}",
                "agent: I found multiple orders. Would you like details on a specific one?"));
        List<ToolDefinition> tools = List.of(
                cloneDefinition(toolRegistry.get("ListOrders")),
                cloneDefinition(toolRegistry.get("GetOrder")),
                cloneDefinition(toolRegistry.get("TrackPackage")));
        goal.setTools(tools);
        return goal;
    }

    private AgentGoal buildAgentSelectionGoal(ToolRegistry toolRegistry) {
        AgentGoal goal = new AgentGoal();
        goal.setId("goal_choose_agent_type");
        goal.setCategoryTag("core");
        goal.setAgentName("Select Agent Type");
        goal.setAgentFriendlyDescription("Help the user choose which agent to interact with.");
        goal.setDescription("Understand the user's intent and propose the best agent goal from the catalog.");
        goal.setStarterPrompt("Greet the user and explain you can help choose from available goals.");
        goal.setExampleConversationHistory(String.join("\n",
                "user: I want help tracking an order",
                "agent: Great! I can connect you with the order tracking agent. Does that sound good?"));
        goal.setTools(Collections.emptyList());
        return goal;
    }

    private AgentGoal buildPandadocAutomationGoal(ToolRegistry toolRegistry) {
        ToolDefinition listTemplates = cloneDefinition(toolRegistry.get("ListPandadocTemplates"));
        ToolDefinition createDocument = cloneDefinition(toolRegistry.get("CreatePandadocDocument"));
        ToolDefinition sendDocument = cloneDefinition(toolRegistry.get("SendPandadocDocument"));
        ToolDefinition getStatus = cloneDefinition(toolRegistry.get("GetPandadocDocumentStatus"));
        if (listTemplates == null || createDocument == null || sendDocument == null || getStatus == null) {
            return null;
        }
        AgentGoal goal = new AgentGoal();
        goal.setId("goal_pandadoc_automation");
        goal.setCategoryTag("documents");
        goal.setAgentName("PandaDoc Agreement Automation");
        goal.setAgentFriendlyDescription("Prepare, send, and track PandaDoc agreements end-to-end.");
        goal.setDescription(String.join(" ",
                "Guide the user through choosing a PandaDoc template, collecting signer details,",
                "filling required token values, creating the document draft, sending it for signature,",
                "and monitoring status until it is completed. Always list templates so the user can",
                "confirm the correct template name or ID before creating a document."));
        goal.setStarterPrompt("Introduce yourself as a PandaDoc assistant, offer to list templates, and gather the request details.");
        goal.setExampleConversationHistory(String.join("\n",
                "user: Can you create an NDA for Acme?",
                "agent: Absolutely. Here are the PandaDoc templates I can use: ... Which one should we start with?",
                "user: Use the Mutual NDA template and send it to John Doe john@example.com.",
                "agent: Great! I'll create the document draft now and send it for signature once it's ready."));
        List<ToolDefinition> tools = List.of(listTemplates, createDocument, sendDocument, getStatus);
        goal.setTools(tools);
        return goal;
    }

    private ToolDefinition cloneDefinition(ToolDefinition original) {
        if (original == null) {
            return null;
        }
        ToolDefinition clone = new ToolDefinition();
        clone.setName(original.getName());
        clone.setDescription(original.getDescription());
        clone.setArguments(original.getArguments());
        clone.setHandler(original.getHandler());
        return clone;
    }
}
