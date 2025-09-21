package io.temporal.agent.model.workflow;

public enum NextStep {
    CONFIRM("confirm"),
    QUESTION("question"),
    PICK_NEW_GOAL("pick-new-goal"),
    DONE("done");

    private final String jsonValue;

    NextStep(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    public String getJsonValue() {
        return jsonValue;
    }

    public static NextStep fromValue(String value) {
        if (value == null) {
            return QUESTION;
        }
        return switch (value) {
            case "confirm" -> CONFIRM;
            case "question" -> QUESTION;
            case "pick-new-goal" -> PICK_NEW_GOAL;
            case "done" -> DONE;
            default -> QUESTION;
        };
    }
}
