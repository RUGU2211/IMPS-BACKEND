package com.hitachi.imps.service.listaccpvd.reqlistaccpvd;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hitachi.imps.client.NpciMockClient;
import com.hitachi.imps.entity.InstitutionMaster;
import com.hitachi.imps.spec.AccPvdSpec;
import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.repository.InstitutionMasterRepository;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.XmlParsingService;

/**
 * Service for handling ReqListAccPvd requests from NPCI.
 * 
 * Flow: NPCI (XML) → App → Create txn record → Build Response from DB → Mark SUCCESS → Send RespListAccPvd to NPCI
 * Note: ListAccPvd typically doesn't go to Switch - it's fetched from local DB
 */
@Service
public class NpciReqListAccPvdService {

    @Autowired private InstitutionMasterRepository institutionRepo;
    @Autowired private NpciMockClient npciMockClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private TransactionService transactionService;

    @Async
    public void processAsync(String xml) {
        try {
            process(xml, null);
        } catch (Exception e) {
            System.err.println("NpciReqListAccPvdService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void processAsync(String xml, String pathTxnId) {
        try {
            process(xml, pathTxnId);
        } catch (Exception e) {
            System.err.println("NpciReqListAccPvdService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void process(String xml) {
        process(xml, null);
    }

    @Transactional
    public void process(String xml, String pathTxnId) {
        String msgId = xmlParsingService.extractMsgId(xml);
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : xmlParsingService.extractTxnId(xml);
        if (txnId == null || txnId.isBlank()) txnId = msgId;

        // 1. Create transaction record (txn_type = LISTACCPVD, switch_status = INIT)
        TransactionEntity txn = transactionService.createRequest(txnId, xml, "LISTACCPVD");

        // 2. Audit incoming XML
        auditService.saveRaw(msgId, "NPCI_REQLISTACCPVD_XML_IN", xml);

        System.out.println("=== Processing NPCI ReqListAccPvd ===");
        System.out.println("MsgId: " + msgId + ", TxnId: " + txnId);
        System.out.println("switch reqlistaccpvd");

        // 3. Fetch active institutions from DB
        List<InstitutionMaster> banks = institutionRepo.findByActiveTrue();

        // 4. Build response XML
        String respXml = buildRespListAccPvd(msgId, banks);

        // 5. Mark transaction SUCCESS (no Switch; response built locally)
        transactionService.markSuccess(txn, respXml, null, null);

        // 6. Audit response
        auditService.saveRaw(msgId, "NPCI_RESPLISTACCPVD_XML_OUT", respXml);

        System.out.println("=== RespListAccPvd XML Built ===");
        System.out.println(respXml);

        // 7. Send response to NPCI Mock: POST /npci/resplistaccpvd/{txnId}
        try {
            String response = npciMockClient.sendRespListAccPvd(respXml, txnId);
            if (response != null) {
                System.out.println("=== NPCI MOCK CLIENT ACK Received ===");
            } else {
                System.out.println("=== WARNING: NPCI Mock Client not available (port 8083) - Continuing without ACK ===");
            }
        } catch (Exception e) {
            System.out.println("=== WARNING: NPCI Mock Client not available - Continuing without ACK ===");
            System.out.println("   Error: " + e.getMessage());
        }
    }

    /**
     * Build RespListAccPvd per NPCI spec 7.4.3 – Account Provider.
     * AccPvdList 1..1, AccPvd 1..n with: name, iin (4-digit NBIN), bankCode (3-digit), ifsc, active (Y/N),
     * url (0..n), spocName, spocEmail, spocPhone (0..n), prods (0..n e.g. IMPS), lastModifiedTs (1..1 ISODateTime), featureSupported (0..1 "01").
     */
    private String buildRespListAccPvd(String msgId, List<InstitutionMaster> banks) {
        StringBuilder accList = new StringBuilder();

        for (InstitutionMaster bank : banks) {
            String name = bank.getName() != null ? bank.getName() : "";
            String iin = AccPvdSpec.formatIin(bank.getnBinCode());
            String bankCode = AccPvdSpec.formatBankCode(bank.getBankCode());
            String ifsc = bank.getIfscCode() != null ? bank.getIfscCode() : "";
            String active = AccPvdSpec.formatActive(bank.getActive());
            String lastModifiedTs = bank.getLastModifiedTs() != null
                ? bank.getLastModifiedTs().toString()
                : OffsetDateTime.now().toString();

            StringBuilder accPvd = new StringBuilder();
            accPvd.append("<AccPvd name=\"").append(escapeXml(name)).append("\" ");
            accPvd.append("iin=\"").append(escapeXml(iin)).append("\" ");
            accPvd.append("bankCode=\"").append(escapeXml(bankCode)).append("\" ");
            accPvd.append("ifsc=\"").append(escapeXml(ifsc)).append("\" ");
            accPvd.append("active=\"").append(active).append("\" ");
            if (bank.getUrl() != null && !bank.getUrl().isBlank())
                accPvd.append("url=\"").append(escapeXml(bank.getUrl())).append("\" ");
            if (bank.getSpocName() != null && !bank.getSpocName().isBlank())
                accPvd.append("spocName=\"").append(escapeXml(bank.getSpocName())).append("\" ");
            if (bank.getSpocEmail() != null && !bank.getSpocEmail().isBlank())
                accPvd.append("spocEmail=\"").append(escapeXml(bank.getSpocEmail())).append("\" ");
            if (bank.getSpocPhone() != null && !bank.getSpocPhone().isBlank())
                accPvd.append("spocPhone=\"").append(escapeXml(bank.getSpocPhone())).append("\" ");
            accPvd.append("prods=\"").append(AccPvdSpec.PRODS_IMPS).append("\" ");
            accPvd.append("lastModifiedTs=\"").append(escapeXml(lastModifiedTs)).append("\" ");
            accPvd.append("featureSupported=\"").append(AccPvdSpec.FEATURE_SUPPORTED_DEFAULT).append("\"/>");
            accList.append(accPvd);
        }

        // Rule 021: response Head msgId 35 chars (new id; reqMsgId keeps request correlation)
        String respMsgId = "RSP" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String ts = OffsetDateTime.now().toString();
        return """
            <ns2:RespListAccPvd xmlns:ns2="http://npci.org/upi/schema/">
                <Head ver="2.0" ts="%s" orgId="BANK01" msgId="%s" prodType="IMPS"/>
                <Txn type="ListAccPvd"/>
                <Resp reqMsgId="%s" result="SUCCESS"/>
                <AccPvdList>
                    %s
                </AccPvdList>
            </ns2:RespListAccPvd>
            """.formatted(ts, respMsgId, msgId, accList.toString());
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
