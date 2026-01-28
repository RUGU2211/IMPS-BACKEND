package com.hitachi.imps.exception;

import java.util.UUID;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * NPCI Duplicate Transaction Handling
     * Response Code = 94 (MANDATORY)
     */
    @ExceptionHandler(NpciDuplicateTxnException.class)
    public String handleDuplicateTxn(NpciDuplicateTxnException ex) {
        return buildFailureRespPay(
            ex.getReqMsgId(),
            ex.getApprovalNumber(),
            ex.getRrn(),
            "94",
            "DUPLICATE_TRANSACTION"
        );
    }

    /**
     * Fallback - System Error
     */
    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex) {
        String approvalNum = String.valueOf(System.currentTimeMillis()).substring(7);
        String rrn = String.valueOf(System.currentTimeMillis()).substring(1, 13);

        return buildFailureRespPay(
            UUID.randomUUID().toString(),
            approvalNum,
            rrn,
            "96",
            "SYSTEM_ERROR"
        );
    }

    /**
     * Build failure RespPay XML response
     */
    private String buildFailureRespPay(String reqMsgId, String approvalNum, String rrn, 
                                       String errCode, String errMsg) {
        return """
            <ns2:RespPay xmlns:ns2="http://npci.org/upi/schema/">
                <Head ver="2.0" prodType="IMPS"/>
                <Resp reqMsgId="%s" result="FAILURE" approvalNum="%s" rrn="%s" errCode="%s">
                    <ErrMsg>%s</ErrMsg>
                </Resp>
            </ns2:RespPay>
            """.formatted(reqMsgId, approvalNum, rrn, errCode, errMsg);
    }
}
