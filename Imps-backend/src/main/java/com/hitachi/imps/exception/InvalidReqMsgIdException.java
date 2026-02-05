package com.hitachi.imps.exception;

/**
 * Thrown when reqMsgId (Head @msgId from request XML) is missing or blank.
 * ACK requires a valid reqMsgId to identify which request is being acknowledged.
 */
public class InvalidReqMsgIdException extends IllegalArgumentException {

    public InvalidReqMsgIdException(String message) {
        super(message);
    }

    public InvalidReqMsgIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
