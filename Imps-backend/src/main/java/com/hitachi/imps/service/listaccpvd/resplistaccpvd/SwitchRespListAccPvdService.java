package com.hitachi.imps.service.listaccpvd.resplistaccpvd;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.NpciMockClient;
import com.hitachi.imps.converter.IsoToXmlConverter;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.util.IsoUtil;

/**
 * Service for handling RespListAccPvd responses from Switch.
 * 
 * Flow: Switch (ISO) → App → NPCI (XML)
 */
@Service
public class SwitchRespListAccPvdService {

    @Autowired private IsoToXmlConverter isoToXmlConverter;
    @Autowired private NpciMockClient npciMockClient;
    @Autowired private MessageAuditService auditService;

    @Async
    public void processAsync(byte[] isoBytes) {
        try {
            process(isoBytes);
        } catch (Exception e) {
            System.err.println("SwitchRespListAccPvdService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(byte[] isoBytes) {
        String txnId = "SWITCH_LISTACCPVD_RESP_" + System.currentTimeMillis();

        // 1. Unpack ISO and audit in same format as SWITCH_*_ISO_OUT (MTI + DE fields, not Base64)
        ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
        auditService.saveParsed(txnId, "SWITCH_RESPLISTACCPVD_ISO_IN", iso);

        System.out.println("=== Processing Switch RespListAccPvd ===");
        System.out.println("ISO bytes length: " + isoBytes.length);

        // 2. Convert ISO to XML
        String xml = isoToXmlConverter.convertRespListAccPvdToXml(isoBytes);

        // 3. Audit XML message
        auditService.saveRaw(txnId, "NPCI_RESPLISTACCPVD_XML_OUT", xml);

        System.out.println("=== XML Response Built ===");
        System.out.println(xml);

        // 4. Send XML to NPCI Mock Client (optional - won't fail if not running)
        try {
            String response = npciMockClient.sendRespListAccPvd(xml);
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
