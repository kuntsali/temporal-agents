package io.temporal.agent.model.workflow;

import io.temporal.agent.model.tools.AgentGoal;
import java.io.Serializable;

public class CombinedInput implements Serializable {

    private AgentGoalWorkflowParams toolParams;
    private AgentGoal agentGoal;

    public CombinedInput() {
    }

    public CombinedInput(AgentGoalWorkflowParams toolParams, AgentGoal agentGoal) {
        this.toolParams = toolParams;
        this.agentGoal = agentGoal;
    }

    public AgentGoalWorkflowParams getToolParams() {
        return toolParams;
    }

    public void setToolParams(AgentGoalWorkflowParams toolParams) {
        this.toolParams = toolParams;
    }

    public AgentGoal getAgentGoal() {
        return agentGoal;
    }

    public void setAgentGoal(AgentGoal agentGoal) {
        this.agentGoal = agentGoal;
    }
}
