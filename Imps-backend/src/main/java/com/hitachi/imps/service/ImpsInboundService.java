package com.hitachi.imps.service;

import com.hitachi.imps.exception.CommonCodeValidationException;
import com.hitachi.imps.exception.InvalidReqMsgIdException;
import com.hitachi.imps.exception.ReqPayValidationException;
import com.hitachi.imps.service.ack.AckSender;
import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.chktxn.ReqChkTxnService;
import com.hitachi.imps.service.heartbeat.ReqHbtService;
import com.hitachi.imps.service.listaccpvd.ReqListAccPvdService;
import com.hitachi.imps.service.pay.ReqPayService;
import com.hitachi.imps.service.pay.ReqPayValidationService;
import com.hitachi.imps.service.valadd.ReqValAddService;
import com.hitachi.imps.service.validation.CommonCodeValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Inbound request handling. ACK is sent via the provided AckSender (REST to NPCI Mock).
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
    @Autowired private ReqValAddService reqValAddService;

    public void validateNewTxnId(String txnId) {
        if (txnId == null || txnId.isBlank())
            throw new IllegalArgumentException("txnId must not be blank");
        transactionService.validateNewTxnId(txnId);
    }

    private void ackAndProcess(String xml, String txnId, String apiName, String reqType, AckSender ackSender, Runnable process) {
        String reqMsgId = xmlParsingService.extractMsgId(xml);
        if (reqMsgId == null || reqMsgId.isBlank())
            throw new InvalidReqMsgIdException("reqMsgId (Head @msgId) is required for ACK and must not be blank");
        System.out.println("[IMPS] ACK sent to NPCI " + reqType + "/" + txnId);
        String ack = ackService.buildAck(apiName, reqMsgId);
        ackSender.send(ack);
        process.run();
    }

    public String handleReqPay(String xml, String txnId, AckSender ackSender) throws ReqPayValidationException {
        System.out.println("[IMPS] ReqPay received txnId=" + txnId + " | transaction + message_audit_log will be updated");
        validateNewTxnId(txnId);
        reqPayValidationService.validate(xml);
        ackAndProcess(xml, txnId, "ReqPay", "reqpay", ackSender, () -> reqPayService.processAsync(xml, txnId));
        return ackService.buildAck("ReqPay", xmlParsingService.extractMsgId(xml));
    }

    public String handleReqChkTxn(String xml, String txnId, AckSender ackSender) throws CommonCodeValidationException {
        System.out.println("[IMPS] ReqChkTxn received txnId=" + txnId + " | transaction + message_audit_log will be updated");
        validateNewTxnId(txnId);
        commonCodeValidationService.validateCommonHeadTxn(xml);
        ackAndProcess(xml, txnId, "ReqChkTxn", "reqchktxn", ackSender, () -> reqChkTxnService.processAsync(xml, txnId));
        return ackService.buildAck("ReqChkTxn", xmlParsingService.extractMsgId(xml));
    }

    public String handleReqHbt(String xml, String txnId, AckSender ackSender) throws CommonCodeValidationException {
        System.out.println("[IMPS] ReqHbt received txnId=" + txnId + " | transaction + message_audit_log will be updated");
        validateNewTxnId(txnId);
        commonCodeValidationService.validateCommonHeadTxn(xml);
        ackAndProcess(xml, txnId, "ReqHbt", "reqhbt", ackSender, () -> reqHbtService.processAsync(xml, txnId));
        return ackService.buildAck("ReqHbt", xmlParsingService.extractMsgId(xml));
    }

    public String handleReqListAccPvd(String xml, String txnId, AckSender ackSender) throws CommonCodeValidationException {
        System.out.println("[IMPS] ReqListAccPvd received txnId=" + txnId + " | transaction + message_audit_log will be updated");
        validateNewTxnId(txnId);
        commonCodeValidationService.validateCommonHeadTxn(xml);
        ackAndProcess(xml, txnId, "ReqListAccPvd", "reqlistaccpvd", ackSender, () -> reqListAccPvdService.processAsync(xml, txnId));
        return ackService.buildAck("ReqListAccPvd", xmlParsingService.extractMsgId(xml));
    }

    public String handleReqValAdd(String xml, String txnId, AckSender ackSender) throws CommonCodeValidationException {
        System.out.println("[IMPS] ReqValAdd received txnId=" + txnId + " | transaction + message_audit_log will be updated");
        validateNewTxnId(txnId);
        commonCodeValidationService.validateCommonHeadTxn(xml);
        ackAndProcess(xml, txnId, "ReqValAdd", "reqvaladd", ackSender, () -> reqValAddService.processAsync(xml, txnId));
        return ackService.buildAck("ReqValAdd", xmlParsingService.extractMsgId(xml));
    }
}
