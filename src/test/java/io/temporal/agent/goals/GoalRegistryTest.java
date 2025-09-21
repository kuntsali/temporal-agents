package io.temporal.agent.goals;

import static org.assertj.core.api.Assertions.assertThat;

import io.temporal.agent.tools.EcommerceToolsConfiguration;
import io.temporal.agent.tools.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoalRegistryTest {

    private GoalRegistry goalRegistry;

    @BeforeEach
    void setup() {
        ToolRegistry registry = new ToolRegistry();
        new EcommerceToolsConfiguration(registry);
        goalRegistry = new GoalRegistry(registry);
    }

    @Test
    void goalsLoaded() {
        assertThat(goalRegistry.listGoals()).isNotEmpty();
        assertThat(goalRegistry.findGoal("goal_ecomm_order_status")).isNotNull();
    }
}
