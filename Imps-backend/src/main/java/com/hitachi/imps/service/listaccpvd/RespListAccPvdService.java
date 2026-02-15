package com.hitachi.imps.service.listaccpvd;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.npci.NpciRestClient;
import com.hitachi.imps.converter.IsoToXmlConverter;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.socket.PendingSocketResponseStore;
import com.hitachi.imps.service.XmlParsingService;
import com.hitachi.imps.util.IsoUtil;

/** RespListAccPvd: NPCI (XML) and Switch (ISO). txn_id from path only. */
@Service
public class RespListAccPvdService {

    private static final String UNKNOWN_TXN = "UNKNOWN";

    @Autowired private IsoToXmlConverter isoToXmlConverter;
    @Autowired private NpciRestClient npciRestClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private TransactionService transactionService;
    @Autowired private PendingSocketResponseStore pendingSocketStore;

    @Async
    public void processAsync(String xml, String pathTxnId) {
        try { processFromNpci(xml, pathTxnId, null); } catch (Exception e) { System.err.println("RespListAccPvdService (NPCI) ERROR: " + e.getMessage()); }
    }

    @Async
    public void processAsync(String xml, String pathTxnId, String reqMsgId) {
        try { processFromNpci(xml, pathTxnId, reqMsgId); } catch (Exception e) { System.err.println("RespListAccPvdService (NPCI) ERROR: " + e.getMessage()); }
    }

    public void processFromNpci(String xml, String pathTxnId) {
        processFromNpci(xml, pathTxnId, null);
    }

    public void processFromNpci(String xml, String pathTxnId, String knownReqMsgId) {
        String msgId = (knownReqMsgId != null && !knownReqMsgId.isBlank()) ? knownReqMsgId : xmlParsingService.extractMsgId(xml);
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : xmlParsingService.extractTxnId(xml);
        if (txnId == null || txnId.isBlank()) txnId = msgId;
        auditService.saveRaw(txnId, "NPCI_RESPLISTACCPVD_XML_IN", xml);
    }

    @Async
    public void processAsync(byte[] isoBytes, String pathTxnId) {
        try { processFromSwitch(isoBytes, pathTxnId); } catch (Exception e) { System.err.println("RespListAccPvdService (Switch) ERROR: " + e.getMessage()); }
    }

    public void processFromSwitch(byte[] isoBytes, String pathTxnId) {
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : UNKNOWN_TXN;
        ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
        auditService.saveParsed(txnId, "SWITCH_RESPLISTACCPVD_ISO_IN", iso);
        String xml = isoToXmlConverter.convertRespListAccPvdToXml(isoBytes);
        if (pathTxnId != null && !pathTxnId.isBlank()) {
            try {
                transactionService.findOptionalByTxnId(pathTxnId).ifPresent(txn -> transactionService.markSuccess(txn, xml, null, null));
            } catch (Exception e) { System.err.println("Error updating ListAccPvd transaction: " + e.getMessage()); }
        }
        auditService.saveRaw(txnId, "NPCI_RESPLISTACCPVD_XML_OUT", xml);
        if (pathTxnId != null && pendingSocketStore.completePending(pathTxnId, xml)) return;
        try {
            if (pathTxnId != null && !pathTxnId.isBlank()) npciRestClient.sendRespListAccPvd(xml, pathTxnId);
            else npciRestClient.sendRespListAccPvd(xml);
        } catch (Exception e) { System.out.println("NPCI not available: " + e.getMessage()); }
    }
}
