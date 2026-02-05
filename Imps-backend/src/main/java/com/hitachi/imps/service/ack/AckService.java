package com.hitachi.imps.service.ack;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.hitachi.imps.exception.InvalidReqMsgIdException;

/**
 * Service to build ACK (Acknowledgement) messages as per IMPS specification.
 * ACK is returned to NPCI from IMPS immediately when a request/response lands,
 * to confirm receipt and that processing has started.
 *
 * Return format (exact):
 * <ns2:Ack xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/"
 * api="ReqPay" reqMsgId="" ts=""></ns2:Ack>
 *
 * - api: identifies which request/response this ACK is for (ReqPay, RespPay, ReqChkTxn, etc.).
 * - reqMsgId: message ID from the request being acknowledged (Head @msgId); validated like txn_id (required, non-blank when required).
 * - ts: ISO timestamp.
 */
@Service
public class AckService {

    private static final String XMLNS_NS2 = "http://npci.org/upi/schema/";
    private static final String XMLNS_NS3 = "http://npci.org/cm/schema/";
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    /**
     * Build ACK XML. reqMsgId must be non-blank (validated like txn_id).
     * api must be the message type being acknowledged: ReqPay, RespPay, ReqChkTxn, RespChkTxn, ReqHbt, RespHbt, ReqListAccPvd, RespListAccPvd, ReqValAdd, RespValAdd.
     *
     * @param api    the API/message type (e.g. ReqPay, RespPay)
     * @param reqMsgId message ID from the request (Head @msgId); must not be null or blank
     * @return single-line ACK XML
     * @throws InvalidReqMsgIdException if reqMsgId is null or blank
     */
    public String buildAck(String api, String reqMsgId) {
        if (reqMsgId == null || reqMsgId.isBlank())
            throw new InvalidReqMsgIdException("reqMsgId is required for ACK and must not be blank");
        String ts = OffsetDateTime.now().format(TS_FORMAT);
        String escapedReqMsgId = escapeXmlAttr(reqMsgId.trim());
        String escapedApi = escapeXmlAttr(api != null ? api : "");
        return "<ns2:Ack xmlns:ns2=\"" + XMLNS_NS2 + "\" xmlns:ns3=\"" + XMLNS_NS3 + "\" api=\"" + escapedApi + "\" reqMsgId=\"" + escapedReqMsgId + "\" ts=\"" + ts + "\"></ns2:Ack>";
    }

    /**
     * Build ACK when reqMsgId may be blank (e.g. Switch callback uses txnId). Uses txnId as reqMsgId; if both blank, throws.
     */
    public String buildAckWithFallback(String api, String reqMsgId, String fallbackReqMsgId) {
        String id = (reqMsgId != null && !reqMsgId.isBlank()) ? reqMsgId : fallbackReqMsgId;
        if (id == null || id.isBlank())
            throw new InvalidReqMsgIdException("reqMsgId or fallback (txnId) is required for ACK");
        return buildAck(api, id);
    }

    private static String escapeXmlAttr(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    public String buildReqPayAck(String reqMsgId) { return buildAck("ReqPay", reqMsgId); }
    public String buildRespPayAck(String reqMsgId) { return buildAck("RespPay", reqMsgId); }
    public String buildReqChkTxnAck(String reqMsgId) { return buildAck("ReqChkTxn", reqMsgId); }
    public String buildRespChkTxnAck(String reqMsgId) { return buildAck("RespChkTxn", reqMsgId); }
    public String buildReqHbtAck(String reqMsgId) { return buildAck("ReqHbt", reqMsgId); }
    public String buildRespHbtAck(String reqMsgId) { return buildAck("RespHbt", reqMsgId); }
    public String buildReqValAddAck(String reqMsgId) { return buildAck("ReqValAdd", reqMsgId); }
    public String buildRespValAddAck(String reqMsgId) { return buildAck("RespValAdd", reqMsgId); }
    public String buildReqListAccPvdAck(String reqMsgId) { return buildAck("ReqListAccPvd", reqMsgId); }
    public String buildRespListAccPvdAck(String reqMsgId) { return buildAck("RespListAccPvd", reqMsgId); }
}

