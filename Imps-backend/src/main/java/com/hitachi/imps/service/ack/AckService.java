package com.hitachi.imps.service.ack;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

/**
 * Service to build ACK (Acknowledgement) messages as per IMPS specification.
 * ACK is sent immediately upon receiving any request.
 */
@Service
public class AckService {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    /**
     * Build ACK XML as per IMPS specification
     * Format: <ns2:Ack xmlns:ns2="http://npci.org/upi/schema/" api="ApiName" reqMsgId="msgId" ts="timestamp"/>
     */
    public String buildAck(String api, String msgId) {
        String timestamp = OffsetDateTime.now().format(TS_FORMAT);
        return """
            <ns2:Ack xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/" api="%s" reqMsgId="%s" ts="%s"></ns2:Ack>
            """.formatted(api, msgId, timestamp).trim();
    }

    /**
     * Build ACK for ReqPay
     */
    public String buildReqPayAck(String msgId) {
        return buildAck("ReqPay", msgId);
    }

    /**
     * Build ACK for RespPay
     */
    public String buildRespPayAck(String msgId) {
        return buildAck("RespPay", msgId);
    }

    /**
     * Build ACK for ReqChkTxn
     */
    public String buildReqChkTxnAck(String msgId) {
        return buildAck("ReqChkTxn", msgId);
    }

    /**
     * Build ACK for RespChkTxn
     */
    public String buildRespChkTxnAck(String msgId) {
        return buildAck("RespChkTxn", msgId);
    }

    /**
     * Build ACK for ReqHbt
     */
    public String buildReqHbtAck(String msgId) {
        return buildAck("ReqHbt", msgId);
    }

    /**
     * Build ACK for RespHbt
     */
    public String buildRespHbtAck(String msgId) {
        return buildAck("RespHbt", msgId);
    }

    /**
     * Build ACK for ReqValAdd
     */
    public String buildReqValAddAck(String msgId) {
        return buildAck("ReqValAdd", msgId);
    }

    /**
     * Build ACK for RespValAdd
     */
    public String buildRespValAddAck(String msgId) {
        return buildAck("RespValAdd", msgId);
    }

    /**
     * Build ACK for ReqListAccPvd
     */
    public String buildReqListAccPvdAck(String msgId) {
        return buildAck("ReqListAccPvd", msgId);
    }

    /**
     * Build ACK for RespListAccPvd
     */
    public String buildRespListAccPvdAck(String msgId) {
        return buildAck("RespListAccPvd", msgId);
    }
}

