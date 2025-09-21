package io.temporal.agent.controller;

import io.temporal.agent.controller.dto.PromptRequest;
import io.temporal.agent.controller.dto.SelectGoalRequest;
import io.temporal.agent.controller.dto.StartConversationRequest;
import io.temporal.agent.controller.dto.StartConversationResponse;
import io.temporal.agent.model.conversation.ConversationHistory;
import io.temporal.agent.model.tools.AgentGoal;
import io.temporal.agent.model.workflow.ToolDecision;
import io.temporal.agent.service.AgentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/start")
    public StartConversationResponse startConversation(@RequestBody StartConversationRequest request) {
        String workflowId = agentService.startConversation(request.getWorkflowId(), request.getGoalId());
        return new StartConversationResponse(workflowId);
    }

    @PostMapping("/{workflowId}/prompt")
    public ResponseEntity<Void> submitPrompt(@PathVariable String workflowId, @Valid @RequestBody PromptRequest request) {
        agentService.sendPrompt(workflowId, request.getPrompt());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{workflowId}/confirm")
    public ResponseEntity<Void> confirmTool(@PathVariable String workflowId) {
        agentService.confirmTool(workflowId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{workflowId}/end")
    public ResponseEntity<Void> endConversation(@PathVariable String workflowId) {
        agentService.endConversation(workflowId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{workflowId}/goal")
    public ResponseEntity<Void> selectGoal(@PathVariable String workflowId, @Valid @RequestBody SelectGoalRequest request) {
        agentService.selectGoal(workflowId, request.getGoalId());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{workflowId}/history")
    public ConversationHistory getHistory(@PathVariable String workflowId) {
        return agentService.getHistory(workflowId);
    }

    @GetMapping("/{workflowId}/tool")
    public ToolDecision getToolDecision(@PathVariable String workflowId) {
        return agentService.getToolDecision(workflowId);
    }

    @GetMapping("/{workflowId}/goal")
    public AgentGoal getCurrentGoal(@PathVariable String workflowId) {
        return agentService.getCurrentGoal(workflowId);
    }

    @GetMapping("/goals")
    public List<AgentGoal> listGoals() {
        return agentService.listGoals();
    }
}
