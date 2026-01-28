package com.hitachi.imps.exception;

public class NpciDuplicateTxnException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NpciDuplicateTxnException(String message) {
        super(message);
    }

	public String getApprovalNumber() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getReqMsgId() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getRrn() {
		// TODO Auto-generated method stub
		return null;
	}
}
