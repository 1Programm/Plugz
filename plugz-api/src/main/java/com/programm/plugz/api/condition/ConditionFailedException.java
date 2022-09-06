package com.programm.plugz.api.condition;

public class ConditionFailedException extends Exception {

    private final String condition;
    private final String reason;

    public ConditionFailedException(String condition, String reason) {
        this.condition = condition;
        this.reason = reason;
    }

    public String condition() {
        return condition;
    }

    public String reason() {
        return reason;
    }
}
