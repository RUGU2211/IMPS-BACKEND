package com.hitachi.imps.service.listaccpvd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.jpos.iso.ISOMsg;

import com.hitachi.imps.client.npci.NpciRestClient;
import com.hitachi.imps.converter.XmlToIsoConverter;
import com.hitachi.imps.entity.InstitutionMaster;
import com.hitachi.imps.util.IsoUtil;
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

    private static final Logger log = LoggerFactory.getLogger(ReqListAccPvdService.class);

    @Value("${imps.org-id:BANK01}")
    private String orgId;

    private static final String UNKNOWN_TXN = "UNKNOWN";
    /** Rule 020: Head ts = ISO with up to 3 fractional seconds. */
    private static final DateTimeFormatter HEAD_TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Autowired private InstitutionMasterRepository institutionRepo;
    @Autowired private NpciRestClient npciRestClient;
    @Autowired private XmlToIsoConverter xmlToIsoConverter;
    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private TransactionService transactionService;
    @Autowired private PendingSocketResponseStore pendingSocketStore;
    @Autowired(required = false) private INpciResponseSender npciResponseSender;

    @Async
    public void processAsync(String xml, String pathTxnId) {
        try { processFromNpci(xml, pathTxnId, null); } catch (Exception e) { log.error("ReqListAccPvdService (NPCI) error", e); }
    }

    @Async
    public void processAsync(String xml, String pathTxnId, String reqMsgId) {
        try { processFromNpci(xml, pathTxnId, reqMsgId); } catch (Exception e) { log.error("ReqListAccPvdService (NPCI) error", e); }
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
        TransactionEntity txn = transactionService.createRequest(txnId, xml, "LISTACCPVD");  // INIT when req received
        auditService.saveRaw(txnId, "NPCI_REQLISTACCPVD_XML_IN", xml);
        List<InstitutionMaster> banks = institutionRepo.findByActiveTrue();
        String respXml = buildRespListAccPvd(msgId, banks);
        auditService.saveRaw(txnId, "NPCI_RESPLISTACCPVD_XML_OUT", respXml);
        transactionService.markSuccess(txn, respXml, null, null);  // SUCCESS when resp sent to NPCI
        if (sendToNpci(txnId, respXml)) return;
        try { npciRestClient.sendRespListAccPvd(respXml, txnId); } catch (Exception e) { log.warn("NPCI not available: {}", e.getMessage()); }
    }

    private boolean sendToNpci(String txnId, String respXml) {
        if (npciResponseSender != null) return npciResponseSender.send(txnId, respXml);
        return pendingSocketStore.completePending(txnId, respXml);
    }

    @Async
    public void processAsync(byte[] isoBytes, String pathTxnId) {
        try { processFromSwitch(isoBytes, pathTxnId); } catch (Exception e) { log.error("ReqListAccPvdService (Switch) error", e); }
    }

    public void processFromSwitch(byte[] isoBytes, String pathTxnId) {
        processFromSwitchSync(isoBytes, pathTxnId);
    }

    /** Reverse flow: Switch sends Req ISO → IMPS builds Resp from DB → converts to ISO, returns. Duplicate txn_id rejected (409). */
    public byte[] processFromSwitchSync(byte[] isoBytes, String pathTxnId) {
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : UNKNOWN_TXN;
        if (!UNKNOWN_TXN.equals(txnId))
            transactionService.validateNewTxnId(txnId);
        auditService.saveRawBytesWithParsed(txnId, "SWITCH_REQLISTACCPVD_ISO_IN", isoBytes);
        String reqStored = "<ReqListAccPvd from=\"Switch\"/>";
        TransactionEntity txn = transactionService.createRequest(txnId, reqStored, "LISTACCPVD");
        List<InstitutionMaster> banks = institutionRepo.findByActiveTrue();
        String respXml = buildRespListAccPvd(txnId, banks);
        auditService.saveRaw(txnId, "SWITCH_RESPLISTACCPVD_XML", respXml);
        try {
            ISOMsg respIso = xmlToIsoConverter.convertRespListAccPvd(respXml);
            byte[] respBytes = IsoUtil.pack(respIso);
            auditService.saveRawBytesWithParsed(txnId, "SWITCH_RESPLISTACCPVD_ISO_OUT", respBytes);
            transactionService.markSuccess(txn, respXml, null, null);
            return respBytes;
        } catch (Exception e) {
            log.error("IMPS: RespListAccPvd XML to ISO failed", e);
            transactionService.markFailure(txn, null);
            return null;
        }
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
        String ts = OffsetDateTime.now().format(HEAD_TS_FORMAT);
        String orgIdTrunc = com.hitachi.imps.converter.RespPaySpec.truncate(orgId, com.hitachi.imps.converter.RespPaySpec.HEAD_ORGID_MAX);
        return "<ns2:RespListAccPvd xmlns:ns2=\"http://npci.org/upi/schema/\"><Head ver=\"2.0\" ts=\"" + ts + "\" orgId=\"" + escapeXml(orgIdTrunc) + "\" msgId=\"" + respMsgId + "\" prodType=\"IMPS\"/><Txn type=\"ListAccPvd\"/><Resp reqMsgId=\"" + escapeXml(reqMsgId) + "\" result=\"SUCCESS\"/><AccPvdList>" + accList + "</AccPvdList></ns2:RespListAccPvd>";
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
