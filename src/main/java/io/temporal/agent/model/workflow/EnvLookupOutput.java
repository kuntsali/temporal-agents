package io.temporal.agent.model.workflow;

import java.io.Serializable;

public class EnvLookupOutput implements Serializable {

    private boolean showConfirm;
    private boolean multiGoalMode;

    public EnvLookupOutput() {
    }

    public EnvLookupOutput(boolean showConfirm, boolean multiGoalMode) {
        this.showConfirm = showConfirm;
        this.multiGoalMode = multiGoalMode;
    }

    public boolean isShowConfirm() {
        return showConfirm;
    }

    public void setShowConfirm(boolean showConfirm) {
        this.showConfirm = showConfirm;
    }

    public boolean isMultiGoalMode() {
        return multiGoalMode;
    }

    public void setMultiGoalMode(boolean multiGoalMode) {
        this.multiGoalMode = multiGoalMode;
    }
}
