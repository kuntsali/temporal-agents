package io.temporal.agent.controller.dto;

import jakarta.validation.constraints.NotBlank;

public class SelectGoalRequest {

    @NotBlank
    private String goalId;

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }
}
