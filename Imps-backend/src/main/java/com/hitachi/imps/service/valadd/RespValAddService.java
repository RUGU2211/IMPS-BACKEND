package com.hitachi.imps.service.valadd;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.switchclient.ISwitchClient;
import com.hitachi.imps.client.npci.NpciRestClient;
import com.hitachi.imps.converter.IsoToXmlConverter;
import com.hitachi.imps.converter.XmlToIsoConverter;
import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.socket.PendingSocketResponseStore;
import com.hitachi.imps.service.XmlParsingService;
import com.hitachi.imps.util.IsoUtil;

/** RespValAdd: NPCI (XML) and Switch (ISO). txn_id from path/DE120 only. */
@Service
public class RespValAddService {

    private static final String UNKNOWN_TXN = "UNKNOWN";

    @Autowired private XmlToIsoConverter xmlToIsoConverter;
    @Autowired private IsoToXmlConverter isoToXmlConverter;
    @Autowired private ISwitchClient switchClient;
    @Autowired private NpciRestClient npciRestClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private TransactionService transactionService;
    @Autowired private PendingSocketResponseStore pendingSocketStore;

    @Async
    public void processAsync(String xml, String pathTxnId) {
        try { processFromNpci(xml, pathTxnId, null); } catch (Exception e) { System.err.println("RespValAddService (NPCI) ERROR: " + e.getMessage()); }
    }

    @Async
    public void processAsync(String xml, String pathTxnId, String reqMsgId) {
        try { processFromNpci(xml, pathTxnId, reqMsgId); } catch (Exception e) { System.err.println("RespValAddService (NPCI) ERROR: " + e.getMessage()); }
    }

    public void processFromNpci(String xml, String pathTxnId) {
        processFromNpci(xml, pathTxnId, null);
    }

    public void processFromNpci(String xml, String pathTxnId, String knownReqMsgId) {
        String msgId = (knownReqMsgId != null && !knownReqMsgId.isBlank()) ? knownReqMsgId : xmlParsingService.extractMsgId(xml);
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : xmlParsingService.extractTxnId(xml);
        if (txnId == null || txnId.isBlank()) txnId = msgId;
        auditService.saveRaw(txnId, "NPCI_RESPVALADD_XML_IN", xml);
        ISOMsg iso = xmlToIsoConverter.convertRespValAdd(xml);
        auditService.saveParsed(txnId, "SWITCH_RESPVALADD_ISO_OUT", iso);
        switchClient.sendRespValAdd(iso, txnId);
    }

    @Async
    public void processAsync(byte[] isoBytes, String pathTxnId) {
        try { processFromSwitch(isoBytes, pathTxnId); } catch (Exception e) { System.err.println("RespValAddService (Switch) ERROR: " + e.getMessage()); }
    }

    public void processFromSwitch(byte[] isoBytes, String pathTxnId) {
        ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
        String origTxnId = null;
        String respCodeVal = null;
        String approvalNumVal = null;
        try {
            if (iso.hasField(120)) origTxnId = iso.getString(120);
            if (iso.hasField(39)) respCodeVal = iso.getString(39);
            if (iso.hasField(38)) approvalNumVal = iso.getString(38);
        } catch (Exception e) { System.err.println("Error extracting RespValAdd ISO: " + e.getMessage()); }
        final String responseCode = respCodeVal;
        final String approvalNumber = approvalNumVal;
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId
            : (origTxnId != null && !origTxnId.isBlank() ? origTxnId : UNKNOWN_TXN);
        auditService.saveParsed(txnId, "SWITCH_RESPVALADD_ISO_IN", iso);
        String xml = isoToXmlConverter.convertRespValAddToXml(isoBytes);
        String lookupId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : origTxnId;
        try {
            if (lookupId != null && !lookupId.isBlank()) {
                var opt = transactionService.findOptionalByTxnId(lookupId);
                if (opt.isEmpty()) opt = transactionService.findOptionalValAddIsoSentByTxnIdInReqXml(lookupId);
                opt.ifPresent(txn -> {
                    if ("00".equals(responseCode)) transactionService.markSuccess(txn, xml, approvalNumber, null);
                    else transactionService.markFailure(txn, xml);
                });
            }
        } catch (Exception e) { System.err.println("Error updating ValAdd transaction: " + e.getMessage()); }
        auditService.saveRaw(txnId, "NPCI_RESPVALADD_XML_OUT", xml);
        String txnIdForNpci = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : origTxnId;
        if (txnIdForNpci != null && pendingSocketStore.completePending(txnIdForNpci, xml)) return;
        try {
            if (txnIdForNpci != null && !txnIdForNpci.isBlank()) npciRestClient.sendRespValAdd(xml, txnIdForNpci);
            else npciRestClient.sendRespValAdd(xml);
        } catch (Exception e) { System.out.println("NPCI not available: " + e.getMessage()); }
    }
}
