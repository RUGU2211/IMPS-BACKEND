package com.hitachi.imps.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown when ReqPay message fails NPCI rules/validations (Section 8.1 / 8.2).
 * Holds rule IDs and messages for the response.
 */
public class ReqPayValidationException extends Exception {

    private final List<String> ruleIds = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();

    public ReqPayValidationException(String ruleId, String message) {
        super(message);
        this.ruleIds.add(ruleId);
        this.messages.add(message);
    }

    public ReqPayValidationException() {
        super("ReqPay validation failed");
    }

    public void addError(String ruleId, String message) {
        ruleIds.add(ruleId);
        messages.add(message);
    }

    public boolean hasErrors() {
        return !ruleIds.isEmpty();
    }

    public List<String> getRuleIds() {
        return new ArrayList<>(ruleIds);
    }

    public List<String> getMessages() {
        return new ArrayList<>(messages);
    }
}
