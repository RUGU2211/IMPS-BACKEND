package com.hitachi.imps.service.listaccpvd;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hitachi.imps.client.NpciMockClient;
import com.hitachi.imps.entity.InstitutionMaster;
import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.repository.InstitutionMasterRepository;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.npci.INpciResponseSender;
import com.hitachi.imps.service.socket.PendingSocketResponseStore;
import com.hitachi.imps.service.util.ResponseIdHelper;
import com.hitachi.imps.service.XmlParsingService;
import com.hitachi.imps.spec.AccPvdSpec;

/** ReqListAccPvd: NPCI (XML) and Switch (ISO). txn_id from path/XML only. RespListAccPvd format must follow NPCI_IMPS_Message_Formats.md (project root). */
@Service
public class ReqListAccPvdService {

    private static final String UNKNOWN_TXN = "UNKNOWN";

    @Autowired private InstitutionMasterRepository institutionRepo;
    @Autowired private NpciMockClient npciMockClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private TransactionService transactionService;
    @Autowired private PendingSocketResponseStore pendingSocketStore;
    @Autowired(required = false) private INpciResponseSender npciResponseSender;

    @Async
    public void processAsync(String xml, String pathTxnId) {
        try { processFromNpci(xml, pathTxnId); } catch (Exception e) { System.err.println("ReqListAccPvdService (NPCI) ERROR: " + e.getMessage()); }
    }

    @Transactional
    public void processFromNpci(String xml, String pathTxnId) {
        String msgId = xmlParsingService.extractMsgId(xml);
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : xmlParsingService.extractTxnId(xml);
        if (txnId == null || txnId.isBlank()) txnId = msgId;
        TransactionEntity txn = transactionService.createRequest(txnId, xml, "LISTACCPVD");  // INIT when req received
        auditService.saveRaw(txnId, "NPCI_REQLISTACCPVD_XML_IN", xml);
        List<InstitutionMaster> banks = institutionRepo.findByActiveTrue();
        String respXml = buildRespListAccPvd(msgId, banks);
        auditService.saveRaw(txnId, "NPCI_RESPLISTACCPVD_XML_OUT", respXml);
        transactionService.markSuccess(txn, respXml, null, null);  // SUCCESS when resp sent to NPCI
        if (sendToNpci(txnId, respXml)) return;
        try { npciMockClient.sendRespListAccPvd(respXml, txnId); } catch (Exception e) { System.out.println("NPCI Mock not available: " + e.getMessage()); }
    }

    private boolean sendToNpci(String txnId, String respXml) {
        if (npciResponseSender != null) return npciResponseSender.send(txnId, respXml);
        return pendingSocketStore.completePending(txnId, respXml);
    }

    @Async
    public void processAsync(byte[] isoBytes, String pathTxnId) {
        try { processFromSwitch(isoBytes, pathTxnId); } catch (Exception e) { System.err.println("ReqListAccPvdService (Switch) ERROR: " + e.getMessage()); }
    }

    public void processFromSwitch(byte[] isoBytes, String pathTxnId) {
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : UNKNOWN_TXN;
        auditService.saveRawBytesWithParsed(txnId, "SWITCH_REQLISTACCPVD_ISO_IN", isoBytes);
        List<InstitutionMaster> banks = institutionRepo.findByActiveTrue();
        String respXml = buildRespListAccPvd(txnId, banks);
        auditService.saveRaw(txnId, "SWITCH_RESPLISTACCPVD_OUT", respXml);
    }

    private String buildRespListAccPvd(String reqMsgId, List<InstitutionMaster> banks) {
        StringBuilder accList = new StringBuilder();
        for (InstitutionMaster bank : banks) {
            String name = bank.getName() != null ? bank.getName() : "";
            String iin = AccPvdSpec.formatIin(bank.getnBinCode());
            String bankCode = AccPvdSpec.formatBankCode(bank.getBankCode());
            String ifsc = bank.getIfscCode() != null ? bank.getIfscCode() : "";
            String active = AccPvdSpec.formatActive(bank.getActive());
            String lastModifiedTs = bank.getLastModifiedTs() != null ? bank.getLastModifiedTs().toString() : OffsetDateTime.now().toString();
            StringBuilder accPvd = new StringBuilder();
            accPvd.append("<AccPvd name=\"").append(escapeXml(name)).append("\" iin=\"").append(escapeXml(iin)).append("\" bankCode=\"").append(escapeXml(bankCode)).append("\" ifsc=\"").append(escapeXml(ifsc)).append("\" active=\"").append(active).append("\" ");
            if (bank.getUrl() != null && !bank.getUrl().isBlank()) accPvd.append("url=\"").append(escapeXml(bank.getUrl())).append("\" ");
            if (bank.getSpocName() != null && !bank.getSpocName().isBlank()) accPvd.append("spocName=\"").append(escapeXml(bank.getSpocName())).append("\" ");
            if (bank.getSpocEmail() != null && !bank.getSpocEmail().isBlank()) accPvd.append("spocEmail=\"").append(escapeXml(bank.getSpocEmail())).append("\" ");
            if (bank.getSpocPhone() != null && !bank.getSpocPhone().isBlank()) accPvd.append("spocPhone=\"").append(escapeXml(bank.getSpocPhone())).append("\" ");
            accPvd.append("prods=\"").append(AccPvdSpec.PRODS_IMPS).append("\" lastModifiedTs=\"").append(escapeXml(lastModifiedTs)).append("\" featureSupported=\"").append(AccPvdSpec.FEATURE_SUPPORTED_DEFAULT).append("\"/>");
            accList.append(accPvd);
        }
        String respMsgId = ResponseIdHelper.responseMsgIdFromRequest(reqMsgId);
        String ts = OffsetDateTime.now().toString();
        return "<ns2:RespListAccPvd xmlns:ns2=\"http://npci.org/upi/schema/\"><Head ver=\"2.0\" ts=\"" + ts + "\" orgId=\"BANK01\" msgId=\"" + respMsgId + "\" prodType=\"IMPS\"/><Txn type=\"ListAccPvd\"/><Resp reqMsgId=\"" + escapeXml(reqMsgId) + "\" result=\"SUCCESS\"/><AccPvdList>" + accList + "</AccPvdList></ns2:RespListAccPvd>";
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
