package com.hitachi.imps.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown when any IMPS request (ReqPay, ReqChkTxn, ReqValAdd, ReqHbt, ReqListAccPvd) fails
 * IMPS Common Code Technical Specifications – Appendix (Rules) that apply to all txns:
 * Rule 019 Head_Version, 020 Head_ts, 021 Head_MsgId, 022 Txn_UUID.
 */
public class CommonCodeValidationException extends Exception {

    private final List<String> ruleIds = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();

    public CommonCodeValidationException(String ruleId, String message) {
        super(message);
        this.ruleIds.add(ruleId);
        this.messages.add(message);
    }

    public CommonCodeValidationException() {
        super("Common code validation failed");
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
