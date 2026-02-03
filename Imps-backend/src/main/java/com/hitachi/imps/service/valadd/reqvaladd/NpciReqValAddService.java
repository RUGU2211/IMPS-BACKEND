package com.hitachi.imps.service.valadd.reqvaladd;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.NpciMockClient;
import com.hitachi.imps.client.SwitchClient;
import com.hitachi.imps.converter.XmlToIsoConverter;
import com.hitachi.imps.entity.AccountMaster;
import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.repository.AccountMasterRepository;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.XmlParsingService;

/**
 * Service for handling ReqValAdd (Name Enquiry) requests from NPCI.
 * 
 * Flow: NPCI (XML) → App → Check local DB or forward to Switch
 */
@Service
public class NpciReqValAddService {

    @Autowired private XmlToIsoConverter xmlToIsoConverter;
    @Autowired private SwitchClient switchClient;
    @Autowired private NpciMockClient npciMockClient;
    @Autowired private AccountMasterRepository accountRepo;
    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private TransactionService transactionService;

    @Async
    public void processAsync(String xml) {
        try {
            process(xml);
        } catch (Exception e) {
            System.err.println("NpciReqValAddService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(String xml) {
        String msgId = xmlParsingService.extractMsgId(xml);

        // 1. Audit incoming XML
        auditService.saveRaw(msgId, "NPCI_REQVALADD_XML_IN", xml);

        System.out.println("=== Processing NPCI ReqValAdd ===");
        System.out.println("MsgId: " + msgId);

        // 2. Parse request
        Map<String, String> parsed = xmlParsingService.parseReqValAdd(xml);
        String acNum = parsed.get("ACNUM");
        String ifsc = parsed.get("IFSC");

        System.out.println("Account: " + acNum + ", IFSC: " + ifsc);

        // 3. Try to validate locally first
        String respXml;
        if (acNum != null && ifsc != null) {
            AccountMaster acc = accountRepo
                .findByAccountNumberAndIfscCodeAndAccountStatusAndImpsEnabled(
                    acNum.trim(),
                    ifsc.trim().toUpperCase(),
                    "ACTIVE",
                    "Y"
                )
                .orElse(null);

            if (acc != null) {
                // Found locally - send success response (account_master used for validation)
                respXml = buildSuccessResponse(msgId, acc.getAccountHolderName(), acNum, ifsc);
                System.out.println("=== Account Found Locally ===");
            } else {
                // Not found locally - forward to Switch
                System.out.println("=== Account Not Found Locally - Forwarding to Switch ===");
                // Use Txn @id so it matches DE120 in Switch response (for DB lookup)
                String txnId = xmlParsingService.extractTxnId(xml);
                if (txnId == null || txnId.isBlank()) {
                    txnId = msgId;
                }
                TransactionEntity txn = transactionService.createRequest(txnId, xml, "VALADD");
                ISOMsg iso = xmlToIsoConverter.convertReqValAdd(xml);
                auditService.saveParsed(msgId, "SWITCH_REQVALADD_ISO_OUT", iso);
                printIso(iso);
                switchClient.sendReqValAdd(iso);
                transactionService.markIsoSent(txn);
                return; // Response will come from Switch
            }
        } else {
            // Invalid request
            respXml = buildFailureResponse(msgId, "14", "Invalid Account Details");
        }

        // 4. Send response to NPCI Mock Client (optional - won't fail if not running)
        auditService.saveRaw(msgId, "NPCI_RESPVALADD_XML_OUT", respXml);
        System.out.println("=== RespValAdd XML ===");
        System.out.println(respXml);

        try {
            String response = npciMockClient.sendRespValAdd(respXml);
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

    private String buildSuccessResponse(String reqMsgId, String name, String acNum, String ifsc) {
        return """
            <ns2:RespValAdd xmlns:ns2="http://npci.org/upi/schema/">
                <Head ver="2.0"
                      ts="%s"
                      orgId="BANK01"
                      msgId="%s"
                      prodType="IMPS"/>
                <Txn type="NameEnq" ts="%s"/>
                <Resp reqMsgId="%s"
                      result="SUCCESS"
                      IFSC="%s"
                      acNum="%s"
                      accType="DEFAULT"
                      approvalNum="%s"
                      code="0000"
                      maskName="%s"
                      type="PERSON"/>
            </ns2:RespValAdd>
            """.formatted(
                OffsetDateTime.now(),
                UUID.randomUUID().toString().replace("-", "").substring(0, 20),
                OffsetDateTime.now(),
                reqMsgId,
                ifsc,
                acNum,
                String.format("%06d", System.currentTimeMillis() % 1000000),
                name != null ? name : "ACCOUNT HOLDER"
            );
    }

    private String buildFailureResponse(String reqMsgId, String respCode, String reason) {
        return """
            <ns2:RespValAdd xmlns:ns2="http://npci.org/upi/schema/">
                <Head ver="2.0"
                      ts="%s"
                      orgId="BANK01"
                      msgId="%s"
                      prodType="IMPS"/>
                <Resp reqMsgId="%s"
                      result="FAILURE"
                      respCode="%s"/>
            </ns2:RespValAdd>
            """.formatted(
                OffsetDateTime.now(),
                UUID.randomUUID().toString().replace("-", "").substring(0, 20),
                reqMsgId,
                respCode
            );
    }

    private void printIso(ISOMsg iso) {
        try {
            System.out.println("MTI: " + iso.getMTI());
            for (int i = 0; i <= 128; i++) {
                if (iso.hasField(i)) {
                    System.out.println("DE" + i + ": " + iso.getString(i));
                }
            }
        } catch (Exception e) {
            System.err.println("Error printing ISO: " + e.getMessage());
        }
    }
}
