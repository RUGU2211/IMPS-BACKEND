package com.hitachi.imps.service.pay;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.ISwitchClient;
import com.hitachi.imps.client.NpciMockClient;
import com.hitachi.imps.converter.IsoToXmlConverter;
import com.hitachi.imps.converter.XmlToIsoConverter;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.TransactionValidationService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.socket.PendingSocketResponseStore;
import com.hitachi.imps.service.XmlParsingService;
import com.hitachi.imps.util.IsoUtil;

/**
 * RespPay: single service for both NPCI→IMPS (XML) and Switch→IMPS (ISO).
 * txn_id from path or DE120 only; no random generation.
 */
@Service
public class RespPayService {

    private static final String UNKNOWN_TXN = "UNKNOWN";

    @Autowired private XmlToIsoConverter xmlToIsoConverter;
    @Autowired private IsoToXmlConverter isoToXmlConverter;
    @Autowired private ISwitchClient switchClient;
    @Autowired private NpciMockClient npciMockClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private TransactionService transactionService;
    @Autowired private TransactionValidationService validationService;
    @Autowired private PendingSocketResponseStore pendingSocketStore;

    // ----- NPCI → IMPS (XML): receive RespPay XML, convert to ISO, send to Switch -----
    @Async
    public void processAsync(String xml, String pathTxnId) {
        try {
            processFromNpci(xml, pathTxnId, null);
        } catch (Exception e) {
            System.err.println("RespPayService (NPCI) ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void processAsync(String xml, String pathTxnId, String reqMsgId) {
        try {
            processFromNpci(xml, pathTxnId, reqMsgId);
        } catch (Exception e) {
            System.err.println("RespPayService (NPCI) ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void processFromNpci(String xml, String pathTxnId) {
        processFromNpci(xml, pathTxnId, null);
    }

    public void processFromNpci(String xml, String pathTxnId, String knownReqMsgId) {
        String msgId = (knownReqMsgId != null && !knownReqMsgId.isBlank()) ? knownReqMsgId : xmlParsingService.extractMsgId(xml);
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : xmlParsingService.extractTxnId(xml);
        if (txnId == null || txnId.isBlank()) txnId = msgId;

        auditService.saveRaw(txnId, "NPCI_RESPPAY_XML_IN", xml);
        ISOMsg iso = xmlToIsoConverter.convertRespPay(xml);
        auditService.saveParsed(txnId, "SWITCH_RESPPAY_ISO_OUT", iso);
        switchClient.sendRespPay(iso, txnId);
    }

    // ----- Switch → IMPS (ISO): receive RespPay ISO, update txn, convert to XML, send to NPCI -----
    @Async
    public void processAsync(byte[] isoBytes, String pathTxnId) {
        try {
            processFromSwitch(isoBytes, pathTxnId);
        } catch (Exception e) {
            System.err.println("RespPayService (Switch) ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void processFromSwitch(byte[] isoBytes, String pathTxnId) {
        ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
        String origTxnId = null;
        String respCodeVal = null;
        String approvalNumVal = null;
        try {
            origTxnId = iso.getString(120);
            respCodeVal = iso.getString(39);
            approvalNumVal = iso.getString(38);
        } catch (Exception e) {
            System.err.println("Error extracting ISO fields: " + e.getMessage());
        }
        final String respCode = respCodeVal;
        final String approvalNum = approvalNumVal;
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId
            : (origTxnId != null && !origTxnId.isBlank() ? origTxnId : UNKNOWN_TXN);

        auditService.saveParsed(txnId, "SWITCH_RESPPAY_ISO_IN", iso);

        String xml = isoToXmlConverter.convertRespPayToXml(isoBytes);
        String lookupId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : origTxnId;
        try {
            if (lookupId != null && !lookupId.isBlank()) {
                transactionService.findOptionalByTxnId(lookupId).ifPresent(txn -> {
                    TransactionValidationService.ValidationResult vr = validationService.validateTransaction(iso, txn);
                    if (!vr.isValid())
                        vr.getValidations().forEach((k, v) -> System.out.println("  " + k + ": " + v));
                    if ("00".equals(respCode))
                        transactionService.markSuccess(txn, xml, approvalNum, null);
                    else
                        transactionService.markFailure(txn, xml);
                });
            }
        } catch (Exception e) {
            System.err.println("Error updating transaction: " + e.getMessage());
        }

        auditService.saveRaw(txnId, "NPCI_RESPPAY_XML_OUT", xml);
        String txnIdForNpci = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : origTxnId;
        if (txnIdForNpci != null && pendingSocketStore.completePending(txnIdForNpci, xml)) return;
        try {
            if (txnIdForNpci != null && !txnIdForNpci.isBlank())
                npciMockClient.sendRespPay(xml, txnIdForNpci);
            else
                npciMockClient.sendRespPay(xml);
        } catch (Exception e) {
            System.out.println("NPCI Mock Client not available: " + e.getMessage());
        }
    }
}
