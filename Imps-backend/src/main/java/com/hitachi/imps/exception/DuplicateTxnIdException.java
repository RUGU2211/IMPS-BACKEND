package com.hitachi.imps.exception;

/**
 * Thrown when a request carries a txn_id that already exists in public.transaction.
 * Used to reject duplicate transactions (HTTP 409).
 */
public class DuplicateTxnIdException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String txnId;

    public DuplicateTxnIdException(String txnId) {
        super("Duplicate txn_id: " + txnId);
        this.txnId = txnId;
    }

    public String getTxnId() {
        return txnId;
    }
}
