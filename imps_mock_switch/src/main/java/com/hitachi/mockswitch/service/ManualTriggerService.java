package com.hitachi.mockswitch.service;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.hitachi.mockswitch.dto.ManualRespPayRequest;
import com.hitachi.mockswitch.dto.ManualTriggerResponse;
import com.hitachi.mockswitch.entity.AuditLog.ProcessingStatus;
import com.hitachi.mockswitch.iso.MockIsoPackager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Service for manually triggering ISO 8583 messages to IMPS Backend.
 * 
 * This simulates Switch-initiated transactions for testing purposes.
 * 
 * Flow: Mock Switch (ISO) → IMPS Backend → NPCI (XML)
 */
@Service
public class ManualTriggerService {

    @Value("${imps.backend.base-url}")
    private String impsBackendUrl;

    @Autowired
    private AuditService auditService;

    /** For manual mode: when txnId not provided, use last received request DE120 so behaviour matches auto. */
    @Autowired
    private LastRequestContext lastRequestContext;

    private final RestTemplate restTemplate = new RestTemplate();
    private final MockIsoPackager packager = new MockIsoPackager();
    private final Random random = new Random();

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMdd");

    /**
     * Manually trigger a RespPay (0210) to IMPS Backend.
     * This simulates the beneficiary bank sending a credit response.
     */
    public ManualTriggerResponse sendRespPay(ManualRespPayRequest request) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(packager);
            iso.setMTI("0210");

            // Set fields from request
            String processingCode = request.getProcessingCode() != null ? request.getProcessingCode() : "400000";
            iso.set(3, processingCode);
            
            // Amount - format to 12 digits (cents)
            String amount = formatAmount(request.getAmount());
            iso.set(4, amount);
            
            // Generate STAN
            String stan = generateStan();
            iso.set(11, stan);
            
            // Date/Time
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            
            // RRN (from request or generate)
            String rrn = request.getRrn() != null && !request.getRrn().isEmpty() 
                    ? request.getRrn() 
                    : generateRrn();
            iso.set(37, rrn);
            
            // Approval Number
            String approvalNum = request.getApprovalNumber() != null && !request.getApprovalNumber().isEmpty()
                    ? request.getApprovalNumber()
                    : generateApprovalNumber();
            iso.set(38, approvalNum);
            
            // Response Code (00 = success)
            String respCode = request.getResponseCode() != null && !request.getResponseCode().isEmpty()
                    ? request.getResponseCode()
                    : "00";
            iso.set(39, respCode);
            
            // Terminal ID
            iso.set(41, "IMPSTERM");
            
            // Currency
            String currency = request.getCurrency() != null ? request.getCurrency() : "356";
            iso.set(49, currency);
            
            // Accounts
            if (request.getPayerAccount() != null) {
                iso.set(102, request.getPayerAccount());
            }
            if (request.getPayeeAccount() != null) {
                iso.set(103, request.getPayeeAccount());
            }
            
            // Transaction ID (DE120): from request or last ReqPay DE120 so manual matches auto
            String txnId = request.getTxnId();
            if (txnId == null || txnId.isEmpty()) txnId = lastRequestContext.getLastReqPayDe120();
            if (txnId != null && !txnId.isEmpty()) iso.set(120, txnId);

            // Pack and send
            byte[] packed = iso.pack();
            
            // Log
            System.out.println("\n===========================================");
            System.out.println("  MANUAL TRIGGER: SENDING RESPPAY TO IMPS");
            System.out.println("  To: " + impsBackendUrl + "/switch/resppay/2.1");
            System.out.println("  ISO Length: " + packed.length + " bytes");
            System.out.println("===========================================");
            printIso(iso, "MANUAL RESPPAY");

            // Send to IMPS Backend
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> httpRequest = new HttpEntity<>(packed, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                impsBackendUrl + "/switch/resppay/2.1",
                HttpMethod.POST,
                httpRequest,
                String.class
            );

            System.out.println("=== IMPS Backend Response ===");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            System.out.println("=============================\n");

            // Audit
            auditService.logOutboundResponse(packed, iso, "MANUAL_RESPPAY",
                    impsBackendUrl + "/switch/resppay/2.1",
                    response.getStatusCode().value(),
                    ProcessingStatus.SENT, null);

            // Build response
            ManualTriggerResponse resp = ManualTriggerResponse.success("RespPay sent successfully");
            resp.setRrn(rrn);
            resp.setStan(stan);
            resp.setApprovalNumber(approvalNum);
            resp.setIsoHex(bytesToHex(packed));
            resp.setBackendResponse(response.getBody());
            
            return resp;

        } catch (Exception e) {
            System.err.println("Error in manual trigger: " + e.getMessage());
            e.printStackTrace();
            return ManualTriggerResponse.error("Failed to send RespPay: " + e.getMessage());
        }
    }

    /**
     * Manually trigger a ReqPay (0200) to IMPS Backend.
     * This simulates a Switch-initiated fund transfer request.
     */
    public ManualTriggerResponse sendReqPay(ManualRespPayRequest request) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(packager);
            iso.setMTI("0200");

            String processingCode = request.getProcessingCode() != null ? request.getProcessingCode() : "400000";
            iso.set(3, processingCode);
            
            String amount = formatAmount(request.getAmount());
            iso.set(4, amount);
            
            String stan = generateStan();
            iso.set(11, stan);
            
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            
            String rrn = request.getRrn() != null && !request.getRrn().isEmpty() 
                    ? request.getRrn() 
                    : generateRrn();
            iso.set(37, rrn);
            
            iso.set(41, "IMPSTERM");
            
            String currency = request.getCurrency() != null ? request.getCurrency() : "356";
            iso.set(49, currency);
            
            if (request.getPayerAccount() != null) {
                iso.set(102, request.getPayerAccount());
            }
            if (request.getPayeeAccount() != null) {
                iso.set(103, request.getPayeeAccount());
            }
            
            if (request.getTxnId() != null) {
                iso.set(120, request.getTxnId());
            }

            byte[] packed = iso.pack();
            
            System.out.println("\n===========================================");
            System.out.println("  MANUAL TRIGGER: SENDING REQPAY TO IMPS");
            System.out.println("  To: " + impsBackendUrl + "/switch/reqpay/2.1");
            System.out.println("  ISO Length: " + packed.length + " bytes");
            System.out.println("===========================================");
            printIso(iso, "MANUAL REQPAY");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> httpRequest = new HttpEntity<>(packed, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                impsBackendUrl + "/switch/reqpay/2.1",
                HttpMethod.POST,
                httpRequest,
                String.class
            );

            System.out.println("=== IMPS Backend Response ===");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            System.out.println("=============================\n");

            auditService.logOutboundResponse(packed, iso, "MANUAL_REQPAY",
                    impsBackendUrl + "/switch/reqpay/2.1",
                    response.getStatusCode().value(),
                    ProcessingStatus.SENT, null);

            ManualTriggerResponse resp = ManualTriggerResponse.success("ReqPay sent successfully");
            resp.setRrn(rrn);
            resp.setStan(stan);
            resp.setIsoHex(bytesToHex(packed));
            resp.setBackendResponse(response.getBody());
            
            return resp;

        } catch (Exception e) {
            System.err.println("Error in manual trigger: " + e.getMessage());
            e.printStackTrace();
            return ManualTriggerResponse.error("Failed to send ReqPay: " + e.getMessage());
        }
    }

    /**
     * Manually trigger a RespChkTxn (0210) to IMPS Backend.
     */
    public ManualTriggerResponse sendRespChkTxn(ManualRespPayRequest request) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(packager);
            iso.setMTI("0210");

            iso.set(3, "380000"); // Check status processing code
            
            String stan = generateStan();
            iso.set(11, stan);
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            
            String rrn = request.getRrn() != null && !request.getRrn().isEmpty() 
                    ? request.getRrn() : generateRrn();
            iso.set(37, rrn);
            
            String approvalNum = request.getApprovalNumber() != null && !request.getApprovalNumber().isEmpty()
                    ? request.getApprovalNumber() : generateApprovalNumber();
            iso.set(38, approvalNum);
            
            String respCode = request.getResponseCode() != null && !request.getResponseCode().isEmpty()
                    ? request.getResponseCode() : "00";
            iso.set(39, respCode);
            
            iso.set(41, "IMPSTERM");
            
            // DE120 + DE48: from request or last ReqChkTxn DE120 so manual matches auto (IMPS looks up by DE120)
            String txnId = request.getTxnId();
            if (txnId == null || txnId.isEmpty()) txnId = lastRequestContext.getLastReqChkTxnDe120();
            if (txnId != null && !txnId.isEmpty()) {
                iso.set(48, txnId);
                iso.set(120, txnId);
            }

            byte[] packed = iso.pack();
            
            System.out.println("\n===========================================");
            System.out.println("  MANUAL TRIGGER: SENDING RESPCHKTXN TO IMPS");
            System.out.println("  To: " + impsBackendUrl + "/switch/respchktxn/2.1");
            System.out.println("===========================================");
            printIso(iso, "MANUAL RESPCHKTXN");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            HttpEntity<byte[]> httpRequest = new HttpEntity<>(packed, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                impsBackendUrl + "/switch/respchktxn/2.1",
                HttpMethod.POST, httpRequest, String.class
            );

            System.out.println("=== IMPS Backend Response ===");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            System.out.println("=============================\n");

            auditService.logOutboundResponse(packed, iso, "MANUAL_RESPCHKTXN",
                    impsBackendUrl + "/switch/respchktxn/2.1",
                    response.getStatusCode().value(), ProcessingStatus.SENT, null);

            ManualTriggerResponse resp = ManualTriggerResponse.success("RespChkTxn sent successfully");
            resp.setRrn(rrn);
            resp.setStan(stan);
            resp.setApprovalNumber(approvalNum);
            resp.setBackendResponse(response.getBody());
            return resp;

        } catch (Exception e) {
            System.err.println("Error in manual trigger: " + e.getMessage());
            return ManualTriggerResponse.error("Failed to send RespChkTxn: " + e.getMessage());
        }
    }

    /**
     * Manually trigger a RespHbt (0810) to IMPS Backend.
     */
    public ManualTriggerResponse sendRespHbt(ManualRespPayRequest request) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(packager);
            iso.setMTI("0810");

            iso.set(3, "990000"); // Heartbeat processing code
            
            String stan = generateStan();
            iso.set(11, stan);
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            
            String rrn = request.getRrn() != null && !request.getRrn().isEmpty() 
                    ? request.getRrn() : generateRrn();
            iso.set(37, rrn);
            
            String respCode = request.getResponseCode() != null && !request.getResponseCode().isEmpty()
                    ? request.getResponseCode() : "00";
            iso.set(39, respCode);
            
            iso.set(41, "IMPSTERM");

            // DE120: from request or last ReqHbt DE120 so manual matches auto
            String txnId = request.getTxnId();
            if (txnId == null || txnId.isEmpty()) txnId = lastRequestContext.getLastReqHbtDe120();
            if (txnId != null && !txnId.isEmpty()) iso.set(120, txnId);

            byte[] packed = iso.pack();
            
            System.out.println("\n===========================================");
            System.out.println("  MANUAL TRIGGER: SENDING RESPHBT TO IMPS");
            System.out.println("  To: " + impsBackendUrl + "/switch/resphbt/2.1");
            System.out.println("===========================================");
            printIso(iso, "MANUAL RESPHBT");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            HttpEntity<byte[]> httpRequest = new HttpEntity<>(packed, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                impsBackendUrl + "/switch/resphbt/2.1",
                HttpMethod.POST, httpRequest, String.class
            );

            System.out.println("=== IMPS Backend Response ===");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            System.out.println("=============================\n");

            auditService.logOutboundResponse(packed, iso, "MANUAL_RESPHBT",
                    impsBackendUrl + "/switch/resphbt/2.1",
                    response.getStatusCode().value(), ProcessingStatus.SENT, null);

            ManualTriggerResponse resp = ManualTriggerResponse.success("RespHbt sent successfully");
            resp.setRrn(rrn);
            resp.setStan(stan);
            resp.setBackendResponse(response.getBody());
            return resp;

        } catch (Exception e) {
            System.err.println("Error in manual trigger: " + e.getMessage());
            return ManualTriggerResponse.error("Failed to send RespHbt: " + e.getMessage());
        }
    }

    /**
     * Manually trigger a RespValAdd (0210) to IMPS Backend.
     */
    public ManualTriggerResponse sendRespValAdd(ManualRespPayRequest request) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(packager);
            iso.setMTI("0210");

            iso.set(3, "310000"); // Name enquiry processing code
            
            String stan = generateStan();
            iso.set(11, stan);
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            
            String rrn = request.getRrn() != null && !request.getRrn().isEmpty() 
                    ? request.getRrn() : generateRrn();
            iso.set(37, rrn);
            
            String approvalNum = request.getApprovalNumber() != null && !request.getApprovalNumber().isEmpty()
                    ? request.getApprovalNumber() : generateApprovalNumber();
            iso.set(38, approvalNum);
            
            String respCode = request.getResponseCode() != null && !request.getResponseCode().isEmpty()
                    ? request.getResponseCode() : "00";
            iso.set(39, respCode);
            
            iso.set(41, "IMPSTERM");
            
            // Account holder name in additional data
            iso.set(48, "ACCOUNT_HOLDER_NAME");
            
            if (request.getPayeeAccount() != null) {
                iso.set(102, request.getPayeeAccount());
            }
            
            // DE120: from request or last ReqValAdd DE120 so manual matches auto (IMPS updates transaction by DE120)
            String txnId = request.getTxnId();
            if (txnId == null || txnId.isEmpty()) txnId = lastRequestContext.getLastReqValAddDe120();
            if (txnId != null && !txnId.isEmpty()) iso.set(120, txnId);

            byte[] packed = iso.pack();
            
            System.out.println("\n===========================================");
            System.out.println("  MANUAL TRIGGER: SENDING RESPVALADD TO IMPS");
            System.out.println("  To: " + impsBackendUrl + "/switch/respvaladd/2.1");
            System.out.println("===========================================");
            printIso(iso, "MANUAL RESPVALADD");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            HttpEntity<byte[]> httpRequest = new HttpEntity<>(packed, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                impsBackendUrl + "/switch/respvaladd/2.1",
                HttpMethod.POST, httpRequest, String.class
            );

            System.out.println("=== IMPS Backend Response ===");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            System.out.println("=============================\n");

            auditService.logOutboundResponse(packed, iso, "MANUAL_RESPVALADD",
                    impsBackendUrl + "/switch/respvaladd/2.1",
                    response.getStatusCode().value(), ProcessingStatus.SENT, null);

            ManualTriggerResponse resp = ManualTriggerResponse.success("RespValAdd sent successfully");
            resp.setRrn(rrn);
            resp.setStan(stan);
            resp.setApprovalNumber(approvalNum);
            resp.setBackendResponse(response.getBody());
            return resp;

        } catch (Exception e) {
            System.err.println("Error in manual trigger: " + e.getMessage());
            return ManualTriggerResponse.error("Failed to send RespValAdd: " + e.getMessage());
        }
    }

    /**
     * Manually trigger a RespListAccPvd (0210) to IMPS Backend.
     */
    public ManualTriggerResponse sendRespListAccPvd(ManualRespPayRequest request) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(packager);
            iso.setMTI("0210");

            iso.set(3, "320000"); // List account provider processing code
            
            String stan = generateStan();
            iso.set(11, stan);
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            
            String rrn = request.getRrn() != null && !request.getRrn().isEmpty() 
                    ? request.getRrn() : generateRrn();
            iso.set(37, rrn);
            
            String respCode = request.getResponseCode() != null && !request.getResponseCode().isEmpty()
                    ? request.getResponseCode() : "00";
            iso.set(39, respCode);
            
            iso.set(41, "IMPSTERM");
            
            // DE120: from request or last ReqListAccPvd DE120 so manual matches auto
            String txnId = request.getTxnId();
            if (txnId == null || txnId.isEmpty()) txnId = lastRequestContext.getLastReqListAccPvdDe120();
            if (txnId != null && !txnId.isEmpty()) iso.set(120, txnId);
            
            // Bank list in additional data
            iso.set(48, "HDFC|ICICI|SBI|AXIS");

            byte[] packed = iso.pack();
            
            System.out.println("\n===========================================");
            System.out.println("  MANUAL TRIGGER: SENDING RESPLISTACCPVD TO IMPS");
            System.out.println("  To: " + impsBackendUrl + "/switch/resplistaccpvd/2.1");
            System.out.println("===========================================");
            printIso(iso, "MANUAL RESPLISTACCPVD");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            HttpEntity<byte[]> httpRequest = new HttpEntity<>(packed, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                impsBackendUrl + "/switch/resplistaccpvd/2.1",
                HttpMethod.POST, httpRequest, String.class
            );

            System.out.println("=== IMPS Backend Response ===");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            System.out.println("=============================\n");

            auditService.logOutboundResponse(packed, iso, "MANUAL_RESPLISTACCPVD",
                    impsBackendUrl + "/switch/resplistaccpvd/2.1",
                    response.getStatusCode().value(), ProcessingStatus.SENT, null);

            ManualTriggerResponse resp = ManualTriggerResponse.success("RespListAccPvd sent successfully");
            resp.setRrn(rrn);
            resp.setStan(stan);
            resp.setBackendResponse(response.getBody());
            return resp;

        } catch (Exception e) {
            System.err.println("Error in manual trigger: " + e.getMessage());
            return ManualTriggerResponse.error("Failed to send RespListAccPvd: " + e.getMessage());
        }
    }

    // Helper methods
    private String formatAmount(String amount) {
        if (amount == null || amount.isEmpty()) {
            return "000000100000"; // Default 1000.00
        }
        try {
            double value = Double.parseDouble(amount);
            long cents = (long) (value * 100);
            return String.format("%012d", cents);
        } catch (NumberFormatException e) {
            return "000000100000";
        }
    }

    private String generateStan() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String generateRrn() {
        return String.format("%012d", System.currentTimeMillis() % 1_000_000_000_000L);
    }

    private String generateApprovalNumber() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private void printIso(ISOMsg iso, String type) {
        try {
            System.out.println("--- " + type + " ISO ---");
            System.out.println("MTI: " + iso.getMTI());
            for (int i = 2; i <= 128; i++) {
                if (iso.hasField(i)) {
                    System.out.println("DE" + i + ": " + iso.getString(i));
                }
            }
            System.out.println("--- End ISO ---");
        } catch (ISOException e) {
            System.err.println("Error printing ISO: " + e.getMessage());
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
