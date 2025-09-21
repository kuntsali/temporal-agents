package io.temporal.agent.model.workflow;

import java.io.Serializable;

public class EnvLookupInput implements Serializable {

    private String showConfirmEnvVarName;
    private boolean showConfirmDefault;

    public EnvLookupInput() {
    }

    public EnvLookupInput(String showConfirmEnvVarName, boolean showConfirmDefault) {
        this.showConfirmEnvVarName = showConfirmEnvVarName;
        this.showConfirmDefault = showConfirmDefault;
    }

    public String getShowConfirmEnvVarName() {
        return showConfirmEnvVarName;
    }

    public void setShowConfirmEnvVarName(String showConfirmEnvVarName) {
        this.showConfirmEnvVarName = showConfirmEnvVarName;
    }

    public boolean isShowConfirmDefault() {
        return showConfirmDefault;
    }

    public void setShowConfirmDefault(boolean showConfirmDefault) {
        this.showConfirmDefault = showConfirmDefault;
    }
}
