package com.hitachi.imps.service.valadd.respvaladd;

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
 * Service for handling RespValAdd responses from Switch.
 * 
 * Flow: Switch (ISO) → App → Update transaction (DE120/DE39) → NPCI (XML)
 */
@Service
public class SwitchRespValAddService {

    @Autowired private IsoToXmlConverter isoToXmlConverter;
    @Autowired private NpciMockClient npciMockClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private TransactionService transactionService;

    @Async
    public void processAsync(byte[] isoBytes) {
        try {
            process(isoBytes);
        } catch (Exception e) {
            System.err.println("SwitchRespValAddService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(byte[] isoBytes) {
        // 1. Unpack ISO to extract DE120 (original txnId), DE39 (response code), DE38 (approval)
        ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
        String origTxnId = null;
        String respCode = null;
        String approvalNum = null;
        try {
            if (iso.hasField(120)) {
                origTxnId = iso.getString(120);
            }
            if (iso.hasField(39)) respCode = iso.getString(39);
            if (iso.hasField(38)) approvalNum = iso.getString(38);
        } catch (Exception e) {
            System.err.println("Error extracting RespValAdd ISO fields: " + e.getMessage());
            e.printStackTrace();
        }
        final String originalTxnId = origTxnId;
        final String responseCode = respCode;
        final String approvalNumber = approvalNum;
        String txnId = (originalTxnId != null && !originalTxnId.isBlank())
                ? originalTxnId
                : "SWITCH_VALADD_RESP_" + System.currentTimeMillis();

        // 2. Audit incoming ISO (as Base64 for binary safety)
        auditService.saveRawBytes(txnId, "SWITCH_RESPVALADD_ISO_IN", isoBytes);

        System.out.println("=== Processing Switch RespValAdd ===");
        System.out.println("ISO bytes length: " + isoBytes.length);
        System.out.println("DE120 present: " + (iso.hasField(120)));
        System.out.println("Original TxnId (DE120): " + originalTxnId);
        System.out.println("Response Code (DE39): " + responseCode);
        if (originalTxnId == null || originalTxnId.isBlank()) {
            System.err.println("=== WARNING: DE120 is null/blank — cannot update transaction row ===");
        }

        // 3. Convert ISO to XML first (so we can store it in transaction)
        String xml = isoToXmlConverter.convertRespValAddToXml(isoBytes);

        // 4. Update transaction status if we have the original txnId (store resp_xml and resp_out_date_time)
        try {
            if (originalTxnId != null && !originalTxnId.isBlank()) {
                Optional<TransactionEntity> opt = transactionService.findOptionalByTxnId(originalTxnId);
                // Fallback: old rows may have txn_id = msgId; DE120 is Txn @id — find by Txn id in req_xml
                if (opt.isEmpty()) {
                    opt = transactionService.findOptionalValAddIsoSentByTxnIdInReqXml(originalTxnId);
                    if (opt.isPresent()) {
                        System.out.println("=== ValAdd transaction found by Txn id in req_xml (fallback for legacy row) ===");
                    }
                }
                if (opt.isPresent()) {
                    TransactionEntity txn = opt.get();
                    System.out.println("=== Updating transaction id=" + txn.getId() + " txn_id=" + txn.getTxnId() + " to SUCCESS/FAILED ===");
                    if ("00".equals(responseCode)) {
                        transactionService.markSuccess(txn, xml, approvalNumber, null);
                        System.out.println("=== ValAdd transaction marked SUCCESS ===");
                    } else {
                        transactionService.markFailure(txn, xml);
                        System.out.println("=== ValAdd transaction marked FAILED (code: " + responseCode + ") ===");
                    }
                } else {
                    System.err.println("=== WARNING: ValAdd response DE120=" + originalTxnId + " — no matching transaction (by txn_id or req_xml). Is Mock Switch sending responses to this IMPS backend? ===");
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating ValAdd transaction status: " + e.getMessage());
            e.printStackTrace();
        }

        // 5. Audit XML message
        auditService.saveRaw(txnId, "NPCI_RESPVALADD_XML_OUT", xml);

        System.out.println("=== XML Response Message Built ===");
        System.out.println(xml);

        // 6. Send XML to NPCI Mock Client (optional - won't fail if not running)
        try {
            String response = npciMockClient.sendRespValAdd(xml);
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
