package com.hitachi.imps.service.heartbeat.resphbt;

import java.util.Optional;

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

/**
 * Service for handling RespHbt responses from Switch.
 * 
 * Flow: Switch (ISO) → App → Update transaction (DE120/DE39) → NPCI (XML)
 * 1. Receive ISO response from Switch
 * 2. Extract original txnId (DE120) and response code (DE39)
 * 3. Update transaction status in database
 * 4. Convert ISO to XML
 * 5. Send XML to NPCI
 */
@Service
public class SwitchRespHbtService {

    @Autowired private IsoToXmlConverter isoToXmlConverter;
    @Autowired private NpciMockClient npciMockClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private TransactionService transactionService;

    @Async
    public void processAsync(byte[] isoBytes) {
        try {
            process(isoBytes);
        } catch (Exception e) {
            System.err.println("SwitchRespHbtService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(byte[] isoBytes) {
        // 1. Unpack ISO to extract DE120 (original txnId), DE39 (response code)
        ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
        String origTxnId = null;
        String respCode = null;
        try {
            origTxnId = iso.getString(120);
            respCode = iso.getString(39);
        } catch (Exception e) {
            System.err.println("Error extracting RespHbt ISO fields: " + e.getMessage());
        }
        final String originalTxnId = origTxnId;
        final String responseCode = respCode;
        String txnId = (originalTxnId != null && !originalTxnId.isBlank())
                ? originalTxnId
                : "SWITCH_HBT_RESP_" + System.currentTimeMillis();

        // 2. Audit incoming ISO (as Base64 for binary safety)
        auditService.saveRawBytes(txnId, "SWITCH_RESPHBT_ISO_IN", isoBytes);

        System.out.println("=== Processing Switch RespHbt ===");
        System.out.println("ISO bytes length: " + isoBytes.length);
        System.out.println("Original TxnId (DE120): " + originalTxnId);
        System.out.println("Response Code (DE39): " + responseCode);

        // 3. Convert ISO to XML first (so we can store it in transaction)
        String xml = isoToXmlConverter.convertRespHbtToXml(isoBytes);

        // 4. Update transaction status if we have the original txnId (store resp_xml and resp_out_date_time)
        try {
            if (originalTxnId != null && !originalTxnId.isBlank()) {
                Optional<TransactionEntity> opt = transactionService.findOptionalByTxnId(originalTxnId);
                opt.ifPresent(txn -> {
                    if ("00".equals(responseCode)) {
                        transactionService.markSuccess(txn, xml, null, null);
                        System.out.println("=== Heartbeat transaction marked SUCCESS ===");
                    } else {
                        transactionService.markFailure(txn, xml);
                        System.out.println("=== Heartbeat transaction marked FAILED (code: " + responseCode + ") ===");
                    }
                });
            } else {
                System.out.println("=== WARNING: No original txnId found in DE120, cannot update transaction ===");
            }
        } catch (Exception e) {
            System.err.println("Error updating Heartbeat transaction status: " + e.getMessage());
        }

        // 5. Audit XML message
        auditService.saveRaw(txnId, "NPCI_RESPHBT_XML_OUT", xml);

        System.out.println("=== XML Heartbeat Response Built ===");
        System.out.println(xml);

        // 6. Send XML to NPCI Mock Client (optional - won't fail if not running)
        try {
            String response = npciMockClient.sendRespHbt(xml);
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
