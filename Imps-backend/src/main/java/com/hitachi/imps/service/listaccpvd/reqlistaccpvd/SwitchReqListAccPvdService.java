package com.hitachi.imps.service.listaccpvd.reqlistaccpvd;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.SwitchClient;
import com.hitachi.imps.converter.XmlToIsoConverter;
import com.hitachi.imps.entity.InstitutionMaster;
import com.hitachi.imps.repository.InstitutionMasterRepository;
import com.hitachi.imps.service.audit.MessageAuditService;

/**
 * Service for handling ReqListAccPvd requests from Switch.
 * 
 * Flow: Switch (ISO) → App → Build Response from DB → Send RespListAccPvd (ISO) to Switch
 */
@Service
public class SwitchReqListAccPvdService {

    @Autowired private InstitutionMasterRepository institutionRepo;
    @Autowired private SwitchClient switchClient;
    @Autowired private XmlToIsoConverter xmlToIsoConverter;
    @Autowired private MessageAuditService auditService;

    @Async
    public void processAsync(byte[] isoBytes) {
        try {
            process(isoBytes);
        } catch (Exception e) {
            System.err.println("SwitchReqListAccPvdService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(byte[] isoBytes) {
        String txnId = "SWITCH_LISTACCPVD_REQ_" + System.currentTimeMillis();

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

    private String buildRespListAccPvd(String msgId, List<InstitutionMaster> banks) {
        StringBuilder accList = new StringBuilder();

        for (InstitutionMaster bank : banks) {
            accList.append("""
                    <AccPvd name="%s"
                            bankCode="%s"
                            iin="%s"
                            ifsc="%s"
                            active="Y"
                            prods="IMPS"
                            lastModifiedTs="%s"
                            featureSupported="01"/>
                """.formatted(
                    bank.getName() != null ? bank.getName() : "",
                    bank.getBankCode() != null ? bank.getBankCode() : "",
                    bank.getnBinCode() != null ? bank.getnBinCode() : "",
                    bank.getIfscCode() != null ? bank.getIfscCode() : "",
                    OffsetDateTime.now()
                ));
        }

        return """
            <ns2:RespListAccPvd xmlns:ns2="http://npci.org/upi/schema/">
                <Head ver="2.0"
                      ts="%s"
                      orgId="BANK01"
                      msgId="%s"
                      prodType="IMPS"/>
                <Txn type="ListAccPvd"/>
                <Resp reqMsgId="%s" result="SUCCESS" errCode="00"/>
                <AccPvdList>
                    %s
                </AccPvdList>
            </ns2:RespListAccPvd>
            """.formatted(
                OffsetDateTime.now(),
                msgId + "_RESP",
                msgId,
                accList.toString()
            );
    }
}
