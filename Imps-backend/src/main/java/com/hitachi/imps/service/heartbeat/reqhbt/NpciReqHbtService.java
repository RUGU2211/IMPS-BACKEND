package com.hitachi.imps.service.heartbeat.reqhbt;

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
import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.XmlParsingService;

/**
 * Service for handling ReqHbt (Heartbeat) requests from NPCI.
 * 
 * Flow: NPCI (XML) → App → Switch (ISO)
 * Also sends RespHbt back to NPCI immediately
 */
@Service
public class NpciReqHbtService {

    @Autowired private XmlToIsoConverter xmlToIsoConverter;
    @Autowired private SwitchClient switchClient;
    @Autowired private NpciMockClient npciMockClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private TransactionService transactionService;

    @Async
    public void processAsync(String xml) {
        try {
            process(xml);
        } catch (Exception e) {
            System.err.println("NpciReqHbtService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(String xml) {
        String msgId = xmlParsingService.extractMsgId(xml);
        String txnId = xmlParsingService.extractTxnId(xml);

        // 1. Audit incoming XML
        auditService.saveRaw(msgId, "NPCI_REQHBT_XML_IN", xml);

        // 2. Create transaction record (txn_type = HBT, switch_status = INIT)
        TransactionEntity txn = transactionService.createRequest(
            txnId != null && !txnId.isBlank() ? txnId : msgId, xml, "HBT");

        System.out.println("=== Processing NPCI ReqHbt ===");
        System.out.println("MsgId: " + msgId + ", TxnId: " + txnId);

        // 3. Parse heartbeat details
        Map<String, String> hbt = xmlParsingService.parseReqHbt(xml);
        String note = hbt.get("note");
        String refId = hbt.get("ref_id");
        String txnTs = hbt.get("txn_ts");

        // 4. Build and send RespHbt back to NPCI
        String respXml = buildRespHbt(msgId, txnId, note, refId, txnTs);
        auditService.saveRaw(msgId, "NPCI_RESPHBT_XML_OUT", respXml);

        System.out.println("=== RespHbt XML Built ===");
        System.out.println(respXml);

        // Send response to NPCI Mock Client (optional - won't fail if not running)
        try {
            String response = npciMockClient.sendRespHbt(respXml);
            if (response != null) {
                System.out.println("=== NPCI MOCK CLIENT ACK Received ===");
            } else {
                System.out.println("=== WARNING: NPCI Mock Client not available (port 8083) - Continuing without ACK ===");
            }
        } catch (Exception e) {
            System.out.println("=== WARNING: NPCI Mock Client not available - Continuing without ACK ===");
            System.out.println("   Error: " + e.getMessage());
        }

        // 5. Forward heartbeat to Switch and mark ISO sent
        ISOMsg iso = xmlToIsoConverter.convertReqHbt(xml);
        auditService.saveParsed(msgId, "SWITCH_REQHBT_ISO_OUT", iso);

        System.out.println("=== ISO Heartbeat Message Built ===");
        printIso(iso);

        transactionService.markIsoSent(txn);
        switchClient.sendReqHbt(iso);
    }

    private String buildRespHbt(String reqMsgId, String txnId, String note, String refId, String txnTs) {
        return """
            <upi:RespHbt xmlns:upi="http://npci.org/upi/schema/">
                <Head ver="1.0"
                      ts="%s"
                      orgId="BANK01"
                      msgId="%s"/>
                <Txn id="%s"
                     note="%s"
                     refId="%s"
                     refUrl=""
                     ts="%s"
                     type="Hbt"/>
                <Resp reqMsgId="%s"
                      result="SUCCESS"/>
            </upi:RespHbt>
            """.formatted(
                OffsetDateTime.now(),
                UUID.randomUUID().toString().replace("-", "").substring(0, 20),
                txnId != null ? txnId : "",
                note != null ? note : "",
                refId != null ? refId : "",
                txnTs != null ? txnTs : OffsetDateTime.now().toString(),
                reqMsgId
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
