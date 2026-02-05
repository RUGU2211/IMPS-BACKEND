package com.hitachi.imps.service.listaccpvd.resplistaccpvd;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.NpciMockClient;
import com.hitachi.imps.converter.IsoToXmlConverter;
import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.util.IsoUtil;

import java.util.Optional;

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
    @Autowired private TransactionService transactionService;

    @Async
    public void processAsync(byte[] isoBytes) {
        try {
            process(isoBytes, null);
        } catch (Exception e) {
            System.err.println("SwitchRespListAccPvdService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void processAsync(byte[] isoBytes, String pathTxnId) {
        try {
            process(isoBytes, pathTxnId);
        } catch (Exception e) {
            System.err.println("SwitchRespListAccPvdService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(byte[] isoBytes) {
        process(isoBytes, null);
    }

    public void process(byte[] isoBytes, String pathTxnId) {
        String txnId = (pathTxnId != null && !pathTxnId.isBlank())
            ? pathTxnId
            : "SWITCH_LISTACCPVD_RESP_" + System.currentTimeMillis();

        // 1. Unpack ISO and audit in same format as SWITCH_*_ISO_OUT (MTI + DE fields, not Base64)
        ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
        auditService.saveParsed(txnId, "SWITCH_RESPLISTACCPVD_ISO_IN", iso);

        System.out.println("=== Processing Switch RespListAccPvd ===");
        System.out.println("ISO bytes length: " + isoBytes.length);

        // 2. Convert ISO to XML
        String xml = isoToXmlConverter.convertRespListAccPvdToXml(isoBytes);

        // 3. Update transaction status when pathTxnId is present (same id used when sending to switch)
        if (pathTxnId != null && !pathTxnId.isBlank()) {
            try {
                Optional<TransactionEntity> opt = transactionService.findOptionalByTxnId(pathTxnId);
                opt.ifPresent(txn -> {
                    transactionService.markSuccess(txn, xml, null, null);
                    System.out.println("=== ListAccPvd transaction marked SUCCESS ===");
                });
            } catch (Exception e) {
                System.err.println("Error updating ListAccPvd transaction status: " + e.getMessage());
            }
        }

        // 4. Audit XML message
        auditService.saveRaw(txnId, "NPCI_RESPLISTACCPVD_XML_OUT", xml);

        System.out.println("=== XML Response Built ===");
        System.out.println(xml);

        // 5. Send XML to NPCI Mock Client (dynamic URL /npci/resplistaccpvd/{txnId})
        try {
            if (pathTxnId != null && !pathTxnId.isBlank())
                System.out.println("resplistaccpvd/" + pathTxnId + " send to npci");
            String response = (pathTxnId != null && !pathTxnId.isBlank())
                ? npciMockClient.sendRespListAccPvd(xml, pathTxnId)
                : npciMockClient.sendRespListAccPvd(xml);
            if (response != null && pathTxnId != null && !pathTxnId.isBlank())
                System.out.println("npci ack receive for resplistaccpvd/" + pathTxnId);
            if (response == null && pathTxnId != null)
                System.out.println("=== WARNING: NPCI Mock Client not available (port 8083) - Continuing without ACK ===");
        } catch (Exception e) {
            System.out.println("=== WARNING: NPCI Mock Client not available - Continuing without ACK ===");
            System.out.println("   Error: " + e.getMessage());
        }
    }
}
