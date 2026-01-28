package com.hitachi.imps.service.chktxn.reqchktxn;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.SwitchClient;
import com.hitachi.imps.converter.XmlToIsoConverter;
import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.XmlParsingService;

/**
 * Service for handling ReqChkTxn requests from NPCI.
 * 
 * Flow: NPCI (XML) → App → Switch (ISO)
 * 1. Receive XML from NPCI
 * 2. Convert XML to ISO 0200 (inquiry)
 * 3. Send ISO to Switch
 */
@Service
public class NpciReqChkTxnService {

    @Autowired private XmlToIsoConverter xmlToIsoConverter;
    @Autowired private SwitchClient switchClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private TransactionService transactionService;

    @Async
    public void processAsync(String xml) {
        try {
            process(xml);
        } catch (Exception e) {
            System.err.println("NpciReqChkTxnService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(String xml) {
        String msgId = xmlParsingService.extractMsgId(xml);
        String txnId = xmlParsingService.extractTxnId(xml);

        // 1. Audit incoming XML
        auditService.saveRaw(msgId, "NPCI_REQCHKTXN_XML_IN", xml);

        // 2. Create transaction record (txn_type = CHKTXN, switch_status = INIT)
        TransactionEntity txn = transactionService.createRequest(
            txnId != null && !txnId.isBlank() ? txnId : msgId, xml, "CHKTXN");

        System.out.println("=== Processing NPCI ReqChkTxn ===");
        System.out.println("MsgId: " + msgId + ", TxnId: " + txnId);

        // 3. Convert XML to ISO 0200
        ISOMsg iso = xmlToIsoConverter.convertReqChkTxn(xml);

        // 4. Audit ISO message
        auditService.saveParsed(msgId, "SWITCH_REQCHKTXN_ISO_OUT", iso);

        System.out.println("=== ISO Message Built ===");
        printIso(iso);

        // 5. Mark ISO sent and send to Switch
        transactionService.markIsoSent(txn);
        byte[] response = switchClient.sendReqChkTxn(iso);

        if (response != null) {
            System.out.println("=== Switch Response Received ===");
            System.out.println("Response length: " + response.length + " bytes");
        } else {
            System.out.println("=== No Response from Switch ===");
        }
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
