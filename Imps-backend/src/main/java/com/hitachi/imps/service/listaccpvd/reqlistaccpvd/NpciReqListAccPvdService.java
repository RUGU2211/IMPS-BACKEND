package com.hitachi.imps.service.listaccpvd.reqlistaccpvd;

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
            process(xml);
        } catch (Exception e) {
            System.err.println("NpciReqListAccPvdService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void process(String xml) {
        String msgId = xmlParsingService.extractMsgId(xml);
        String txnId = xmlParsingService.extractTxnId(xml);
        if (txnId == null || txnId.isBlank()) {
            txnId = msgId;
        }

        // 1. Create transaction record (txn_type = LISTACCPVD, switch_status = INIT)
        TransactionEntity txn = transactionService.createRequest(txnId, xml, "LISTACCPVD");

        // 2. Audit incoming XML
        auditService.saveRaw(msgId, "NPCI_REQLISTACCPVD_XML_IN", xml);

        System.out.println("=== Processing NPCI ReqListAccPvd ===");
        System.out.println("MsgId: " + msgId + ", TxnId: " + txnId);

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

        // 7. Send response to NPCI Mock Client (optional - won't fail if not running)
        try {
            String response = npciMockClient.sendRespListAccPvd(respXml);
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
