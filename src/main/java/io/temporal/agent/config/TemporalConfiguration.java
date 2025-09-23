package io.temporal.agent.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.agent.activities.ToolActivitiesImpl;
import io.temporal.agent.workflow.AgentGoalWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.util.List;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

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
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.createXmlMapper(false).build();
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Bean
    public RestClientCustomizer restClientCustomizer(ObjectMapper objectMapper) {
        return builder -> builder.messageConverters(converters -> replaceJacksonMapper(converters, objectMapper));
    }

    private void replaceJacksonMapper(List<HttpMessageConverter<?>> converters, ObjectMapper objectMapper) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                jacksonConverter.setObjectMapper(objectMapper);
            }
        }
    }
}
