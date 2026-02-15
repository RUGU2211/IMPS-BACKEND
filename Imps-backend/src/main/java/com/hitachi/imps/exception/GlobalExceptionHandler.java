package com.hitachi.imps.exception;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.hitachi.imps.converter.RespPaySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${imps.org-id:BANK01}")
    private String orgId;

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    /** Rule 020: Head ts = ISO with up to 3 fractional seconds. */
    private static final DateTimeFormatter HEAD_TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    /**
     * Duplicate txn_id in URL/request - reject with 409.
     * txn_id must be unique in public.transaction.
     */
    @ExceptionHandler(DuplicateTxnIdException.class)
    public ResponseEntity<String> handleDuplicateTxnId(DuplicateTxnIdException ex) {
        String body = """
            <ns2:Error xmlns:ns2="http://npci.org/upi/schema/">
                <code>DUPLICATE_TXN_ID</code>
                <message>%s</message>
                <txnId>%s</txnId>
            </ns2:Error>
            """.formatted(ex.getMessage(), ex.getTxnId() != null ? ex.getTxnId() : "").trim();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Common code validation failed (Rules 019, 020, 021, 022) on any request.
     */
    @ExceptionHandler(CommonCodeValidationException.class)
    public ResponseEntity<String> handleCommonCodeValidation(CommonCodeValidationException ex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ex.getRuleIds().size(); i++) {
            String r = ex.getRuleIds().get(i);
            String m = i < ex.getMessages().size() ? ex.getMessages().get(i) : ex.getMessage();
            sb.append("<error ruleId=\"").append(r != null ? r : "").append("\">")
              .append(m != null ? m : "").append("</error>");
        }
        String body = """
            <ns2:Error xmlns:ns2="http://npci.org/upi/schema/">
                <code>COMMON_CODE_VALIDATION</code>
                <message>%s</message>
                %s
            </ns2:Error>
            """.formatted(ex.getMessage(), sb.toString()).trim();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Missing or blank reqMsgId (Head @msgId) - required for ACK.
     */
    @ExceptionHandler(InvalidReqMsgIdException.class)
    public ResponseEntity<String> handleInvalidReqMsgId(InvalidReqMsgIdException ex) {
        String body = """
            <ns2:Error xmlns:ns2="http://npci.org/upi/schema/">
                <code>INVALID_REQ_MSG_ID</code>
                <message>%s</message>
            </ns2:Error>
            """.formatted(ex.getMessage() != null ? ex.getMessage() : "reqMsgId is required for ACK").trim();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * ReqPay validation failed (NPCI Section 8.1 / 8.2 rules).
     */
    @ExceptionHandler(ReqPayValidationException.class)
    public ResponseEntity<String> handleReqPayValidation(ReqPayValidationException ex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ex.getRuleIds().size(); i++) {
            String r = ex.getRuleIds().get(i);
            String m = i < ex.getMessages().size() ? ex.getMessages().get(i) : ex.getMessage();
            sb.append("<error ruleId=\"").append(r != null ? r : "").append("\">")
              .append(m != null ? m : "").append("</error>");
        }
        String body = """
            <ns2:Error xmlns:ns2="http://npci.org/upi/schema/">
                <code>REQPAY_VALIDATION</code>
                <message>%s</message>
                %s
            </ns2:Error>
            """.formatted(ex.getMessage(), sb.toString()).trim();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Missing or empty request body (e.g. POST to /imps/reqpay/{txnId} with no body).
     * ISO endpoints need: Content-Type: application/octet-stream and binary ISO body.
     * XML endpoints need: Content-Type: application/xml and XML body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleMissingBody(HttpMessageNotReadableException ex) {
        log.warn("Request body missing or not readable: {}", ex.getMessage());
        String message = "Request body is required. "
            + "For ISO (reverse flow): use Content-Type: application/octet-stream and send binary ISO 8583 in body. "
            + "For XML (NPCI flow): use Content-Type: application/xml and send XML in body.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN).body(message);
    }

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
     * Fallback - System Error (errCode 96).
     * If the cause is missing body, return 400 instead of 96 (handles wrapped HttpMessageNotReadableException).
     */
    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof HttpMessageNotReadableException) {
                return handleMissingBody((HttpMessageNotReadableException) cause);
            }
            cause = cause.getCause();
        }
        log.error("IMPS system error (96 SYSTEM_ERROR). Check logs for root cause.", ex);
        String approvalNum = String.valueOf(System.currentTimeMillis()).substring(7);
        String rrn = String.valueOf(System.currentTimeMillis()).substring(1, 13);
        String reqMsgId = "REQ" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String errMsg = ex.getMessage() != null ? RespPaySpec.truncate(ex.getMessage(), 200) : "SYSTEM_ERROR";
        return buildFailureRespPay(reqMsgId, approvalNum, rrn, "96", errMsg);
    }

    /**
     * Build failure RespPay XML per NPCI spec tables:
     * Head (ver, ts Max255, orgId Max20, msgId Length35, prodType), Txn (id 35, note Max50, refId, refUrl, ts, type, subType, initiationMode Max3, refCategory Max2), Resp (reqMsgId 35, result, errCode Max20)
     */
    private String buildFailureRespPay(String reqMsgId, String approvalNum, String rrn,
                                       String errCode, String errMsg) {
        String ns = "http://npci.org/upi/schema/";
        ns = RespPaySpec.truncate(ns, RespPaySpec.XMLNS_MAX);
        // Rule 021: Head msgId 35 chars
        String msgId = "MSG" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        reqMsgId = RespPaySpec.exactLen(reqMsgId, RespPaySpec.RESP_REQMSGID_LEN, '0');
        String ts = OffsetDateTime.now().format(HEAD_TS_FORMAT);
        String txnId = RespPaySpec.exactLen("TXN" + System.currentTimeMillis(), RespPaySpec.TXN_ID_LEN, '0');
        String orgIdVal = RespPaySpec.truncate(orgId, RespPaySpec.HEAD_ORGID_MAX);
        String note = RespPaySpec.truncate("Failure", RespPaySpec.TXN_NOTE_MAX);
        String result = RespPaySpec.truncate("FAILURE", RespPaySpec.RESP_RESULT_MAX);
        errCode = RespPaySpec.truncate(errCode != null ? errCode : "96", RespPaySpec.RESP_ERRCODE_MAX);
        return """
            <ns2:RespPay xmlns:ns2="%s">
                <Head ver="2.0" ts="%s" orgId="%s" msgId="%s" prodType="%s"/>
                <Txn id="%s" note="%s" refId="" refUrl="" ts="%s" type="PAY" subType="PAY" initiationMode="API" refCategory="00"/>
                <Resp reqMsgId="%s" result="%s" errCode="%s">
                    <ErrMsg>%s</ErrMsg>
                </Resp>
            </ns2:RespPay>
            """.formatted(ns, ts, orgIdVal, msgId, RespPaySpec.PRODTYPE_FIXED, txnId, note, ts, reqMsgId, result, errCode, errMsg != null ? errMsg : "");
    }
}
