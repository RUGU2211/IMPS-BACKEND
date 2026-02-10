package com.hitachi.imps.service.valadd;

import java.util.Map;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hitachi.imps.client.npci.NpciMockClient;
import com.hitachi.imps.client.switchclient.ISwitchClient;
import com.hitachi.imps.converter.IsoToXmlConverter;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.util.IsoUtil;
import com.hitachi.imps.converter.XmlToIsoConverter;
import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.npci.INpciResponseSender;
import com.hitachi.imps.service.socket.PendingSocketResponseStore;
import com.hitachi.imps.service.XmlParsingService;
import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.routing.SwitchAddressResolver;
import com.hitachi.imps.service.validation.InstitutionValidationService;
import com.hitachi.imps.service.validation.CommonCodeValidationService;
import com.hitachi.imps.exception.CommonCodeValidationException;

/** ReqValAdd: NPCI (XML) and Switch (ISO). account_master validation done in Switch only – IMPS forwards to Switch. */
@Service
public class ReqValAddService {

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
    @Autowired private CommonCodeValidationService commonCodeValidationService;
    @Autowired private SwitchAddressResolver switchAddressResolver;

    @Async
    public void processAsync(String xml, String pathTxnId) {
        try { processFromNpci(xml, pathTxnId, null); } catch (Exception e) { System.err.println("ReqValAddService (NPCI) ERROR: " + e.getMessage()); }
    }

    @Async
    public void processAsync(String xml, String pathTxnId, String reqMsgId) {
        try { processFromNpci(xml, pathTxnId, reqMsgId); } catch (Exception e) { System.err.println("ReqValAddService (NPCI) ERROR: " + e.getMessage()); }
    }

    @Transactional
    public void processFromNpci(String xml, String pathTxnId) {
        processFromNpci(xml, pathTxnId, null);
    }

    @Transactional
    public void processFromNpci(String xml, String pathTxnId, String knownReqMsgId) {
        String msgId = (knownReqMsgId != null && !knownReqMsgId.isBlank()) ? knownReqMsgId : xmlParsingService.extractMsgId(xml);
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : xmlParsingService.extractTxnId(xml);
        if (txnId == null || txnId.isBlank()) txnId = msgId;
        auditService.saveRaw(txnId, "NPCI_REQVALADD_XML_IN", xml);
        Map<String, String> parsed = xmlParsingService.parseReqValAdd(xml);
        String acNum = parsed.get("ACNUM");
        String ifsc = parsed.get("IFSC");
        String respXml;

        // Institution (IFSC) validation – IMPS only
        String instErr = institutionValidationService.validatePayeeIfsc(ifsc);
        if (instErr != null) {
            TransactionEntity txn = transactionService.createRequest(txnId, xml, "VALADD");
            respXml = buildFailureResponse(msgId, "MJ", instErr);
            transactionService.markFailure(txn, respXml);
            auditService.saveRaw(txnId, "NPCI_RESPVALADD_XML_OUT", respXml);
            if (sendToNpci(txnId, respXml)) return;
            try { npciMockClient.sendRespValAdd(respXml); } catch (Exception e) { System.out.println("NPCI Mock not available: " + e.getMessage()); }
            return;
        }

        // Bank/switch down check – validate bank active before processing; do not forward to switch
        String requestOrgId = xmlParsingService.extractOrgId(xml);
        if (switchAddressResolver.isBankDown(requestOrgId)) {
            var downOpt = switchAddressResolver.findDownInstitution(requestOrgId);
            downOpt.ifPresent(SwitchAddressResolver::logFailedSwitchToConsole);
            String errMsg = downOpt.map(SwitchAddressResolver::buildBankDownErrMsg).orElse("Bank switch unreachable. Transaction failed.");
            TransactionEntity txn = transactionService.createRequest(txnId, xml, "VALADD");
            respXml = buildFailureResponse(msgId, "BANK_DOWN", errMsg);
            transactionService.markFailure(txn, respXml);
            auditService.saveRaw(txnId, "NPCI_RESPVALADD_XML_OUT", respXml);
            if (sendToNpci(txnId, respXml)) return;
            try { npciMockClient.sendRespValAdd(respXml); } catch (Exception e) { System.out.println("NPCI Mock not available: " + e.getMessage()); }
            return;
        }

        if (acNum != null && ifsc != null) {
            TransactionEntity txn = transactionService.createRequest(txnId, xml, "VALADD");
            ISOMsg iso = xmlToIsoConverter.convertReqValAdd(xml);
            auditService.saveParsed(txnId, "SWITCH_REQVALADD_ISO_OUT", iso);
            transactionService.markIsoSent(txn);
            byte[] response = switchClient.sendReqValAdd(iso, txnId, requestOrgId);
            if (response != null) {
                auditService.saveRawBytesWithParsed(txnId, "SWITCH_REQVALADD_ISO_IN", response);
                respXml = isoToXmlConverter.convertRespValAddToXml(response);
                auditService.saveRaw(txnId, "NPCI_RESPVALADD_XML_OUT", respXml);
                String approvalNum = extractApprovalNum(response);
                transactionService.markSuccess(txn, respXml, approvalNum, null);
                if (sendToNpci(txnId, respXml)) return;
                try { npciMockClient.sendRespValAdd(respXml, txnId); } catch (Exception e) { System.out.println("NPCI Mock not available: " + e.getMessage()); }
            } else {
                respXml = buildFailureResponse(msgId, "96", "Switch response timeout");
                transactionService.markFailure(txn, respXml);
                auditService.saveRaw(txnId, "NPCI_RESPVALADD_XML_OUT", respXml);
                if (sendToNpci(txnId, respXml)) return;
                try { npciMockClient.sendRespValAdd(respXml, txnId); } catch (Exception e) { System.out.println("NPCI Mock not available: " + e.getMessage()); }
            }
            return;
        }
        TransactionEntity txn = transactionService.createRequest(txnId, xml, "VALADD");
        respXml = buildFailureResponse(msgId, "14", "Invalid Account Details");
        transactionService.markFailure(txn, respXml);
                auditService.saveRaw(txnId, "NPCI_RESPVALADD_XML_OUT", respXml);
        if (sendToNpci(txnId, respXml)) return;
        try { npciMockClient.sendRespValAdd(respXml); } catch (Exception e) { System.out.println("NPCI Mock not available: " + e.getMessage()); }
    }

    private boolean sendToNpci(String txnId, String respXml) {
        if (npciResponseSender != null) return npciResponseSender.send(txnId, respXml);
        return pendingSocketStore.completePending(txnId, respXml);
    }

    private static String extractApprovalNum(byte[] isoBytes) {
        try {
            org.jpos.iso.ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
            return iso.hasField(38) ? iso.getString(38) : null;
        } catch (Exception e) { return null; }
    }

    private String buildFailureResponse(String reqMsgId, String respCode, String reason) {
        return ackService.buildFailureRespValAdd(reqMsgId, respCode, reason);
    }

    @Async
    public void processAsync(byte[] isoBytes, String pathTxnId) {
        try { processFromSwitch(isoBytes, pathTxnId); } catch (Exception e) { System.err.println("ReqValAddService (Switch) ERROR: " + e.getMessage()); }
    }

    public void processFromSwitch(byte[] isoBytes, String pathTxnId) {
        processFromSwitchSync(isoBytes, pathTxnId);
    }

    /** Reverse flow: Switch → IMPS → NPCI → IMPS → Switch. Returns Resp ISO. Duplicate txn_id rejected (409). */
    public byte[] processFromSwitchSync(byte[] isoBytes, String pathTxnId) {
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : UNKNOWN_TXN;
        if (!UNKNOWN_TXN.equals(txnId))
            transactionService.validateNewTxnId(txnId);
        auditService.saveRawBytesWithParsed(txnId, "SWITCH_REQVALADD_ISO_IN", isoBytes);
        try {
            org.jpos.iso.ISOMsg iso = new org.jpos.iso.ISOMsg();
            iso.setPackager(new com.hitachi.imps.iso.ImpsIsoPackager());
            iso.unpack(isoBytes);
            String payeeIfsc = iso.hasField(33) ? iso.getString(33) : null;
            String instErr = institutionValidationService.validatePayeeIfsc(payeeIfsc);
            if (instErr != null) {
                System.out.println("IMPS: ReqValAdd from Switch – institution invalid: " + instErr);
                return null;
            }
        } catch (Exception e) {
            System.err.println("IMPS: ReqValAdd from Switch – could not validate IFSC: " + e.getMessage());
            return null;
        }
        String reqXml = isoToXmlConverter.convertReqValAddToXml(isoBytes);
        auditService.saveRaw(txnId, "NPCI_REQVALADD_XML_OUT", reqXml);
        try {
            commonCodeValidationService.validateCommonHeadTxn(reqXml);
        } catch (CommonCodeValidationException e) {
            System.out.println("[IMPS] ReqValAdd from Switch – validation failed: " + e.getMessage());
            String errMsg = e.getRuleIds().isEmpty() ? e.getMessage() : (e.getRuleIds().get(0) + ": " + (e.getMessages().isEmpty() ? e.getMessage() : e.getMessages().get(0)));
            com.hitachi.imps.entity.TransactionEntity txnFail = transactionService.createRequest(txnId, reqXml, "VALADD");
            String errResp = ackService.buildFailureRespValAdd(xmlParsingService.extractMsgId(reqXml), "96", errMsg);
            transactionService.markFailure(txnFail, errResp);
            return ackService.buildFailureRespIso(isoBytes, errMsg);
        }
        TransactionEntity txn = transactionService.createRequest(txnId, reqXml, "VALADD");
        transactionService.markIsoSent(txn);
        String respXml;
        try {
            respXml = (txnId != null && !txnId.isBlank()) ? npciMockClient.sendReqValAdd(reqXml, txnId) : npciMockClient.sendReqValAdd(reqXml);
        } catch (Exception e) {
            System.err.println("NPCI Mock not available: " + e.getMessage());
            transactionService.markFailure(txn, null);
            return null;
        }
        if (respXml == null || respXml.isBlank()) {
            transactionService.markFailure(txn, null);
            return null;
        }
        auditService.saveRaw(txnId, "NPCI_RESPVALADD_XML_IN", respXml);
        try {
            org.jpos.iso.ISOMsg respIso = xmlToIsoConverter.convertRespValAdd(respXml);
            byte[] respBytes = IsoUtil.pack(respIso);
            auditService.saveRawBytesWithParsed(txnId, "SWITCH_RESPVALADD_ISO_OUT", respBytes);
            String approvalNum = extractApprovalNum(respBytes);
            transactionService.markSuccess(txn, respXml, approvalNum, null);
            return respBytes;
        } catch (Exception e) {
            System.err.println("IMPS: RespValAdd XML to ISO failed: " + e.getMessage());
            transactionService.markFailure(txn, null);
            return null;
        }
    }
}
