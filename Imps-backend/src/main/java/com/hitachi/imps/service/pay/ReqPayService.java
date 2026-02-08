package com.hitachi.imps.service.pay;

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

/**
 * ReqPay: single service for both NPCI→IMPS (XML) and Switch→IMPS (ISO).
 * txn_id and msg_id come from NPCI/path only; no random generation.
 * Institution (IFSC) validation in IMPS only – mock_switch has no access to institution_master.
 */
@Service
public class ReqPayService {

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

    // ----- NPCI → IMPS (XML): receive XML, convert to ISO, send to Switch -----
    @Async
    public void processAsync(String xml, String pathTxnId) {
        try {
            processFromNpci(xml, pathTxnId);
        } catch (Exception e) {
            System.err.println("ReqPayService (NPCI) ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void processFromNpci(String xml, String pathTxnId) {
        String msgId = xmlParsingService.extractMsgId(xml);
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
            try { npciMockClient.sendRespPay(errResp, txnId); } catch (Exception e) { System.out.println("NPCI Mock not available: " + e.getMessage()); }
            return;
        }

        // Bank/switch down check – validate bank active before processing; do not forward to switch
        String requestOrgId = xmlParsingService.extractOrgId(xml);
        if (switchAddressResolver.isBankDown(requestOrgId)) {
            var downOpt = switchAddressResolver.findDownInstitution(requestOrgId);
            downOpt.ifPresent(SwitchAddressResolver::logFailedSwitchToConsole);
            String errMsg = downOpt.map(SwitchAddressResolver::buildBankDownErrMsg).orElse("Bank switch unreachable. Transaction failed.");
            TransactionEntity txn = transactionService.createRequest(txnId, xml);
            String errResp = ackService.buildFailureRespPay(msgId, "BANK_DOWN", errMsg);
            transactionService.markFailure(txn, errResp);
            auditService.saveRaw(txnId, "NPCI_RESPPAY_XML_OUT", errResp);
            if (sendToNpci(txnId, errResp)) return;
            try { npciMockClient.sendRespPay(errResp, txnId); } catch (Exception e) { System.out.println("NPCI Mock not available: " + e.getMessage()); }
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
            System.err.println("Error setting DE fields: " + e.getMessage());
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
            try { npciMockClient.sendRespPay(respXml, txnId); } catch (Exception e) { System.out.println("NPCI Mock not available: " + e.getMessage()); }
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

    // ----- Switch → IMPS (ISO): receive ISO, convert to XML, send to NPCI -----
    @Async
    public void processAsync(byte[] isoBytes, String pathTxnId) {
        try {
            processFromSwitch(isoBytes, pathTxnId);
        } catch (Exception e) {
            System.err.println("ReqPayService (Switch) ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void processFromSwitch(byte[] isoBytes, String pathTxnId) {
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : UNKNOWN_TXN;

        auditService.saveRawBytesWithParsed(txnId, "SWITCH_REQPAY_ISO_IN", isoBytes);
        // Institution (IFSC) validation – IMPS only (DE33 = payee IFSC)
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(new com.hitachi.imps.iso.ImpsIsoPackager());
            iso.unpack(isoBytes);
            String payeeIfsc = iso.hasField(33) ? iso.getString(33) : null;
            String instErr = institutionValidationService.validatePayeeIfsc(payeeIfsc);
            if (instErr != null) {
                System.out.println("IMPS: ReqPay from Switch – institution invalid: " + instErr + ", not forwarding to NPCI");
                return;
            }
        } catch (Exception e) {
            System.err.println("IMPS: ReqPay from Switch – could not validate IFSC: " + e.getMessage());
        }
        String xml = isoToXmlConverter.convertReqPayToXml(isoBytes);
        auditService.saveRaw(txnId, "NPCI_REQPAY_XML_OUT", xml);

        try {
            if (txnId != null && !txnId.isBlank())
                npciMockClient.sendReqPay(xml, txnId);
            else
                npciMockClient.sendReqPay(xml);
        } catch (Exception e) {
            System.out.println("NPCI Mock Client not available: " + e.getMessage());
        }
    }
}
