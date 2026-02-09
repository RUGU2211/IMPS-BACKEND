package com.hitachi.imps.service.chktxn;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.ISwitchClient;
import com.hitachi.imps.client.NpciMockClient;
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

/** ReqChkTxn: NPCI (XML) and Switch (ISO). txn_id from path/XML only. Institution (IFSC) validation in IMPS only. */
@Service
public class ReqChkTxnService {

    private static final String UNKNOWN_TXN = "UNKNOWN";

    @Autowired private XmlToIsoConverter xmlToIsoConverter;
    @Autowired private IsoToXmlConverter isoToXmlConverter;
    @Autowired private ISwitchClient switchClient;
    @Autowired private NpciMockClient npciMockClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private TransactionService transactionService;
    @Autowired private PendingSocketResponseStore pendingSocketStore;
    @Autowired(required = false) private INpciResponseSender npciResponseSender;
    @Autowired private AckService ackService;
    @Autowired private InstitutionValidationService institutionValidationService;
    @Autowired private SwitchAddressResolver switchAddressResolver;

    @Async
    public void processAsync(String xml, String pathTxnId) {
        try { processFromNpci(xml, pathTxnId, null); } catch (Exception e) { System.err.println("ReqChkTxnService (NPCI) ERROR: " + e.getMessage()); }
    }

    @Async
    public void processAsync(String xml, String pathTxnId, String reqMsgId) {
        try { processFromNpci(xml, pathTxnId, reqMsgId); } catch (Exception e) { System.err.println("ReqChkTxnService (NPCI) ERROR: " + e.getMessage()); }
    }

    public void processFromNpci(String xml, String pathTxnId) {
        processFromNpci(xml, pathTxnId, null);
    }

    public void processFromNpci(String xml, String pathTxnId, String knownReqMsgId) {
        String msgId = (knownReqMsgId != null && !knownReqMsgId.isBlank()) ? knownReqMsgId : xmlParsingService.extractMsgId(xml);
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : xmlParsingService.extractTxnId(xml);
        if (txnId == null || txnId.isBlank()) txnId = msgId;
        auditService.saveRaw(txnId, "NPCI_REQCHKTXN_XML_IN", xml);

        // Institution (IFSC) validation – IMPS only
        String payeeIfsc = XmlUtil.parseReqChkTxn(xml).get("payee_ifsc");
        String instErr = institutionValidationService.validatePayeeIfsc(payeeIfsc);
        if (instErr != null) {
            TransactionEntity txn = transactionService.createRequest(txnId, xml, "CHKTXN");
            String errResp = ackService.buildFailureRespChkTxn(msgId, "MJ", instErr);
            transactionService.markFailure(txn, errResp);
            auditService.saveRaw(txnId, "NPCI_RESPCHKTXN_XML_OUT", errResp);
            if (sendToNpci(txnId, errResp)) return;
            try { npciMockClient.sendRespChkTxn(errResp, txnId); } catch (Exception e) { System.out.println("NPCI Mock not available: " + e.getMessage()); }
            return;
        }

        // Bank/switch down check – validate bank active before processing; do not forward to switch
        String requestOrgId = xmlParsingService.extractOrgId(xml);
        if (switchAddressResolver.isBankDown(requestOrgId)) {
            var downOpt = switchAddressResolver.findDownInstitution(requestOrgId);
            downOpt.ifPresent(SwitchAddressResolver::logFailedSwitchToConsole);
            String errMsg = downOpt.map(SwitchAddressResolver::buildBankDownErrMsg).orElse("Bank switch unreachable. Transaction failed.");
            TransactionEntity txn = transactionService.createRequest(txnId, xml, "CHKTXN");
            String errResp = ackService.buildFailureRespChkTxn(msgId, "BANK_DOWN", errMsg);
            transactionService.markFailure(txn, errResp);
            auditService.saveRaw(txnId, "NPCI_RESPCHKTXN_XML_OUT", errResp);
            if (sendToNpci(txnId, errResp)) return;
            try { npciMockClient.sendRespChkTxn(errResp, txnId); } catch (Exception e) { System.out.println("NPCI Mock not available: " + e.getMessage()); }
            return;
        }

        TransactionEntity txn = transactionService.createRequest(txnId, xml, "CHKTXN");
        ISOMsg iso = xmlToIsoConverter.convertReqChkTxn(xml);
        auditService.saveParsed(txnId, "SWITCH_REQCHKTXN_ISO_OUT", iso);
        transactionService.markIsoSent(txn);
        byte[] response = switchClient.sendReqChkTxn(iso, txnId, requestOrgId);
        if (response != null) {
            auditService.saveRawBytesWithParsed(txnId, "SWITCH_REQCHKTXN_ISO_IN", response);
            String respXml = isoToXmlConverter.convertRespChkTxnToXml(response);
            auditService.saveRaw(txnId, "NPCI_RESPCHKTXN_XML_OUT", respXml);
            String approvalNum = extractApprovalNum(response);
            transactionService.markSuccess(txn, respXml, approvalNum, null);
            if (sendToNpci(txnId, respXml)) return;
            try { npciMockClient.sendRespChkTxn(respXml, txnId); } catch (Exception e) { System.out.println("NPCI Mock not available: " + e.getMessage()); }
        } else {
            transactionService.markFailure(txn, null);
            String errAck = ackService.buildAckWithFallback("RespChkTxn", msgId, txnId);
            sendToNpci(txnId, errAck);
        }
    }

    private static String extractApprovalNum(byte[] isoBytes) {
        try {
            ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
            return iso.hasField(38) ? iso.getString(38) : null;
        } catch (Exception e) { return null; }
    }

    private boolean sendToNpci(String txnId, String respXml) {
        if (npciResponseSender != null) return npciResponseSender.send(txnId, respXml);
        return pendingSocketStore.completePending(txnId, respXml);
    }

    @Async
    public void processAsync(byte[] isoBytes, String pathTxnId) {
        try { processFromSwitch(isoBytes, pathTxnId); } catch (Exception e) { System.err.println("ReqChkTxnService (Switch) ERROR: " + e.getMessage()); }
    }

    public void processFromSwitch(byte[] isoBytes, String pathTxnId) {
        processFromSwitchSync(isoBytes, pathTxnId);
    }

    /** Reverse flow: Switch → IMPS → NPCI → IMPS → Switch. Returns Resp ISO. */
    public byte[] processFromSwitchSync(byte[] isoBytes, String pathTxnId) {
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : UNKNOWN_TXN;
        auditService.saveRawBytesWithParsed(txnId, "SWITCH_REQCHKTXN_ISO_IN", isoBytes);
        try {
            org.jpos.iso.ISOMsg iso = new org.jpos.iso.ISOMsg();
            iso.setPackager(new com.hitachi.imps.iso.ImpsIsoPackager());
            iso.unpack(isoBytes);
            String payeeIfsc = iso.hasField(33) ? iso.getString(33) : null;
            String instErr = institutionValidationService.validatePayeeIfsc(payeeIfsc);
            if (instErr != null) {
                System.out.println("IMPS: ReqChkTxn from Switch – institution invalid: " + instErr);
                return null;
            }
        } catch (Exception e) {
            System.err.println("IMPS: ReqChkTxn from Switch – could not validate IFSC: " + e.getMessage());
            return null;
        }
        String reqXml = isoToXmlConverter.convertReqChkTxnToXml(isoBytes);
        auditService.saveRaw(txnId, "NPCI_REQCHKTXN_XML_OUT", reqXml);
        String respXml;
        try {
            respXml = (txnId != null && !txnId.isBlank()) ? npciMockClient.sendReqChkTxn(reqXml, txnId) : npciMockClient.sendReqChkTxn(reqXml);
        } catch (Exception e) {
            System.err.println("NPCI Mock not available: " + e.getMessage());
            return null;
        }
        if (respXml == null || respXml.isBlank()) return null;
        auditService.saveRaw(txnId, "NPCI_RESPCHKTXN_XML_IN", respXml);
        try {
            ISOMsg respIso = xmlToIsoConverter.convertRespChkTxn(respXml);
            byte[] respBytes = IsoUtil.pack(respIso);
            auditService.saveRawBytesWithParsed(txnId, "SWITCH_RESPCHKTXN_ISO_OUT", respBytes);
            return respBytes;
        } catch (Exception e) {
            System.err.println("IMPS: RespChkTxn XML to ISO failed: " + e.getMessage());
            return null;
        }
    }
}
