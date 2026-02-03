package com.hitachi.mockswitch.service;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.hitachi.mockswitch.entity.AuditLog.ProcessingStatus;
import com.hitachi.mockswitch.iso.MockIsoPackager;
import com.hitachi.mockswitch.service.AccountLedgerService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Mock Response Service
 * 
 * Generates mock ISO 8583 responses and sends them back to IMPS Backend.
 */
@Service
public class MockResponseService {

    @Value("${imps.backend.base-url}")
    private String impsBackendUrl;

    @Value("${mock.response-delay-ms:500}")
    private int responseDelayMs;

    @Value("${mock.default-result:SUCCESS}")
    private String defaultResult;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AccountLedgerService accountLedgerService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final MockIsoPackager packager = new MockIsoPackager();
    private final Random random = new Random();

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMdd");

    /* ===============================
       LOG ISO MESSAGE
       =============================== */
    public ISOMsg logIsoMessage(byte[] isoBytes, String type) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(packager);
            iso.unpack(isoBytes);

            System.out.println("--- Parsed ISO " + type + " ---");
            System.out.println("MTI: " + iso.getMTI());
            
            for (int i = 2; i <= 128; i++) {
                if (iso.hasField(i)) {
                    System.out.println("DE" + i + ": " + iso.getString(i));
                }
            }
            System.out.println("--- End ISO ---");

            return iso;

        } catch (ISOException e) {
            System.err.println("Failed to parse ISO: " + e.getMessage());
            return null;
        }
    }

    /* ===============================
       SEND RESPPAY
       =============================== */
    @Async
    public void sendRespPayAsync(byte[] reqIsoBytes, Long inboundAuditId) {
        try {
            // Simulate processing delay
            Thread.sleep(responseDelayMs);

            // Parse request
            ISOMsg reqIso = unpack(reqIsoBytes);
            if (reqIso == null) return;

            // Debit payer and credit payee in account_master; get response code
            String responseCode = accountLedgerService.debitAndCredit(reqIso);

            // Build response
            ISOMsg respIso = new ISOMsg();
            respIso.setPackager(packager);
            respIso.setMTI("0210"); // Response MTI

            // Copy relevant fields from request
            copyField(reqIso, respIso, 3);   // Processing code
            copyField(reqIso, respIso, 4);   // Amount
            copyField(reqIso, respIso, 37);  // RRN
            copyField(reqIso, respIso, 41);  // Terminal ID
            copyField(reqIso, respIso, 49);  // Currency
            copyField(reqIso, respIso, 102); // Payer account
            copyField(reqIso, respIso, 103); // Payee account
            copyField(reqIso, respIso, 120); // Transaction ID

            // Add response fields
            respIso.set(11, generateStan());
            respIso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            respIso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            respIso.set(38, generateApprovalNumber()); // Approval number
            respIso.set(39, responseCode); // 00=success, 51=insufficient funds, 14=invalid account, 96=error

            // Send to IMPS Backend with audit
            sendToBackendWithAudit(respIso, "/switch/resppay/2.1", "RESPPAY", inboundAuditId);

        } catch (Exception e) {
            System.err.println("Error sending RespPay: " + e.getMessage());
            auditService.updateStatus(inboundAuditId, ProcessingStatus.FAILED, e.getMessage());
        }
    }

    /* ===============================
       SEND RESPCHKTXN
       =============================== */
    @Async
    public void sendRespChkTxnAsync(byte[] reqIsoBytes, Long inboundAuditId) {
        try {
            Thread.sleep(responseDelayMs);

            ISOMsg reqIso = unpack(reqIsoBytes);
            if (reqIso == null) return;

            ISOMsg respIso = new ISOMsg();
            respIso.setPackager(packager);
            respIso.setMTI("0210");

            copyField(reqIso, respIso, 3);
            copyField(reqIso, respIso, 37);
            copyField(reqIso, respIso, 41);
            copyField(reqIso, respIso, 48); // Original txn ref
            copyField(reqIso, respIso, 120); // ChkTxn txn id (for DB lookup)

            respIso.set(11, generateStan());
            respIso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            respIso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            respIso.set(38, generateApprovalNumber());
            respIso.set(39, "00"); // Transaction found, SUCCESS

            sendToBackendWithAudit(respIso, "/switch/respchktxn/2.1", "RESPCHKTXN", inboundAuditId);

        } catch (Exception e) {
            System.err.println("Error sending RespChkTxn: " + e.getMessage());
            auditService.updateStatus(inboundAuditId, ProcessingStatus.FAILED, e.getMessage());
        }
    }

    /* ===============================
       SEND RESPHBT
       =============================== */
    @Async
    public void sendRespHbtAsync(byte[] reqIsoBytes, Long inboundAuditId) {
        try {
            Thread.sleep(responseDelayMs);

            ISOMsg reqIso = unpack(reqIsoBytes);
            if (reqIso == null) return;

            ISOMsg respIso = new ISOMsg();
            respIso.setPackager(packager);
            respIso.setMTI("0810"); // Network management response

            copyField(reqIso, respIso, 3);
            copyField(reqIso, respIso, 24); // Function code
            copyField(reqIso, respIso, 37);
            copyField(reqIso, respIso, 41);
            copyField(reqIso, respIso, 120); // HBT txn id (for DB lookup)

            respIso.set(11, generateStan());
            respIso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            respIso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            respIso.set(39, "00"); // Heartbeat OK

            sendToBackendWithAudit(respIso, "/switch/resphbt/2.1", "RESPHBT", inboundAuditId);

        } catch (Exception e) {
            System.err.println("Error sending RespHbt: " + e.getMessage());
            auditService.updateStatus(inboundAuditId, ProcessingStatus.FAILED, e.getMessage());
        }
    }

    /* ===============================
       SEND RESPVALADD
       =============================== */
    @Async
    public void sendRespValAddAsync(byte[] reqIsoBytes, Long inboundAuditId) {
        try {
            Thread.sleep(responseDelayMs);

            ISOMsg reqIso = unpack(reqIsoBytes);
            if (reqIso == null) return;

            ISOMsg respIso = new ISOMsg();
            respIso.setPackager(packager);
            respIso.setMTI("0210");

            copyField(reqIso, respIso, 3);
            copyField(reqIso, respIso, 33); // IFSC
            copyField(reqIso, respIso, 37);
            copyField(reqIso, respIso, 41);
            copyField(reqIso, respIso, 102); // Account
            copyField(reqIso, respIso, 120); // Txn ID

            respIso.set(11, generateStan());
            respIso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            respIso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            respIso.set(38, generateApprovalNumber());
            respIso.set(39, "00"); // Account valid

            // Add account holder name in additional data
            respIso.set(48, "ACCOUNT_HOLDER_NAME");

            sendToBackendWithAudit(respIso, "/switch/respvaladd/2.1", "RESPVALADD", inboundAuditId);

        } catch (Exception e) {
            System.err.println("Error sending RespValAdd: " + e.getMessage());
            auditService.updateStatus(inboundAuditId, ProcessingStatus.FAILED, e.getMessage());
        }
    }

    /* ===============================
       SEND RESPLISTACCPVD
       =============================== */
    @Async
    public void sendRespListAccPvdAsync(byte[] reqIsoBytes, Long inboundAuditId) {
        try {
            Thread.sleep(responseDelayMs);

            ISOMsg reqIso = unpack(reqIsoBytes);
            if (reqIso == null) return;

            ISOMsg respIso = new ISOMsg();
            respIso.setPackager(packager);
            respIso.setMTI("0210");

            copyField(reqIso, respIso, 3);
            copyField(reqIso, respIso, 37);
            copyField(reqIso, respIso, 41);
            copyField(reqIso, respIso, 120); // Txn ID (for IMPS transaction update)

            respIso.set(11, generateStan());
            respIso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            respIso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            respIso.set(39, "00");

            // Add mock bank list in additional data
            respIso.set(48, "HDFC|ICICI|SBI|AXIS");

            sendToBackendWithAudit(respIso, "/switch/resplistaccpvd/2.1", "RESPLISTACCPVD", inboundAuditId);

        } catch (Exception e) {
            System.err.println("Error sending RespListAccPvd: " + e.getMessage());
            auditService.updateStatus(inboundAuditId, ProcessingStatus.FAILED, e.getMessage());
        }
    }

    /* ===============================
       HELPER METHODS
       =============================== */
    private ISOMsg unpack(byte[] isoBytes) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(packager);
            iso.unpack(isoBytes);
            return iso;
        } catch (ISOException e) {
            System.err.println("Failed to unpack ISO: " + e.getMessage());
            return null;
        }
    }

    private void copyField(ISOMsg src, ISOMsg dest, int field) throws ISOException {
        if (src.hasField(field)) {
            dest.set(field, src.getString(field));
        }
    }

    private void sendToBackend(ISOMsg iso, String endpoint, String type) {
        sendToBackendWithAudit(iso, endpoint, type, null);
    }

    private void sendToBackendWithAudit(ISOMsg iso, String endpoint, String type, Long inboundAuditId) {
        byte[] packed = null;
        try {
            packed = iso.pack();

            System.out.println("\n===========================================");
            System.out.println("  MOCK SWITCH: SENDING " + type);
            System.out.println("  To: " + impsBackendUrl + endpoint);
            System.out.println("  ISO Length: " + packed.length + " bytes");
            System.out.println("===========================================");
            System.out.println("--- Response ISO ---");
            System.out.println("MTI: " + iso.getMTI());
            for (int i = 2; i <= 128; i++) {
                if (iso.hasField(i)) {
                    System.out.println("DE" + i + ": " + iso.getString(i));
                }
            }
            System.out.println("--- End ISO ---");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> request = new HttpEntity<>(packed, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                impsBackendUrl + endpoint,
                HttpMethod.POST,
                request,
                String.class
            );

            System.out.println("=== IMPS Backend Response ===");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            System.out.println("=============================\n");

            // Audit log - outbound response
            auditService.logOutboundResponse(packed, iso, type, 
                    impsBackendUrl + endpoint, 
                    response.getStatusCode().value(),
                    ProcessingStatus.SENT, null);

            // Update inbound audit status to SUCCESS
            if (inboundAuditId != null) {
                auditService.updateStatus(inboundAuditId, ProcessingStatus.SUCCESS, null);
            }

        } catch (Exception e) {
            System.err.println("Failed to send to IMPS Backend: " + e.getMessage());
            
            // Audit log - send failed
            auditService.logOutboundResponse(packed, iso, type,
                    impsBackendUrl + endpoint, null,
                    ProcessingStatus.SEND_FAILED, e.getMessage());

            // Update inbound audit status to FAILED
            if (inboundAuditId != null) {
                auditService.updateStatus(inboundAuditId, ProcessingStatus.FAILED, 
                        "Failed to send response: " + e.getMessage());
            }
        }
    }

    private String generateStan() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String generateApprovalNumber() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    /**
     * Forward raw ISO bytes to IMPS Backend (e.g. when Mock Switch received XML and converted to ISO).
     */
    public void forwardIsoToBackend(byte[] isoBytes, String endpoint, String type) {
        ISOMsg iso = unpack(isoBytes);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            HttpEntity<byte[]> request = new HttpEntity<>(isoBytes, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                impsBackendUrl + endpoint,
                HttpMethod.POST,
                request,
                String.class
            );
            System.out.println("=== MOCK SWITCH: FORWARDED " + type + " TO IMPS BACKEND ===");
            System.out.println("Status: " + response.getStatusCode());
            auditService.logOutboundResponse(isoBytes, iso, type,
                impsBackendUrl + endpoint, response.getStatusCode().value(), ProcessingStatus.SENT, null);
        } catch (Exception e) {
            System.err.println("Forward to IMPS Backend failed: " + e.getMessage());
            auditService.logOutboundResponse(isoBytes, iso, type,
                impsBackendUrl + endpoint, null, ProcessingStatus.SEND_FAILED, e.getMessage());
        }
    }
}
