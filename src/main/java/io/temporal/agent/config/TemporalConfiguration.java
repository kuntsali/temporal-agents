package io.temporal.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.agent.activities.ToolActivitiesImpl;
import io.temporal.agent.workflow.AgentGoalWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfiguration {

    @Bean
    public WorkflowServiceStubs workflowServiceStubs(TemporalProperties properties) {
        WorkflowServiceStubsOptions.Builder builder = WorkflowServiceStubsOptions.newBuilder();
        if (properties.address() != null && !properties.address().isBlank()) {
            builder.setTarget(properties.address());
        }
        return WorkflowServiceStubs.newInstance(builder.build());
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs, TemporalProperties properties) {
        WorkflowClientOptions options = WorkflowClientOptions.newBuilder()
                .setNamespace(properties.namespace())
                .build();
        return WorkflowClient.newInstance(workflowServiceStubs, options);
    }

    @Bean(destroyMethod = "shutdown")
    public WorkerFactory workerFactory(
            WorkflowClient workflowClient,
            ToolActivitiesImpl toolActivities,
            TemporalProperties properties) {
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
        Worker worker = factory.newWorker(properties.taskQueue());
        worker.registerWorkflowImplementationTypes(AgentGoalWorkflowImpl.class);
        worker.registerActivitiesImplementations(toolActivities);
        factory.start();
        return factory;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
