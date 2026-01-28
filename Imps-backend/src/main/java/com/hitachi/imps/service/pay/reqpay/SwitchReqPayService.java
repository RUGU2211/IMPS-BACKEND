package com.hitachi.imps.service.pay.reqpay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.NpciMockClient;
import com.hitachi.imps.converter.IsoToXmlConverter;
import com.hitachi.imps.service.audit.MessageAuditService;

/**
 * Service for handling ReqPay requests from Switch.
 * 
 * Flow: Switch (ISO) → App → NPCI (XML)
 * 1. Receive ISO from Switch
 * 2. Convert ISO to XML
 * 3. Send XML to NPCI
 */
@Service
public class SwitchReqPayService {

    @Autowired private IsoToXmlConverter isoToXmlConverter;
    @Autowired private NpciMockClient npciMockClient;
    @Autowired private MessageAuditService auditService;

    @Async
    public void processAsync(byte[] isoBytes) {
        try {
            process(isoBytes);
        } catch (Exception e) {
            System.err.println("SwitchReqPayService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(byte[] isoBytes) {
        String txnId = "SWITCH_REQ_" + System.currentTimeMillis();

        // 1. Audit incoming ISO (as Base64 for binary safety)
        auditService.saveRawBytes(txnId, "SWITCH_REQPAY_ISO_IN", isoBytes);

        System.out.println("=== Processing Switch ReqPay ===");
        System.out.println("ISO bytes length: " + isoBytes.length);

        // 2. Convert ISO to XML
        String xml = isoToXmlConverter.convertReqPayToXml(isoBytes);

        // 3. Audit XML message
        auditService.saveRaw(txnId, "NPCI_REQPAY_XML_OUT", xml);

        System.out.println("=== XML Message Built ===");
        System.out.println(xml);

        // 4. Send XML to NPCI Mock Client (optional - won't fail if not running)
        try {
            String response = npciMockClient.sendReqPay(xml);
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
