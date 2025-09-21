package io.temporal.agent.model.workflow;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public class ValidationResult implements Serializable {

    private boolean validationResult;
    private Map<String, Object> validationFailedReason = Collections.emptyMap();

    public ValidationResult() {
    }

    public ValidationResult(boolean validationResult, Map<String, Object> validationFailedReason) {
        this.validationResult = validationResult;
        if (validationFailedReason != null) {
            this.validationFailedReason = validationFailedReason;
        }
    }

    public boolean isValidationResult() {
        return validationResult;
    }

    public void setValidationResult(boolean validationResult) {
        this.validationResult = validationResult;
    }

    public Map<String, Object> getValidationFailedReason() {
        return validationFailedReason;
    }

    public void setValidationFailedReason(Map<String, Object> validationFailedReason) {
        this.validationFailedReason = validationFailedReason;
    }
}
