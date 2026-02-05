package com.hitachi.imps.service.valadd.respvaladd;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.SwitchClient;
import com.hitachi.imps.converter.XmlToIsoConverter;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.XmlParsingService;

/**
 * Service for handling RespValAdd responses from NPCI.
 * 
 * Flow: NPCI (XML) → App → Switch (ISO)
 */
@Service
public class NpciRespValAddService {

    @Autowired private XmlToIsoConverter xmlToIsoConverter;
    @Autowired private SwitchClient switchClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;

    @Async
    public void processAsync(String xml) {
        try {
            process(xml);
        } catch (Exception e) {
            System.err.println("NpciRespValAddService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void processAsync(String xml, String pathTxnId) {
        try {
            process(xml, pathTxnId);
        } catch (Exception e) {
            System.err.println("NpciRespValAddService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(String xml) {
        process(xml, null);
    }

    public void process(String xml, String pathTxnId) {
        String msgId = xmlParsingService.extractMsgId(xml);
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : xmlParsingService.extractTxnId(xml);
        if (txnId == null || txnId.isBlank()) txnId = msgId;

        // 1. Audit incoming XML
        auditService.saveRaw(msgId, "NPCI_RESPVALADD_XML_IN", xml);

        System.out.println("=== Processing NPCI RespValAdd ===");
        System.out.println("MsgId: " + msgId + ", TxnId: " + txnId);

        // 2. Convert XML to ISO 0210
        ISOMsg iso = xmlToIsoConverter.convertRespValAdd(xml);

        // 3. Audit ISO message
        auditService.saveParsed(msgId, "SWITCH_RESPVALADD_ISO_OUT", iso);

        System.out.println("=== ISO Response Message Built ===");
        printIso(iso);

        // 4. Send ISO to Switch: POST /switch/respvaladd/{txnId}
        byte[] response = switchClient.sendRespValAdd(iso, txnId);

        if (response != null) {
            System.out.println("=== Switch ACK Received ===");
        } else {
            System.out.println("=== No ACK from Switch ===");
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
