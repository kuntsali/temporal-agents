package io.temporal.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "temporal")
public record TemporalProperties(
        @DefaultValue("localhost:7233") String address,
        @DefaultValue("default") String namespace,
        @DefaultValue("agent-task-queue") String taskQueue) {}
