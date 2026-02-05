package com.hitachi.imps.service.listaccpvd.resplistaccpvd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.XmlParsingService;

/**
 * Service for handling RespListAccPvd responses from NPCI.
 * 
 * Flow: NPCI (XML) → App → Log/Process
 * Note: RespListAccPvd from NPCI is typically just logged/acknowledged
 */
@Service
public class NpciRespListAccPvdService {

    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;

    @Async
    public void processAsync(String xml) {
        try {
            process(xml);
        } catch (Exception e) {
            System.err.println("NpciRespListAccPvdService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void processAsync(String xml, String pathTxnId) {
        try {
            process(xml);
        } catch (Exception e) {
            System.err.println("NpciRespListAccPvdService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(String xml) {
        String msgId = xmlParsingService.extractMsgId(xml);

        // 1. Audit incoming XML
        auditService.saveRaw(msgId, "NPCI_RESPLISTACCPVD_XML_IN", xml);

        System.out.println("=== Processing NPCI RespListAccPvd ===");
        System.out.println("MsgId: " + msgId);
        System.out.println(xml);

        // 2. RespListAccPvd is typically terminal - just log it
        System.out.println("=== RespListAccPvd Processed Successfully ===");
    }
}
