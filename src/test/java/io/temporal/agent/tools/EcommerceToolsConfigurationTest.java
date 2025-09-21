package io.temporal.agent.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EcommerceToolsConfigurationTest {

    private ToolRegistry registry;

    @BeforeEach
    void setup() {
        registry = new ToolRegistry();
        new EcommerceToolsConfiguration(registry);
    }

    @Test
    void listOrdersReturnsMatchingOrders() {
        Map<String, Object> result = registry.execute("ListOrders", Map.of("email", "matt.murdock@nelsonmurdock.com"));
        assertThat(result).containsKey("orders");
        List<?> orders = (List<?>) result.get("orders");
        assertThat(orders).isNotEmpty();
    }

    @Test
    void trackPackageReturnsTrackingInformation() {
        Map<String, Object> order = registry.execute("GetOrder", Map.of("order_id", "102"));
        assertThat(order).containsEntry("tracking_id", "039813852990618");
        Map<String, Object> tracking = registry.execute("TrackPackage", Map.of("tracking_id", "039813852990618"));
        assertThat(tracking).containsEntry("carrier", "USPS");
    }
}
