package com.hitachi.imps.service.chktxn.reqchktxn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.NpciMockClient;
import com.hitachi.imps.converter.IsoToXmlConverter;
import com.hitachi.imps.service.audit.MessageAuditService;

/**
 * Service for handling ReqChkTxn requests from Switch.
 * 
 * Flow: Switch (ISO) → App → NPCI (XML)
 * 1. Receive ISO from Switch
 * 2. Convert ISO to XML
 * 3. Send XML to NPCI
 */
@Service
public class SwitchReqChkTxnService {

    @Autowired private IsoToXmlConverter isoToXmlConverter;
    @Autowired private NpciMockClient npciMockClient;
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
            System.err.println("SwitchReqChkTxnService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(byte[] isoBytes) {
        process(isoBytes, null);
    }

    public void process(byte[] isoBytes, String pathTxnId) {
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : "SWITCH_CHKTXN_REQ_" + System.currentTimeMillis();

        // 1. Audit incoming ISO (as Base64 for binary safety)
        auditService.saveRawBytes(txnId, "SWITCH_REQCHKTXN_ISO_IN", isoBytes);

        System.out.println("=== Processing Switch ReqChkTxn ===");
        System.out.println("ISO bytes length: " + isoBytes.length);

        // 2. Convert ISO to XML
        String xml = isoToXmlConverter.convertReqChkTxnToXml(isoBytes);

        // 3. Audit XML message
        auditService.saveRaw(txnId, "NPCI_REQCHKTXN_XML_OUT", xml);

        System.out.println("=== XML Message Built ===");
        System.out.println(xml);

        // 4. Send XML to NPCI Mock Client (dynamic URL with txnId)
        try {
            String response = (txnId != null && !txnId.isBlank()) ? npciMockClient.sendReqChkTxn(xml, txnId) : npciMockClient.sendReqChkTxn(xml);
            if (response != null) {
                System.out.println("=== NPCI MOCK CLIENT ACK Received ===");
                System.out.println(response);
            } else {
                System.out.println("=== WARNING: NPCI Mock Client not available (port 8083) - Continuing without ACK ===");
            }
        } catch (Exception e) {
            System.out.println("=== WARNING: NPCI Mock Client not available - Continuing without ACK ===");
            System.out.println("   Error: " + e.getMessage());
        }
    }
}
