package com.hitachi.imps.service.listaccpvd.reqlistaccpvd;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.entity.InstitutionMaster;
import com.hitachi.imps.repository.InstitutionMasterRepository;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.spec.AccPvdSpec;

/**
 * Service for handling ReqListAccPvd requests from Switch.
 * 
 * Flow: Switch (ISO) → App → Build Response from DB → Send RespListAccPvd (ISO) to Switch
 */
@Service
public class SwitchReqListAccPvdService {

    @Autowired private InstitutionMasterRepository institutionRepo;
    @Autowired private MessageAuditService auditService;

    @Async
    public void processAsync(byte[] isoBytes) {
        processAsync(isoBytes, null);
    }

    @Async
    public void processAsync(byte[] isoBytes, String pathTxnId) {
        try {
            process(isoBytes, pathTxnId);
        } catch (Exception e) {
            System.err.println("SwitchReqListAccPvdService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(byte[] isoBytes) {
        process(isoBytes, null);
    }

    public void process(byte[] isoBytes, String pathTxnId) {
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : "SWITCH_LISTACCPVD_REQ_" + System.currentTimeMillis();

        // 1. Audit incoming ISO (as Base64 for binary safety)
        auditService.saveRawBytes(txnId, "SWITCH_REQLISTACCPVD_ISO_IN", isoBytes);

        System.out.println("=== Processing Switch ReqListAccPvd ===");
        System.out.println("ISO bytes length: " + isoBytes.length);

        // 2. Fetch active institutions from DB
        List<InstitutionMaster> banks = institutionRepo.findByActiveTrue();

        // 3. Build response XML (intermediate step)
        String respXml = buildRespListAccPvd(txnId, banks);

        System.out.println("=== Built Response for " + banks.size() + " banks ===");

        // 4. For ListAccPvd, we send simplified response to Switch
        // Since ListAccPvd doesn't have standard ISO format, we log it
        auditService.saveRaw(txnId, "SWITCH_RESPLISTACCPVD_OUT", respXml);

        System.out.println("=== ListAccPvd Response ===");
        System.out.println(respXml);
    }

    /**
     * Build RespListAccPvd per NPCI spec 7.4.3 – Account Provider.
     * Same structure as NpciReqListAccPvdService: AccPvdList 1..1, AccPvd 1..n with name, iin (4-digit), bankCode (3-digit),
     * ifsc, active (Y/N), optional url/spoc*, prods, lastModifiedTs (1..1), featureSupported (0..1 "01").
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
