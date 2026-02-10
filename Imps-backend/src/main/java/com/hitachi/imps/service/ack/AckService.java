package com.hitachi.imps.service.ack;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Service;

import com.hitachi.imps.exception.InvalidReqMsgIdException;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.util.IsoUtil;

/**
 * Service to build ACK (Acknowledgement) messages as per IMPS specification.
 * All formats must follow NPCI_IMPS_Message_Formats.md (project root).
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
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMdd");

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

    /** Build failure RespPay when institution (IFSC) validation fails. respCode MJ = INVALID IFSC. */
    public String buildFailureRespPay(String reqMsgId, String respCode, String errMsg) {
        String ts = OffsetDateTime.now().format(TS_FORMAT);
        String respMsgId = com.hitachi.imps.service.util.ResponseIdHelper.responseMsgIdFromRequest(reqMsgId != null ? reqMsgId : "REQ");
        String req = reqMsgId != null && reqMsgId.length() >= 35 ? reqMsgId.substring(0, 35) : (reqMsgId != null ? reqMsgId : "");
        String code = respCode != null ? respCode : "MJ";
        String ns = "http://npci.org/upi/schema/";
        return "<ns2:RespPay xmlns:ns2=\"" + ns + "\"><Head ver=\"2.0\" ts=\"" + ts + "\" orgId=\"BANK01\" msgId=\"" + respMsgId + "\" prodType=\"IMPS\"/><Txn id=\"" + req + "\" note=\"Failure\" refId=\"\" refUrl=\"\" ts=\"" + ts + "\" type=\"PAY\" subType=\"PAY\" initiationMode=\"API\" refCategory=\"00\"/><Resp reqMsgId=\"" + escapeXmlAttr(req) + "\" result=\"FAILURE\" errCode=\"" + escapeXmlAttr(code) + "\"><ErrMsg>" + escapeXmlAttr(errMsg != null ? errMsg : "") + "</ErrMsg></Resp></ns2:RespPay>";
    }

    /** Build failure RespChkTxn (institution validation or BANK_DOWN). Includes ErrMsg when provided. */
    public String buildFailureRespChkTxn(String reqMsgId, String respCode, String errMsg) {
        return buildFailureRespWithErrMsg("RespChkTxn", reqMsgId, respCode != null ? respCode : "MJ", errMsg);
    }

    /** Build failure RespValAdd (institution validation or BANK_DOWN). Includes ErrMsg when provided. */
    public String buildFailureRespValAdd(String reqMsgId, String respCode, String errMsg) {
        return buildFailureRespWithErrMsg("RespValAdd", reqMsgId, respCode != null ? respCode : "MJ", errMsg);
    }

    /**
     * Build ISO ACK (0810) when IMPS receives an ISO response from Switch.
     * Copies key fields from the received message so Switch can match the transaction; DE39=00 (approved/ack).
     */
    public byte[] buildIsoAckFromResponse(byte[] receivedIso) {
        if (receivedIso == null || receivedIso.length == 0) return null;
        try {
            ISOMsg received = IsoUtil.unpack(receivedIso, new ImpsIsoPackager());
            ISOMsg ack = new ISOMsg();
            ack.setPackager(new ImpsIsoPackager());
            ack.setMTI("0810");
            copyField(received, ack, 3);
            copyField(received, ack, 24);
            copyField(received, ack, 37);
            copyField(received, ack, 41);
            copyField(received, ack, 120);
            ack.set(11, received.hasField(11) ? received.getString(11) : String.format("%06d", System.currentTimeMillis() % 1_000_000));
            ack.set(12, LocalDateTime.now().format(TIME_FMT));
            ack.set(13, LocalDateTime.now().format(DATE_FMT));
            ack.set(39, "00");
            return IsoUtil.pack(ack);
        } catch (Exception e) {
            return null;
        }
    }

    private static void copyField(ISOMsg src, ISOMsg dst, int field) throws ISOException {
        if (src.hasField(field)) dst.set(field, src.getString(field));
    }

    /**
     * Build failure response ISO (0210) for Switch flow when validation fails.
     * Copies 3, 37, 41, 120 from request; sets DE39=96 (system/validation error).
     */
    public byte[] buildFailureRespIso(byte[] reqIsoBytes, String errMsg) {
        try {
            ISOMsg req = IsoUtil.unpack(reqIsoBytes, new ImpsIsoPackager());
            ISOMsg resp = new ISOMsg();
            resp.setPackager(new ImpsIsoPackager());
            resp.setMTI("0210");
            if (req.hasField(3)) resp.set(3, req.getString(3));
            if (req.hasField(4)) resp.set(4, req.getString(4));
            if (req.hasField(37)) resp.set(37, req.getString(37));
            if (req.hasField(41)) resp.set(41, req.getString(41));
            if (req.hasField(120)) resp.set(120, req.getString(120));
            resp.set(11, req.hasField(11) ? req.getString(11) : String.format("%06d", System.currentTimeMillis() % 1_000_000));
            resp.set(12, LocalDateTime.now().format(TIME_FMT));
            resp.set(13, LocalDateTime.now().format(DATE_FMT));
            resp.set(38, "000000");
            resp.set(39, "96");
            return IsoUtil.pack(resp);
        } catch (Exception e) {
            return null;
        }
    }

    /** Build failure response XML with optional ErrMsg (proper NPCI format). */
    private String buildFailureRespWithErrMsg(String api, String reqMsgId, String respCode, String errMsg) {
        String ts = OffsetDateTime.now().format(TS_FORMAT);
        String respMsgId = com.hitachi.imps.service.util.ResponseIdHelper.responseMsgIdFromRequest(reqMsgId != null ? reqMsgId : "REQ");
        String req = reqMsgId != null && reqMsgId.length() >= 35 ? reqMsgId.substring(0, 35) : (reqMsgId != null ? reqMsgId : "");
        String ns = "http://npci.org/upi/schema/";
        String errContent = (errMsg != null && !errMsg.isBlank()) ? "<ErrMsg>" + escapeXmlAttr(errMsg) + "</ErrMsg>" : "";
        return "<ns2:" + api + " xmlns:ns2=\"" + ns + "\"><Head ver=\"2.0\" ts=\"" + ts + "\" orgId=\"BANK01\" msgId=\"" + respMsgId + "\" prodType=\"IMPS\"/><Resp reqMsgId=\"" + escapeXmlAttr(req) + "\" result=\"FAILURE\" errCode=\"" + escapeXmlAttr(respCode) + "\">" + errContent + "</Resp></ns2:" + api + ">";
    }
}
