package com.hitachi.imps.service.pay.reqpay;

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
 * Service for handling ReqPay requests from NPCI.
 * 
 * Flow: NPCI (XML) → App → Switch (ISO)
 * 1. Receive XML from NPCI
 * 2. Convert XML to ISO 0200
 * 3. Send ISO to Switch
 */
@Service
public class NpciReqPayService {

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
            System.err.println("NpciReqPayService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(String xml) {
        String msgId = xmlParsingService.extractMsgId(xml);
        String txnId = xmlParsingService.extractTxnId(xml);

        // 1. Audit incoming XML
        auditService.saveRaw(msgId, "NPCI_REQPAY_XML_IN", xml);

        // 2. Create transaction record
        TransactionEntity txn = transactionService.createRequest(txnId != null ? txnId : msgId, xml);

        System.out.println("=== Processing NPCI ReqPay ===");
        System.out.println("MsgId: " + msgId);
        System.out.println("TxnId: " + txnId);

        // 3. Convert XML to ISO 0200
        ISOMsg iso = xmlToIsoConverter.convertReqPay(xml);

        // 4. Set DE fields in transaction for tracking
        try {
            if (iso.hasField(11)) txn.setDe11(iso.getString(11));
            if (iso.hasField(37)) txn.setDe37(iso.getString(37));
            if (iso.hasField(12)) txn.setDe12(iso.getString(12));
            if (iso.hasField(13)) txn.setDe13(iso.getString(13));
        } catch (Exception e) {
            System.err.println("Error setting DE fields: " + e.getMessage());
        }

        // 5. Audit ISO message
        auditService.saveParsed(msgId, "SWITCH_REQPAY_ISO_OUT", iso);

        System.out.println("=== ISO Message Built ===");
        printIso(iso);

        // 6. Mark ISO sent and send to Switch
        transactionService.markIsoSent(txn);
        byte[] response = switchClient.sendReqPay(iso);

        if (response == null) {
            System.out.println("=== No Response from Switch ===");
            transactionService.markFailure(txn, null);
        }
        // Audit: only 4 entries per flow. SWITCH_RESPPAY_ISO_IN and NPCI_RESPPAY_XML_OUT are logged in SwitchRespPayService when Switch posts response to /resppay
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
