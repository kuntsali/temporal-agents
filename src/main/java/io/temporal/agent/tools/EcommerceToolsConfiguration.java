package io.temporal.agent.tools;

import io.temporal.agent.model.tools.ToolArgument;
import io.temporal.agent.model.tools.ToolDefinition;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class EcommerceToolsConfiguration {

    private final Map<String, OrderRecord> orders = new HashMap<>();
    private final Map<String, TrackingRecord> tracking = new HashMap<>();

    public EcommerceToolsConfiguration(ToolRegistry toolRegistry) {
        seedData();
        toolRegistry.register(createListOrdersTool(), this::listOrders);
        toolRegistry.register(createGetOrderTool(), this::getOrder);
        toolRegistry.register(createTrackPackageTool(), this::trackPackage);
    }

    private void seedData() {
        orders.put("102", new OrderRecord("102", "Red Sunglasses", "matt.murdock@nelsonmurdock.com",
                "shipped", LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 6), "039813852990618"));
        orders.put("103", new OrderRecord("103", "Blue Sunglasses", "matt.murdock@nelsonmurdock.com",
                "paid", LocalDate.of(2025, 4, 3), LocalDate.of(2025, 4, 7), null));
        orders.put("105", new OrderRecord("105", "Black Hoodie", "jessica.jones@aliasinvestigations.com",
                "processing", LocalDate.of(2025, 4, 8), LocalDate.of(2025, 4, 9), null));

        tracking.put("039813852990618", new TrackingRecord("USPS", LocalDate.of(2025, 4, 30),
                "Your item has left our acceptance facility and is in transit to a sorting facility on April 10, 2025 at 7:06 am in IRON RIDGE, WI 53035.",
                "https://tools.usps.com/go/TrackConfirmAction?qtc_tLabels1=039813852990618",
                LocalDate.of(2025, 4, 10).atTime(7, 6)));
    }

    private ToolDefinition createListOrdersTool() {
        List<ToolArgument> args = List.of(
                new ToolArgument("email", "string", "Email address for the customer whose orders to return"),
                new ToolArgument("limit", "number", "Maximum number of results to return. Defaults to 20."));
        return new ToolDefinition("ListOrders", "List a customer's past orders", args);
    }

    private Map<String, Object> listOrders(Map<String, Object> args) {
        String email = Optional.ofNullable(args.get("email"))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("email is required"));
        int limit = Optional.ofNullable(args.get("limit")).map(Object::toString).map(Integer::parseInt).orElse(20);
        List<Map<String, Object>> matching = orders.values().stream()
                .filter(order -> order.email().equalsIgnoreCase(email))
                .limit(limit)
                .map(OrderRecord::toMap)
                .collect(Collectors.toCollection(ArrayList::new));
        Map<String, Object> response = new HashMap<>();
        response.put("orders", matching);
        response.put("total", matching.size());
        return response;
    }

    private ToolDefinition createGetOrderTool() {
        List<ToolArgument> args = List.of(
                new ToolArgument("order_id", "string", "Identifier for the order to retrieve"));
        return new ToolDefinition("GetOrder", "Fetch a specific order by id", args);
    }

    private Map<String, Object> getOrder(Map<String, Object> args) {
        String orderId = Optional.ofNullable(args.get("order_id"))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("order_id is required"));
        OrderRecord record = Optional.ofNullable(orders.get(orderId))
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return record.toMap();
    }

    private ToolDefinition createTrackPackageTool() {
        List<ToolArgument> args = List.of(
                new ToolArgument("tracking_id", "string", "Tracking identifier for the package"));
        return new ToolDefinition("TrackPackage", "Track the shipment associated with an order", args);
    }

    private Map<String, Object> trackPackage(Map<String, Object> args) {
        String trackingId = Optional.ofNullable(args.get("tracking_id"))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("tracking_id is required"));
        TrackingRecord record = Optional.ofNullable(tracking.get(trackingId))
                .orElseThrow(() -> new IllegalArgumentException("Tracking not found: " + trackingId));
        return record.toMap();
    }

    private record OrderRecord(String id,
                               String summary,
                               String email,
                               String status,
                               LocalDate orderDate,
                               LocalDate lastOrderUpdate,
                               String trackingId) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("summary", summary);
            map.put("email", email);
            map.put("status", status);
            map.put("order_date", orderDate.toString());
            map.put("last_order_update", lastOrderUpdate.toString());
            if (trackingId != null) {
                map.put("tracking_id", trackingId);
            }
            return map;
        }
    }

    private record TrackingRecord(String carrier,
                                  LocalDate scheduledDeliveryDate,
                                  String statusSummary,
                                  String trackingLink,
                                  java.time.LocalDateTime lastTrackingUpdate) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("carrier", carrier);
            map.put("scheduled_delivery_date", scheduledDeliveryDate.toString());
            map.put("status_summary", statusSummary);
            map.put("tracking_link", trackingLink);
            map.put("last_tracking_update", lastTrackingUpdate.toString());
            return map;
        }
    }
}
