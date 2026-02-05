package com.hitachi.imps.service.pay.resppay;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.NpciMockClient;
import com.hitachi.imps.converter.IsoToXmlConverter;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.TransactionValidationService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.util.IsoUtil;

/**
 * Service for handling RespPay responses from Switch.
 * 
 * Flow: Switch (ISO) → App → NPCI (XML)
 * 1. Receive ISO response from Switch
 * 2. Extract original txnId (DE120) and response code (DE39)
 * 3. Update transaction status in database
 * 4. Convert ISO to XML
 * 5. Send XML to NPCI
 */
@Service
public class SwitchRespPayService {

    @Autowired private IsoToXmlConverter isoToXmlConverter;
    @Autowired private NpciMockClient npciMockClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private TransactionService transactionService;
    @Autowired private TransactionValidationService validationService;

    @Async
    public void processAsync(byte[] isoBytes) {
        try {
            process(isoBytes, null);
        } catch (Exception e) {
            System.err.println("SwitchRespPayService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void processAsync(byte[] isoBytes, String pathTxnId) {
        try {
            process(isoBytes, pathTxnId);
        } catch (Exception e) {
            System.err.println("SwitchRespPayService ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(byte[] isoBytes) {
        process(isoBytes, null);
    }

    public void process(byte[] isoBytes, String pathTxnId) {
        // 1. Unpack ISO to extract fields
        ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
        
        String origTxnId = null;
        String respCode = null;
        String approvalNum = null;
        String rrn = null;
        try {
            origTxnId = iso.getString(120);  // Original Transaction ID
            respCode = iso.getString(39);    // Response Code (00 = success)
            approvalNum = iso.getString(38);  // Approval Number
            rrn = iso.getString(37);             // RRN
        } catch (Exception e) {
            System.err.println("Error extracting ISO fields: " + e.getMessage());
        }
        final String originalTxnId = origTxnId;
        final String responseCode = respCode;
        final String approvalNumber = approvalNum;
        
        // Use original txnId if available, otherwise generate one
        String txnId = (originalTxnId != null && !originalTxnId.isBlank()) 
                ? originalTxnId 
                : "SWITCH_RESP_" + System.currentTimeMillis();

        // 2. Audit incoming ISO in same parsed format as SWITCH_*_ISO_OUT (MTI + DE fields, not Base64)
        auditService.saveParsed(txnId, "SWITCH_RESPPAY_ISO_IN", iso);

        System.out.println("=== Processing Switch RespPay ===");
        System.out.println("ISO bytes length: " + isoBytes.length);
        System.out.println("Original TxnId (DE120): " + originalTxnId);
        System.out.println("Response Code (DE39): " + responseCode);
        System.out.println("Approval Number (DE38): " + approvalNumber);
        System.out.println("RRN (DE37): " + rrn);

        // 3. Convert ISO to XML first (so we can store it in transaction)
        String xml = isoToXmlConverter.convertRespPayToXml(isoBytes);

        // 4. Update transaction status in database (with resp_xml and resp_out_date_time)
        // Use pathTxnId from callback URL first (same id IMPS used when sending to switch); fallback to DE120
        String lookupId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : originalTxnId;
        try {
            if (lookupId != null && !lookupId.isBlank()) {
                transactionService.findOptionalByTxnId(lookupId).ifPresent(txn -> {
                    // Perform transaction validation
                    TransactionValidationService.ValidationResult validationResult =
                        validationService.validateTransaction(iso, txn);
                    if (!validationResult.isValid()) {
                        System.out.println("=== VALIDATION WARNINGS ===");
                        validationResult.getValidations().forEach((k, v) ->
                            System.out.println("  " + k + ": " + v));
                    }
                    // Update transaction status and save response XML
                    if ("00".equals(responseCode)) {
                        transactionService.markSuccess(txn, xml, approvalNumber, null);
                        System.out.println("=== Transaction marked SUCCESS ===");
                    } else {
                        transactionService.markFailure(txn, xml);
                        System.out.println("=== Transaction marked FAILED (code: " + responseCode + ") ===");
                    }
                });
            } else {
                System.out.println("=== WARNING: No txnId (path or DE120), cannot update transaction ===");
            }
        } catch (Exception e) {
            System.err.println("Error updating transaction status: " + e.getMessage());
            e.printStackTrace();
        }

        // 5. Audit XML message
        auditService.saveRaw(txnId, "NPCI_RESPPAY_XML_OUT", xml);

        System.out.println("=== XML Response Message Built ===");
        System.out.println(xml);

        // 6. Send XML to NPCI Mock Client (dynamic URL /npci/resppay/{txnId})
        String txnIdForNpci = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : originalTxnId;
        try {
            if (txnIdForNpci != null && !txnIdForNpci.isBlank())
                System.out.println("resppay/" + txnIdForNpci + " send to npci");
            String response = (txnIdForNpci != null && !txnIdForNpci.isBlank())
                ? npciMockClient.sendRespPay(xml, txnIdForNpci)
                : npciMockClient.sendRespPay(xml);
            if (response != null && txnIdForNpci != null && !txnIdForNpci.isBlank())
                System.out.println("npci ack receive for resppay/" + txnIdForNpci);
            if (response == null && txnIdForNpci != null)
                System.out.println("=== WARNING: NPCI Mock Client not available (port 8083) - Continuing without ACK ===");
        } catch (Exception e) {
            System.out.println("=== WARNING: NPCI Mock Client not available - Continuing without ACK ===");
            System.out.println("   Error: " + e.getMessage());
        }
    }
}
