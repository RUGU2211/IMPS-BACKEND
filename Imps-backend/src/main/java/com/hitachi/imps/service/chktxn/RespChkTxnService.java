package com.hitachi.imps.service.chktxn;

import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.switchclient.ISwitchClient;
import com.hitachi.imps.client.npci.NpciRestClient;
import com.hitachi.imps.converter.IsoToXmlConverter;
import com.hitachi.imps.converter.XmlToIsoConverter;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.socket.PendingSocketResponseStore;
import com.hitachi.imps.service.XmlParsingService;
import com.hitachi.imps.util.IsoUtil;

/**
 * RespChkTxn: single service for NPCI→IMPS (XML) and Switch→IMPS (ISO). txn_id from path/DE120 only.
 */
@Service
public class RespChkTxnService {

    private static final Logger log = LoggerFactory.getLogger(RespChkTxnService.class);
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
        try {
            processFromNpci(xml, pathTxnId, null);
        } catch (Exception e) {
            log.error("RespChkTxnService (NPCI) ERROR", e);
        }
    }

    @Async
    public void processAsync(String xml, String pathTxnId, String reqMsgId) {
        try {
            processFromNpci(xml, pathTxnId, reqMsgId);
        } catch (Exception e) {
            log.error("RespChkTxnService (NPCI) ERROR", e);
        }
    }

    public void processFromNpci(String xml, String pathTxnId) {
        processFromNpci(xml, pathTxnId, null);
    }

    public void processFromNpci(String xml, String pathTxnId, String knownReqMsgId) {
        String msgId = (knownReqMsgId != null && !knownReqMsgId.isBlank()) ? knownReqMsgId : xmlParsingService.extractMsgId(xml);
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : xmlParsingService.extractTxnId(xml);
        if (txnId == null || txnId.isBlank()) txnId = msgId;
        auditService.saveRaw(txnId, "NPCI_RESPCHKTXN_XML_IN", xml);
        ISOMsg iso = xmlToIsoConverter.convertRespChkTxn(xml);
        auditService.saveParsed(txnId, "SWITCH_RESPCHKTXN_ISO_OUT", iso);
        switchClient.sendRespChkTxn(iso, txnId);
    }

    @Async
    public void processAsync(byte[] isoBytes, String pathTxnId) {
        try {
            processFromSwitch(isoBytes, pathTxnId);
        } catch (Exception e) {
            log.error("RespChkTxnService (Switch) ERROR", e);
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
            log.warn("Error extracting RespChkTxn ISO fields", e);
        }
        final String responseCode = respCodeVal;
        final String approvalNumber = approvalNumVal;
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId
            : (origTxnId != null && !origTxnId.isBlank() ? origTxnId : UNKNOWN_TXN);
        auditService.saveParsed(txnId, "SWITCH_RESPCHKTXN_ISO_IN", iso);
        String xml = isoToXmlConverter.convertRespChkTxnToXml(isoBytes);
        String lookupId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : origTxnId;
        try {
            if (lookupId != null && !lookupId.isBlank()) {
                transactionService.findOptionalByTxnId(lookupId).ifPresent(txn -> {
                    if ("00".equals(responseCode)) transactionService.markSuccess(txn, xml, approvalNumber, null);
                    else transactionService.markFailure(txn, xml);
                });
            }
        } catch (Exception e) {
            log.error("Error updating ChkTxn transaction", e);
        }
        auditService.saveRaw(txnId, "NPCI_RESPCHKTXN_XML_OUT", xml);
        String txnIdForNpci = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : origTxnId;
        if (txnIdForNpci != null && pendingSocketStore.completePending(txnIdForNpci, xml)) return;
        try {
            if (txnIdForNpci != null && !txnIdForNpci.isBlank()) npciRestClient.sendRespChkTxn(xml, txnIdForNpci);
            else npciRestClient.sendRespChkTxn(xml);
        } catch (Exception e) {
            log.warn("NPCI not available: {}", e.getMessage());
        }
    }
}
