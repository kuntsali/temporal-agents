package io.temporal.agent.goals;

import static org.assertj.core.api.Assertions.assertThat;

import io.temporal.agent.config.PandadocProperties;
import io.temporal.agent.tools.EcommerceToolsConfiguration;
import io.temporal.agent.tools.PandadocToolsConfiguration;
import io.temporal.agent.tools.ToolRegistry;
import io.temporal.agent.tools.pandadoc.DefaultPandadocClient;
import io.temporal.agent.tools.pandadoc.PandadocClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GoalRegistryTest {

    private GoalRegistry goalRegistry;

    @BeforeEach
    void setup() {
        ToolRegistry registry = new ToolRegistry();
        new EcommerceToolsConfiguration(registry);
        PandadocProperties properties = new PandadocProperties(null, "https://api.pandadoc.com/public/v1", 30);
        PandadocClient client = new DefaultPandadocClient(RestClient.builder(), properties);
        new PandadocToolsConfiguration(registry, client);
        goalRegistry = new GoalRegistry(registry);
    }

    @Test
    void goalsLoaded() {
        assertThat(goalRegistry.listGoals()).isNotEmpty();
        assertThat(goalRegistry.findGoal("goal_ecomm_order_status")).isNotNull();
        assertThat(goalRegistry.findGoal("goal_pandadoc_automation")).isNotNull();
    }
}
