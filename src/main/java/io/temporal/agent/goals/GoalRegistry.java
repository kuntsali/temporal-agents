package io.temporal.agent.goals;

import io.temporal.agent.model.tools.AgentGoal;
import io.temporal.agent.model.tools.ToolDefinition;
import io.temporal.agent.tools.ToolRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

@Component
public class GoalRegistry implements SmartInitializingSingleton {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoalRegistry.class);

    private final ToolRegistry toolRegistry;
    private final Map<String, AgentGoal> goalsById = new LinkedHashMap<>();

    public GoalRegistry(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
        refreshGoals();
    }

    @Override
    public void afterSingletonsInstantiated() {
        refreshGoals();
    }

    public synchronized void refreshGoals() {
        goalsById.clear();
        register(buildEcommerceOrderStatus());
        register(buildEcommerceListOrders());
        register(buildAgentSelectionGoal());
        register(buildPandadocAutomationGoal());
    }

    private void register(AgentGoal goal) {
        if (goal != null) {
            goalsById.put(goal.getId(), goal);
        }
    }

    public synchronized List<AgentGoal> listGoals() {
        return Collections.unmodifiableList(new ArrayList<>(goalsById.values()));
    }

    public synchronized AgentGoal findGoal(String id) {
        return goalsById.get(id);
    }

    private AgentGoal buildEcommerceOrderStatus() {
        ToolDefinition getOrder = fetchTool("GetOrder");
        ToolDefinition trackPackage = fetchTool("TrackPackage");
        if (getOrder == null || trackPackage == null) {
            return null;
        }
        AgentGoal goal = new AgentGoal();
        goal.setId("goal_ecomm_order_status");
        goal.setCategoryTag("ecommerce");
        goal.setAgentName("Check Order Status");
        goal.setAgentFriendlyDescription("Check the status of a customer's order and optionally track the package.");
        goal.setDescription(String.join(" ",
                "Serve as a friendly order support specialist who confirms the customer's details, retrieves the latest status,",
                "and clearly explains what you find.",
                "Always gather the order identifier before calling GetOrder.",
                "If the package has shipped or delivered, proactively offer to run TrackPackage to share the current tracking",
                "information.",
                "Keep responses short, acknowledge what the customer has already said, and ask only one question at a time."));
        goal.setStarterPrompt(String.join(" ",
                "Warmly introduce yourself as the order support assistant, mention you can check order status and tracking updates,",
                "and ask for the order number or key details you need to begin."));
        goal.setExampleConversationHistory(String.join("\n",
                "user: I'd like to know the status of my order",
                "agent: Hi! I'd be happy to check on that for you. What's the order number?",
                "user: It's 102",
                "user_confirmed_tool_run: <user clicks confirm on GetOrder>",
                "tool_result: {... order information ...}",
                "agent: Your order shipped earlier today and the tracking ID is 039813852990618. Want me to keep an eye on delivery for you?"));
        goal.setTools(List.of(getOrder, trackPackage));
        return goal;
    }

    private AgentGoal buildEcommerceListOrders() {
        ToolDefinition listOrders = fetchTool("ListOrders");
        ToolDefinition getOrder = fetchTool("GetOrder");
        ToolDefinition trackPackage = fetchTool("TrackPackage");
        if (listOrders == null || getOrder == null || trackPackage == null) {
            return null;
        }
        AgentGoal goal = new AgentGoal();
        goal.setId("goal_ecomm_list_orders");
        goal.setCategoryTag("ecommerce");
        goal.setAgentName("List All Orders");
        goal.setAgentFriendlyDescription("List a customer's orders and offer to drill into details.");
        goal.setDescription(String.join(" ",
                "Help the customer review their recent orders.",
                "Collect the customer's email address, run ListOrders, and summarize the results in everyday language.",
                "If they request information on a particular order, follow up with GetOrder and TrackPackage as needed.",
                "Keep the conversation conversational, ask one question at a time, and avoid repeating information they've already provided."));
        goal.setStarterPrompt(String.join(" ",
                "Introduce yourself as the order history assistant, mention you can list their recent orders,",
                "and ask which customer's email address you should use."));
        goal.setExampleConversationHistory(String.join("\n",
                "user: I'd like to see my orders",
                "agent: Happy to help! Which email should I look up for you?",
                "user: matt.murdock@nelsonmurdock.com",
                "user_confirmed_tool_run: <user clicks confirm on ListOrders>",
                "tool_result: {... list of orders ...}",
                "agent: I found a few recent orders for that email. Want details on any particular one?"));
        goal.setTools(List.of(listOrders, getOrder, trackPackage));
        return goal;
    }

    private AgentGoal buildAgentSelectionGoal() {
        AgentGoal goal = new AgentGoal();
        goal.setId("goal_choose_agent_type");
        goal.setCategoryTag("core");
        goal.setAgentName("Select Agent Type");
        goal.setAgentFriendlyDescription("Help the user choose which agent to interact with.");
        goal.setDescription(String.join(" ",
                "Act as an agent concierge who learns what the user wants to achieve and pairs them with the best catalog goal.",
                "Ask about their objective, timing, and any constraints, then recommend the most relevant agent.",
                "Offer to switch them into that agent once they agree.",
                "Keep your tone warm, concise, and focused on moving them forward."));
        goal.setStarterPrompt(String.join(" ",
                "Warmly welcome the user, explain that you can match them with the right assistant from our catalog,",
                "and ask them to share what they need help accomplishing."));
        goal.setExampleConversationHistory(String.join("\n",
                "user: I want help tracking an order",
                "agent: Happy to help! I can bring in our order tracking assistant. Are you trying to check a specific order?",
                "user: Yes, order 102.",
                "agent: Perfect. I'll hand things off to the order status assistant so we can look that up together."));
        goal.setTools(Collections.emptyList());
        return goal;
    }

    private AgentGoal buildPandadocAutomationGoal() {
        ToolDefinition listTemplates = fetchTool("ListPandadocTemplates");
        ToolDefinition createDocument = fetchTool("CreatePandadocDocument");
        ToolDefinition sendDocument = fetchTool("SendPandadocDocument");
        ToolDefinition getStatus = fetchTool("GetPandadocDocumentStatus");
        if (listTemplates == null || createDocument == null || sendDocument == null || getStatus == null) {
            return null;
        }
        AgentGoal goal = new AgentGoal();
        goal.setId("goal_pandadoc_automation");
        goal.setCategoryTag("documents");
        goal.setAgentName("PandaDoc Agreement Automation");
        goal.setAgentFriendlyDescription("Prepare, send, and track PandaDoc agreements end-to-end.");
        goal.setDescription(String.join(" ",
                "Guide the user through automating a PandaDoc agreement from start to finish.",
                "Begin by offering to list templates and confirming the correct one.",
                "Collect signer details and any required token values before creating the draft with CreatePandadocDocument.",
                "After creation, send the document with SendPandadocDocument and monitor status with GetPandadocDocumentStatus.",
                "Explain each step plainly, stay concise, and confirm before moving on."));
        goal.setStarterPrompt(String.join(" ",
                "Introduce yourself as the PandaDoc automation assistant, offer to list available templates,",
                "and ask for the document details or recipients to get started."));
        goal.setExampleConversationHistory(String.join("\n",
                "user: Can you create an NDA for Acme?",
                "agent: Absolutely! I can walk you through it. Want me to pull up the available templates first?",
                "user: Yes, show me what's available.",
                "agent: Here are the top options... Which template should we use and who needs to sign?"));
        goal.setTools(List.of(listTemplates, createDocument, sendDocument, getStatus));
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

    private ToolDefinition fetchTool(String name) {
        ToolDefinition definition = toolRegistry.get(name);
        if (definition == null) {
            LOGGER.debug("Tool '{}' is not registered; skipping goal wiring", name);
            return null;
        }
        return cloneDefinition(definition);
    }
}
