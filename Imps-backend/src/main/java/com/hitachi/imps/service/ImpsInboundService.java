package com.hitachi.imps.service;

import com.hitachi.imps.exception.CommonCodeValidationException;
import com.hitachi.imps.exception.InvalidReqMsgIdException;
import com.hitachi.imps.exception.ReqPayValidationException;
import com.hitachi.imps.service.ack.AckSender;
import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.chktxn.ReqChkTxnService;
import com.hitachi.imps.service.heartbeat.ReqHbtService;
import com.hitachi.imps.service.listaccpvd.ReqListAccPvdService;
import com.hitachi.imps.service.listaccpvd.RespListAccPvdService;
import com.hitachi.imps.service.pay.ReqPayService;
import com.hitachi.imps.service.pay.ReqPayValidationService;
import com.hitachi.imps.service.pay.RespPayService;
import com.hitachi.imps.service.chktxn.RespChkTxnService;
import com.hitachi.imps.service.valadd.ReqValAddService;
import com.hitachi.imps.service.valadd.RespValAddService;
import com.hitachi.imps.service.validation.CommonCodeValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Inbound request handling. ACK is sent via the provided AckSender (REST to NPCI).
 */
@Service
public class ImpsInboundService {

    @Autowired private AckService ackService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private TransactionService transactionService;
    @Autowired private CommonCodeValidationService commonCodeValidationService;
    @Autowired private ReqPayValidationService reqPayValidationService;
    @Autowired private ReqPayService reqPayService;
    @Autowired private ReqChkTxnService reqChkTxnService;
    @Autowired private ReqHbtService reqHbtService;
    @Autowired private ReqListAccPvdService reqListAccPvdService;
    @Autowired private RespListAccPvdService respListAccPvdService;
    @Autowired private ReqValAddService reqValAddService;
    @Autowired private RespValAddService respValAddService;
    @Autowired private RespPayService respPayService;
    @Autowired private RespChkTxnService respChkTxnService;

    /** Resolve txn_id once at entry: path → Txn @id in XML → reqMsgId. */
    private String resolveTxnId(String pathTxnId, String xml, String reqMsgId) {
        if (pathTxnId != null && !pathTxnId.isBlank()) return pathTxnId;
        String fromXml = xmlParsingService.extractTxnId(xml);
        if (fromXml != null && !fromXml.isBlank()) return fromXml;
        return (reqMsgId != null && !reqMsgId.isBlank()) ? reqMsgId : pathTxnId;
    }

    /** Validate txn_id once at entry (non-blank + not duplicate in DB). Only call for Req* flows. */
    public void validateNewTxnId(String txnId) {
        if (txnId == null || txnId.isBlank())
            throw new IllegalArgumentException("txnId must not be blank");
        transactionService.validateNewTxnId(txnId);
    }

    /** Extract and validate msg_id once at entry; send ACK and run process with same reqMsgId. */
    private void ackAndProcess(String xml, String txnId, String reqMsgId, String apiName, String reqType, AckSender ackSender, Runnable process) {
        if (reqMsgId == null || reqMsgId.isBlank())
            throw new InvalidReqMsgIdException("reqMsgId (Head @msgId) is required for ACK and must not be blank");
        System.out.println("[IMPS] ACK sent to NPCI " + reqType + "/" + txnId);
        String ack = ackService.buildAck(apiName, reqMsgId);
        ackSender.send(ack);
        process.run();
    }

    /** Single place for msg_id and txn_id: extract/resolve once at entry, validate once, pass downstream. */
    public String handleReqPay(String xml, String pathTxnId, AckSender ackSender) throws ReqPayValidationException {
        String reqMsgId = xmlParsingService.extractMsgId(xml);
        if (reqMsgId == null || reqMsgId.isBlank())
            throw new InvalidReqMsgIdException("reqMsgId (Head @msgId) is required for ACK and must not be blank");
        String txnId = resolveTxnId(pathTxnId, xml, reqMsgId);
        System.out.println("[IMPS] ReqPay received txnId=" + txnId + " | transaction + message_audit_log will be updated");
        validateNewTxnId(txnId);
        reqPayValidationService.validate(xml);
        ackAndProcess(xml, txnId, reqMsgId, "ReqPay", "reqpay", ackSender, () -> reqPayService.processAsync(xml, txnId, reqMsgId));
        return ackService.buildAck("ReqPay", reqMsgId);
    }

    public String handleReqChkTxn(String xml, String pathTxnId, AckSender ackSender) throws CommonCodeValidationException {
        String reqMsgId = xmlParsingService.extractMsgId(xml);
        if (reqMsgId == null || reqMsgId.isBlank())
            throw new InvalidReqMsgIdException("reqMsgId (Head @msgId) is required for ACK and must not be blank");
        String txnId = resolveTxnId(pathTxnId, xml, reqMsgId);
        System.out.println("[IMPS] ReqChkTxn received txnId=" + txnId + " | transaction + message_audit_log will be updated");
        validateNewTxnId(txnId);
        commonCodeValidationService.validateCommonHeadTxn(xml);
        ackAndProcess(xml, txnId, reqMsgId, "ReqChkTxn", "reqchktxn", ackSender, () -> reqChkTxnService.processAsync(xml, txnId, reqMsgId));
        return ackService.buildAck("ReqChkTxn", reqMsgId);
    }

    public String handleReqHbt(String xml, String pathTxnId, AckSender ackSender) throws CommonCodeValidationException {
        String reqMsgId = xmlParsingService.extractMsgId(xml);
        if (reqMsgId == null || reqMsgId.isBlank())
            throw new InvalidReqMsgIdException("reqMsgId (Head @msgId) is required for ACK and must not be blank");
        String txnId = resolveTxnId(pathTxnId, xml, reqMsgId);
        System.out.println("[IMPS] ReqHbt received txnId=" + txnId + " | transaction + message_audit_log will be updated");
        validateNewTxnId(txnId);
        commonCodeValidationService.validateCommonHeadTxn(xml);
        ackAndProcess(xml, txnId, reqMsgId, "ReqHbt", "reqhbt", ackSender, () -> reqHbtService.processAsync(xml, txnId, reqMsgId));
        return ackService.buildAck("ReqHbt", reqMsgId);
    }

    public String handleReqListAccPvd(String xml, String pathTxnId, AckSender ackSender) throws CommonCodeValidationException {
        String reqMsgId = xmlParsingService.extractMsgId(xml);
        if (reqMsgId == null || reqMsgId.isBlank())
            throw new InvalidReqMsgIdException("reqMsgId (Head @msgId) is required for ACK and must not be blank");
        String txnId = resolveTxnId(pathTxnId, xml, reqMsgId);
        System.out.println("[IMPS] ReqListAccPvd received txnId=" + txnId + " | transaction + message_audit_log will be updated");
        validateNewTxnId(txnId);
        commonCodeValidationService.validateCommonHeadTxn(xml);
        ackAndProcess(xml, txnId, reqMsgId, "ReqListAccPvd", "reqlistaccpvd", ackSender, () -> reqListAccPvdService.processAsync(xml, txnId, reqMsgId));
        return ackService.buildAck("ReqListAccPvd", reqMsgId);
    }

    public String handleReqValAdd(String xml, String pathTxnId, AckSender ackSender) throws CommonCodeValidationException {
        String reqMsgId = xmlParsingService.extractMsgId(xml);
        if (reqMsgId == null || reqMsgId.isBlank())
            throw new InvalidReqMsgIdException("reqMsgId (Head @msgId) is required for ACK and must not be blank");
        String txnId = resolveTxnId(pathTxnId, xml, reqMsgId);
        System.out.println("[IMPS] ReqValAdd received txnId=" + txnId + " | transaction + message_audit_log will be updated");
        validateNewTxnId(txnId);
        commonCodeValidationService.validateCommonHeadTxn(xml);
        ackAndProcess(xml, txnId, reqMsgId, "ReqValAdd", "reqvaladd", ackSender, () -> reqValAddService.processAsync(xml, txnId, reqMsgId));
        return ackService.buildAck("ReqValAdd", reqMsgId);
    }

    // ---------- Resp* (NPCI → IMPS): single msg_id and txn_id resolve at entry ----------
    public String handleRespPay(String xml, String pathTxnId, AckSender ackSender) {
        String reqMsgId = xmlParsingService.extractMsgId(xml);
        String txnId = resolveTxnId(pathTxnId, xml, reqMsgId);
        String ack = ackService.buildAckWithFallback("RespPay", reqMsgId, txnId);
        ackSender.send(ack);
        respPayService.processAsync(xml, txnId, reqMsgId);
        return ack;
    }

    public String handleRespChkTxn(String xml, String pathTxnId, AckSender ackSender) {
        String reqMsgId = xmlParsingService.extractMsgId(xml);
        String txnId = resolveTxnId(pathTxnId, xml, reqMsgId);
        String ack = ackService.buildAckWithFallback("RespChkTxn", reqMsgId, txnId);
        ackSender.send(ack);
        respChkTxnService.processAsync(xml, txnId, reqMsgId);
        return ack;
    }

    public String handleRespListAccPvd(String xml, String pathTxnId, AckSender ackSender) {
        String reqMsgId = xmlParsingService.extractMsgId(xml);
        String txnId = resolveTxnId(pathTxnId, xml, reqMsgId);
        String ack = ackService.buildAckWithFallback("RespListAccPvd", reqMsgId, txnId);
        ackSender.send(ack);
        respListAccPvdService.processAsync(xml, txnId, reqMsgId);
        return ack;
    }

    public String handleRespValAdd(String xml, String pathTxnId, AckSender ackSender) {
        String reqMsgId = xmlParsingService.extractMsgId(xml);
        String txnId = resolveTxnId(pathTxnId, xml, reqMsgId);
        String ack = ackService.buildAckWithFallback("RespValAdd", reqMsgId, txnId);
        ackSender.send(ack);
        respValAddService.processAsync(xml, txnId, reqMsgId);
        return ack;
    }
}
