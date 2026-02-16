package com.hitachi.imps.service.pay;

import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.switchclient.ISwitchClient;
import com.hitachi.imps.client.npci.NpciRestClient;
import com.hitachi.imps.converter.IsoToXmlConverter;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.util.IsoUtil;
import com.hitachi.imps.converter.XmlToIsoConverter;
import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.npci.INpciResponseSender;
import com.hitachi.imps.service.socket.PendingSocketResponseStore;
import com.hitachi.imps.service.XmlParsingService;
import com.hitachi.imps.service.iso.XmlUtil;
import com.hitachi.imps.service.routing.SwitchAddressResolver;
import com.hitachi.imps.service.validation.InstitutionValidationService;
import com.hitachi.imps.exception.ReqPayValidationException;
import com.hitachi.imps.config.ImpsServerDisplayInfo;

/**
 * ReqPay: single service for both NPCI→IMPS (XML) and Switch→IMPS (ISO).
 * txn_id and msg_id come from NPCI/path only; no random generation.
 * Institution (IFSC) validation in IMPS only – mock_switch has no access to institution_master.
 */
@Service
public class ReqPayService {

    private static final Logger log = LoggerFactory.getLogger(ReqPayService.class);
    private static final String UNKNOWN_TXN = "UNKNOWN";

    @Autowired private XmlToIsoConverter xmlToIsoConverter;
    @Autowired private IsoToXmlConverter isoToXmlConverter;
    @Autowired private ISwitchClient switchClient;
    @Autowired private NpciRestClient npciRestClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private TransactionService transactionService;
    @Autowired private PendingSocketResponseStore pendingSocketStore;
    @Autowired(required = false) private INpciResponseSender npciResponseSender;
    @Autowired private AckService ackService;
    @Autowired private InstitutionValidationService institutionValidationService;
    @Autowired private SwitchAddressResolver switchAddressResolver;
    @Autowired private ReqPayValidationService reqPayValidationService;
    @Autowired private ImpsServerDisplayInfo impsServerDisplay;

    // ----- NPCI → IMPS (XML): receive XML, convert to ISO, send to Switch -----
    @Async
    public void processAsync(String xml, String pathTxnId) {
        try {
            processFromNpci(xml, pathTxnId, null);
        } catch (Exception e) {
            log.error("ReqPayService (NPCI) ERROR", e);
        }
    }

    @Async
    public void processAsync(String xml, String pathTxnId, String reqMsgId) {
        try {
            processFromNpci(xml, pathTxnId, reqMsgId);
        } catch (Exception e) {
            log.error("ReqPayService (NPCI) ERROR", e);
        }
    }

    public void processFromNpci(String xml, String pathTxnId) {
        processFromNpci(xml, pathTxnId, null);
    }

    public void processFromNpci(String xml, String pathTxnId, String knownReqMsgId) {
        String msgId = (knownReqMsgId != null && !knownReqMsgId.isBlank()) ? knownReqMsgId : xmlParsingService.extractMsgId(xml);
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : xmlParsingService.extractTxnId(xml);
        if (txnId == null || txnId.isBlank()) txnId = msgId;

        auditService.saveRaw(txnId, "NPCI_REQPAY_XML_IN", xml);

        // Institution (IFSC) validation – IMPS only
        String payeeIfsc = XmlUtil.parseReqPay(xml).get("payee_ifsc");
        String instErr = institutionValidationService.validatePayeeIfsc(payeeIfsc);
        if (instErr != null) {
            TransactionEntity txn = transactionService.createRequest(txnId, xml);
            String errResp = ackService.buildFailureRespPay(msgId, "MJ", instErr);
            transactionService.markFailure(txn, errResp);
            auditService.saveRaw(txnId, "NPCI_RESPPAY_XML_OUT", errResp);
            if (sendToNpci(txnId, errResp)) return;
            try { npciRestClient.sendRespPay(errResp, txnId); } catch (Exception e) { log.warn("NPCI not available: {}", e.getMessage()); }
            return;
        }

        // Bank/switch down check – validate bank active before processing; do not forward to switch
        String requestOrgId = xmlParsingService.extractOrgId(xml);
        if (switchAddressResolver.isBankDown(requestOrgId)) {
            var downOpt = switchAddressResolver.findDownInstitution(requestOrgId);
            downOpt.ifPresent(switchAddressResolver::logFailedSwitchToConsole);
            String errMsg = downOpt.map(switchAddressResolver::buildBankDownErrMsg).orElse("Bank switch unreachable. Transaction failed.");
            TransactionEntity txn = transactionService.createRequest(txnId, xml);
            String errResp = ackService.buildFailureRespPay(msgId, "BANK_DOWN", errMsg);
            transactionService.markFailure(txn, errResp);
            auditService.saveRaw(txnId, "NPCI_RESPPAY_XML_OUT", errResp);
            if (sendToNpci(txnId, errResp)) return;
            try { npciRestClient.sendRespPay(errResp, txnId); } catch (Exception e) { log.warn("NPCI not available: {}", e.getMessage()); }
            return;
        }

        TransactionEntity txn = transactionService.createRequest(txnId, xml);

        ISOMsg iso = xmlToIsoConverter.convertReqPay(xml, txnId);
        try {
            if (iso.hasField(11)) txn.setDe11(iso.getString(11));
            if (iso.hasField(37)) txn.setDe37(iso.getString(37));
            if (iso.hasField(12)) txn.setDe12(iso.getString(12));
            if (iso.hasField(13)) txn.setDe13(iso.getString(13));
        } catch (Exception e) {
            log.warn("Error setting DE fields: {}", e.getMessage());
        }
        auditService.saveParsed(txnId, "SWITCH_REQPAY_ISO_OUT", iso);

        transactionService.markIsoSent(txn);
        byte[] response = switchClient.sendReqPay(iso, txnId, requestOrgId);
        if (response != null) {
            auditService.saveRawBytesWithParsed(txnId, "SWITCH_REQPAY_ISO_IN", response);
            String respXml = isoToXmlConverter.convertRespPayToXml(response);
            auditService.saveRaw(txnId, "NPCI_RESPPAY_XML_OUT", respXml);
            String approvalNum = extractApprovalNum(response);
            transactionService.markSuccess(txn, respXml, approvalNum, null);
            if (sendToNpci(txnId, respXml)) return;
            try { npciRestClient.sendRespPay(respXml, txnId); } catch (Exception e) { log.warn("NPCI not available: {}", e.getMessage()); }
        } else {
            transactionService.markFailure(txn, null);
            String errAck = ackService.buildAckWithFallback("RespPay", msgId, txnId);
            if (sendToNpci(txnId, errAck)) return;
        }
    }

    private boolean sendToNpci(String txnId, String respXml) {
        if (npciResponseSender != null) return npciResponseSender.send(txnId, respXml);
        return pendingSocketStore.completePending(txnId, respXml);
    }

    private static String extractApprovalNum(byte[] isoBytes) {
        try {
            ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
            return iso.hasField(38) ? iso.getString(38) : null;
        } catch (Exception e) { return null; }
    }

    // ----- Switch → IMPS (ISO): reverse flow – receive ISO, convert to XML, send to NPCI, get Resp XML, convert to ISO, return -----
    @Async
    public void processAsync(byte[] isoBytes, String pathTxnId) {
        try {
            processFromSwitch(isoBytes, pathTxnId);
        } catch (Exception e) {
            log.error("ReqPayService (Switch) ERROR", e);
        }
    }

    public void processFromSwitch(byte[] isoBytes, String pathTxnId) {
        processFromSwitchSync(isoBytes, pathTxnId);
    }

    /**
     * Reverse flow: Switch sends Req ISO → IMPS converts to XML → sends to NPCI → gets Resp XML → converts to ISO.
     * Validates duplicate txn_id, IFSC; logs to transaction + message_audit_log; returns Resp ISO to Switch.
     */
    public byte[] processFromSwitchSync(byte[] isoBytes, String pathTxnId) {
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : UNKNOWN_TXN;
        if (!UNKNOWN_TXN.equals(txnId))
            transactionService.validateNewTxnId(txnId);

        auditService.saveRawBytesWithParsed(txnId, "SWITCH_REQPAY_ISO_IN", isoBytes);
        ISOMsg iso;
        try {
            iso = new ISOMsg();
            iso.setPackager(new com.hitachi.imps.iso.ImpsIsoPackager());
            iso.unpack(isoBytes);
            String payeeIfsc = iso.hasField(33) ? iso.getString(33) : null;
            String instErr = institutionValidationService.validatePayeeIfsc(payeeIfsc);
            if (instErr != null) {
                log.warn("[IMPS] ReqPay from Switch – institution invalid: {}", instErr);
                String reqXmlFail = isoToXmlConverter.convertReqPayToXml(isoBytes);
                com.hitachi.imps.entity.TransactionEntity txnFail = transactionService.createRequest(txnId, reqXmlFail);
                String errResp = ackService.buildFailureRespPay(null, "MJ", instErr);
                transactionService.markFailure(txnFail, errResp);
                return ackService.buildFailureRespIso(isoBytes, instErr);
            }
        } catch (Exception e) {
            log.warn("IMPS: ReqPay from Switch – could not validate IFSC: {}", e.getMessage());
            return null;
        }

        String reqXml = isoToXmlConverter.convertReqPayToXml(isoBytes);
        auditService.saveRaw(txnId, "NPCI_REQPAY_XML_OUT", reqXml);

        try {
            reqPayValidationService.validate(reqXml);
        } catch (ReqPayValidationException e) {
            log.warn("[IMPS] ReqPay from Switch – validation failed: {}", e.getMessage());
            String errMsg = e.getRuleIds().isEmpty() ? e.getMessage() : (e.getRuleIds().get(0) + ": " + (e.getMessages().isEmpty() ? e.getMessage() : e.getMessages().get(0)));
            com.hitachi.imps.entity.TransactionEntity txnFail = transactionService.createRequest(txnId, reqXml);
            String errResp = ackService.buildFailureRespPay(xmlParsingService.extractMsgId(reqXml), "96", errMsg);
            transactionService.markFailure(txnFail, errResp);
            return ackService.buildFailureRespIso(isoBytes, errMsg);
        }

        com.hitachi.imps.entity.TransactionEntity txn = transactionService.createRequest(txnId, reqXml);
        if (iso.hasField(11)) txn.setDe11(iso.getString(11));
        if (iso.hasField(37)) txn.setDe37(iso.getString(37));
        if (iso.hasField(12)) txn.setDe12(iso.getString(12));
        if (iso.hasField(13)) txn.setDe13(iso.getString(13));
        transactionService.markIsoSent(txn);

        log.info("[IMPS] IMPS → NPCI | REQ sent | ReqPay | TxnId: {}", txnId);
        String respXml;
        long tNpci = System.currentTimeMillis();
        try {
            respXml = (txnId != null && !txnId.isBlank()) ? npciRestClient.sendReqPay(reqXml, txnId) : npciRestClient.sendReqPay(reqXml);
        } catch (Exception e) {
            log.warn("NPCI not available: {}", e.getMessage());
            transactionService.markFailure(txn, null);
            return null;
        }
        if (respXml == null || respXml.isBlank()) {
            transactionService.markFailure(txn, null);
            return null;
        }
        long npciRoundtripMs = System.currentTimeMillis() - tNpci;
        log.info("[IMPS] NPCI → IMPS | RESP received | RespPay | TxnId: {} | Roundtrip: {}ms", txnId, npciRoundtripMs);
        auditService.saveRaw(txnId, "NPCI_RESPPAY_XML_IN", respXml);
        try {
            ISOMsg respIso = xmlToIsoConverter.convertRespPay(respXml);
            String approvalNum = respIso.hasField(38) ? respIso.getString(38) : null;
            transactionService.markSuccess(txn, respXml, approvalNum, null);
            byte[] respBytes = IsoUtil.pack(respIso);
            auditService.saveRawBytesWithParsed(txnId, "SWITCH_RESPPAY_ISO_OUT", respBytes);
            log.debug("[IMPS] ReqPay complete: transaction + message_audit_log updated");
            return respBytes;
        } catch (Exception e) {
            log.warn("IMPS: RespPay XML to ISO failed: {}", e.getMessage());
            transactionService.markFailure(txn, respXml);
            return null;
        }
    }

}
