package io.temporal.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "temporal")
public class TemporalProperties {

    /** Temporal frontend endpoint. */
    private String address = "localhost:7233";

    /** Temporal namespace for the application. */
    private String namespace = "default";

    /** Task queue the worker and workflow will use. */
    private String taskQueue = "agent-task-queue";

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getTaskQueue() {
        return taskQueue;
    }

    public void setTaskQueue(String taskQueue) {
        this.taskQueue = taskQueue;
    }
}
