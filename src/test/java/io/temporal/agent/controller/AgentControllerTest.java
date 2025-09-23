package io.temporal.agent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.temporal.agent.service.AgentService;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    @Mock
    private AgentService agentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AgentController(agentService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void startConversationWithoutBodyUsesDefaults() throws Exception {
        when(agentService.startConversation(null, null)).thenReturn("workflow-id");

        mockMvc.perform(post("/api/agent/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowId").value("workflow-id"));
    }

    @Test
    void getHistoryWhenWorkflowMissingReturns404() throws Exception {
        String workflowId = "missing";
        when(agentService.getHistory(workflowId)).thenThrow(new WorkflowNotFoundException(
                WorkflowExecution.newBuilder().setWorkflowId(workflowId).build(),
                "not found",
                null));

        mockMvc.perform(get("/api/agent/{workflowId}/history", workflowId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Workflow not found"));
    }

    @Test
    void startConversationWithInvalidGoalReturns400() throws Exception {
        when(agentService.startConversation(any(), any()))
                .thenThrow(new IllegalArgumentException("Unknown goal"));

        mockMvc.perform(post("/api/agent/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"invalid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown goal"));
    }
}
