package com.hitachi.mockswitch.service;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.hitachi.mockswitch.iso.MockIsoPackager;
import com.hitachi.mockswitch.repository.AccountMasterRepository;
import com.hitachi.mockswitch.service.AccountLedgerService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock Response Service
 * 
 * Generates mock ISO 8583 responses and sends them back to IMPS Backend.
 */
@Service
public class MockResponseService {

    private static final Logger log = LoggerFactory.getLogger(MockResponseService.class);

    @Value("${imps.enabled:true}")
    private boolean impsEnabled;

    @Value("${imps.imps_ip:localhost}")
    private String impsIp;

    @Value("${imps.imps_port:8081}")
    private int impsPort;

    @Value("${imps.base-path:/imps}")
    private String impsBasePath;

    private String getImpsBaseUrl() {
        return "http://" + impsIp + ":" + impsPort;
    }

    /** Build endpoint path dynamically: base-path / resptype / txnId (e.g. /imps/resppay/ABC123). */
    public String buildEndpointPath(String responseType, String txnId) {
        String id = (txnId != null && !txnId.isBlank()) ? txnId : "placeholder";
        String path = impsBasePath.endsWith("/") ? impsBasePath : impsBasePath + "/";
        return path + responseType.toLowerCase() + "/" + id;
    }

    @Value("${mock.response-delay-ms:500}")
    private int responseDelayMs;

    @Value("${mock.default-result:SUCCESS}")
    private String defaultResult;

    @Autowired
    private AccountLedgerService accountLedgerService;

    @Autowired
    private AccountMasterRepository accountMasterRepository;

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

            System.out.println("[SWITCH] Parsed ISO " + type + " MTI=" + iso.getMTI());

            return iso;

        } catch (ISOException e) {
            log.warn("Failed to parse ISO: {}", e.getMessage());
            return null;
        }
    }

    public String formatIsoForConsole(byte[] isoBytes) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(packager);
            iso.unpack(isoBytes);
            StringBuilder sb = new StringBuilder();
            sb.append("MTI=").append(iso.getMTI()).append("\n");
            for (int i = 1; i <= 128; i++) {
                if (iso.hasField(i)) {
                    sb.append("DE").append(i).append("=").append(iso.getString(i)).append("\n");
                }
            }
            return sb.toString();
        } catch (ISOException e) {
            return "ISO parse failed: " + e.getMessage();
        }
    }

    /* ===============================
       SEND RESPPAY
       =============================== */
    @Async
    public void sendRespPayAsync(byte[] reqIsoBytes, String inboundTxnId) {
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

            String endpoint = buildEndpointPath("resppay", inboundTxnId);
            sendToBackend(respIso, endpoint, "RESPPAY");

        } catch (Exception e) {
            log.error("Error sending RespPay: {}", e.getMessage(), e);
        }
    }

    /* ===============================
       SEND RESPCHKTXN
       =============================== */
    @Async
    public void sendRespChkTxnAsync(byte[] reqIsoBytes, String inboundTxnId) {
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

            String endpoint = buildEndpointPath("respchktxn", inboundTxnId);
            sendToBackend(respIso, endpoint, "RESPCHKTXN");

        } catch (Exception e) {
            log.error("Error sending RespChkTxn: {}", e.getMessage(), e);
        }
    }

    /* ===============================
       SEND RESPHBT
       =============================== */
    @Async
    public void sendRespHbtAsync(byte[] reqIsoBytes, String inboundTxnId) {
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

            String endpoint = buildEndpointPath("resphbt", inboundTxnId);
            sendToBackend(respIso, endpoint, "RESPHBT");

        } catch (Exception e) {
            log.error("Error sending RespHbt: {}", e.getMessage(), e);
        }
    }

    /* ===============================
       SEND RESPVALADD
       =============================== */
    @Async
    public void sendRespValAddAsync(byte[] reqIsoBytes, String inboundTxnId) {
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

            String endpoint = buildEndpointPath("respvaladd", inboundTxnId);
            sendToBackend(respIso, endpoint, "RESPVALADD");

        } catch (Exception e) {
            log.error("Error sending RespValAdd: {}", e.getMessage(), e);
        }
    }

    /* ===============================
       SEND RESPLISTACCPVD
       =============================== */
    @Async
    public void sendRespListAccPvdAsync(byte[] reqIsoBytes, String inboundTxnId) {
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

            String endpoint = buildEndpointPath("resplistaccpvd", inboundTxnId);
            sendToBackend(respIso, endpoint, "RESPLISTACCPVD");

        } catch (Exception e) {
            log.error("Error sending RespListAccPvd: {}", e.getMessage(), e);
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
            log.warn("Failed to unpack ISO: {}", e.getMessage());
            return null;
        }
    }

    private void copyField(ISOMsg src, ISOMsg dest, int field) throws ISOException {
        if (src.hasField(field)) {
            dest.set(field, src.getString(field));
        }
    }

    private void sendToBackend(ISOMsg iso, String endpoint, String type) {
        if (!impsEnabled) {
            log.info("[SWITCH] IMPS disabled (imps.enabled=false) – skipping forward to IMPS for {}", type);
            return;
        }
        try {
            byte[] packed = iso.pack();
            String respDisplay = formatIsoForConsole(packed);
            log.info("[SWITCH] ========== RESPONSE TO IMPS (HTTP) ==========");
            log.info("[SWITCH] Message Type: {} | Endpoint: {} | Length: {} bytes", type, endpoint, packed.length);
            log.info("[SWITCH] ISO response sent to IMPS:\n{}", respDisplay);
            System.out.println("==========================================");
            System.out.println("[SWITCH] RESPONSE TO IMPS (HTTP)");
            System.out.println("Message Type: " + type + " | Endpoint: " + endpoint + " | Length: " + packed.length + " bytes");
            System.out.println("==========================================");
            System.out.println(respDisplay);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            HttpEntity<byte[]> request = new HttpEntity<>(packed, headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(
                getImpsBaseUrl() + endpoint,
                HttpMethod.POST,
                request,
                byte[].class
            );
            byte[] body = response.getBody();
            log.info("[SWITCH] ========== ACK FROM IMPS (HTTP) ==========");
            log.info("[SWITCH] Status: {} | Response Length: {} bytes", response.getStatusCode(), body != null ? body.length : 0);
            System.out.println("==========================================");
            System.out.println("[SWITCH] ACK FROM IMPS (HTTP)");
            System.out.println("Status: " + response.getStatusCode() + " | Response Length: " + (body != null ? body.length : 0) + " bytes");
            System.out.println("==========================================");
            if (body != null && body.length > 0) {
                String ackDisplay = formatIsoForConsole(body);
                log.info("[SWITCH] IMPS ACK (ISO) received:\n{}", ackDisplay);
                System.out.println("[SWITCH] IMPS ACK (ISO) received:");
                System.out.println(ackDisplay);
            }
        } catch (Exception e) {
            log.error("[SWITCH] Failed to send to IMPS Backend: {}", e.getMessage(), e);
        }
    }

    private String generateStan() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String generateApprovalNumber() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    /* ===============================
       SYNC RESPONSE (for socket – returns ISO bytes directly)
       account_master is NOT updated here. Only the HTTP proxy path (flow 2: client → Switch → IMPS) updates switch_db.account_master.
       =============================== */
    public byte[] buildRespPaySync(byte[] reqIsoBytes, String txnId) {
        try {
            Thread.sleep(responseDelayMs);
            ISOMsg reqIso = unpack(reqIsoBytes);
            if (reqIso == null) return null;
            log.info("[SWITCH] ReqPay (socket from IMPS) - responding with 00, account_master NOT updated (only updated when request goes via Switch proxy)");
            System.out.println("[SWITCH] ReqPay (socket from IMPS) - responding 00, account_master NOT updated (only via Switch proxy flow)");
            String responseCode = "00";
            ISOMsg respIso = new ISOMsg();
            respIso.setPackager(packager);
            respIso.setMTI("0210");
            copyField(reqIso, respIso, 3); copyField(reqIso, respIso, 4); copyField(reqIso, respIso, 37);
            copyField(reqIso, respIso, 41); copyField(reqIso, respIso, 49); copyField(reqIso, respIso, 102);
            copyField(reqIso, respIso, 103); copyField(reqIso, respIso, 120);
            respIso.set(11, generateStan()); respIso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            respIso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            respIso.set(38, generateApprovalNumber()); respIso.set(39, responseCode);
            return respIso.pack();
        } catch (Exception e) {
            log.error("[SWITCH] buildRespPaySync error: {}", e.getMessage(), e);
            return null;
        }
    }

    public byte[] buildRespChkTxnSync(byte[] reqIsoBytes, String txnId) {
        try {
            Thread.sleep(responseDelayMs);
            ISOMsg reqIso = unpack(reqIsoBytes);
            if (reqIso == null) return null;
            ISOMsg respIso = new ISOMsg();
            respIso.setPackager(packager);
            respIso.setMTI("0210");
            copyField(reqIso, respIso, 3); copyField(reqIso, respIso, 37); copyField(reqIso, respIso, 41);
            copyField(reqIso, respIso, 48); copyField(reqIso, respIso, 120);
            respIso.set(11, generateStan()); respIso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            respIso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            respIso.set(38, generateApprovalNumber()); respIso.set(39, "00");
            return respIso.pack();
        } catch (Exception e) {
            log.warn("buildRespChkTxnSync: {}", e.getMessage());
            return null;
        }
    }

    public byte[] buildRespHbtSync(byte[] reqIsoBytes, String txnId) {
        try {
            Thread.sleep(responseDelayMs);
            ISOMsg reqIso = unpack(reqIsoBytes);
            if (reqIso == null) return null;
            ISOMsg respIso = new ISOMsg();
            respIso.setPackager(packager);
            respIso.setMTI("0810");
            copyField(reqIso, respIso, 3); copyField(reqIso, respIso, 24);
            copyField(reqIso, respIso, 37); copyField(reqIso, respIso, 41); copyField(reqIso, respIso, 120);
            respIso.set(11, generateStan()); respIso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            respIso.set(13, LocalDateTime.now().format(DATE_FORMAT)); respIso.set(39, "00");
            return respIso.pack();
        } catch (Exception e) {
            log.warn("buildRespHbtSync: {}", e.getMessage());
            return null;
        }
    }

    public byte[] buildRespValAddSync(byte[] reqIsoBytes, String txnId) {
        try {
            Thread.sleep(responseDelayMs);
            ISOMsg reqIso = unpack(reqIsoBytes);
            if (reqIso == null) return null;
            String acNum = reqIso.hasField(102) ? reqIso.getString(102) : null;
            String ifsc = reqIso.hasField(33) ? reqIso.getString(33) : null;
            String responseCode = "14";
            String accountHolderName = "";
            if (acNum != null && ifsc != null) {
                var accOpt = accountMasterRepository
                    .findByAccountNumberAndIfscCodeAndAccountStatusAndImpsEnabled(acNum.trim(), ifsc.trim().toUpperCase(), "ACTIVE", "Y");
                if (accOpt.isPresent()) {
                    responseCode = "00";
                    accountHolderName = accOpt.get().getAccountHolderName() != null ? accOpt.get().getAccountHolderName() : "ACCOUNT HOLDER";
                }
            }
            ISOMsg respIso = new ISOMsg();
            respIso.setPackager(packager);
            respIso.setMTI("0210");
            copyField(reqIso, respIso, 3); copyField(reqIso, respIso, 33); copyField(reqIso, respIso, 37);
            copyField(reqIso, respIso, 41); copyField(reqIso, respIso, 102); copyField(reqIso, respIso, 120);
            respIso.set(11, generateStan()); respIso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            respIso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            respIso.set(38, generateApprovalNumber()); respIso.set(39, responseCode);
            respIso.set(48, accountHolderName.isEmpty() ? "INVALID_ACCOUNT" : accountHolderName);
            return respIso.pack();
        } catch (Exception e) {
            log.warn("buildRespValAddSync: {}", e.getMessage());
            return null;
        }
    }

    public byte[] buildRespListAccPvdSync(byte[] reqIsoBytes, String txnId) {
        try {
            Thread.sleep(responseDelayMs);
            ISOMsg reqIso = unpack(reqIsoBytes);
            if (reqIso == null) return null;
            ISOMsg respIso = new ISOMsg();
            respIso.setPackager(packager);
            respIso.setMTI("0210");
            respIso.set(3, "320000");  // RespListAccPvd processing code
            copyField(reqIso, respIso, 37);
            copyField(reqIso, respIso, 41);
            copyField(reqIso, respIso, 120);
            respIso.set(11, generateStan());
            respIso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            respIso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            respIso.set(39, "00");
            respIso.set(49, "356");  // INR
            respIso.set(48, "HDFC|ICICI|SBI|AXIS");  // AccPvd list (mock)
            return respIso.pack();
        } catch (Exception e) {
            log.warn("buildRespListAccPvdSync: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Forward raw ISO bytes to IMPS Backend (e.g. when Mock Switch received XML and converted to ISO).
     * IMPS returns ISO ACK (application/octet-stream); we accept byte[] response.
     */
    public void forwardIsoToBackend(byte[] isoBytes, String endpoint, String type) {
        if (!impsEnabled) {
            log.info("[SWITCH] IMPS disabled (imps.enabled=false) – skipping forward to IMPS for {}", type);
            return;
        }
        try {
            String isoDisplay = formatIsoForConsole(isoBytes);
            log.info("[SWITCH] ========== FORWARDING RESPONSE TO IMPS (HTTP) ==========");
            log.info("[SWITCH] Message Type: {} | Endpoint: {} | Length: {} bytes", type, endpoint, isoBytes.length);
            log.info("[SWITCH] ISO being forwarded:\n{}", isoDisplay);
            System.out.println("==========================================");
            System.out.println("[SWITCH] FORWARDING RESPONSE TO IMPS (HTTP)");
            System.out.println("Message Type: " + type + " | Endpoint: " + endpoint + " | Length: " + isoBytes.length + " bytes");
            System.out.println("==========================================");
            System.out.println(isoDisplay);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            HttpEntity<byte[]> request = new HttpEntity<>(isoBytes, headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(
                getImpsBaseUrl() + endpoint,
                HttpMethod.POST,
                request,
                byte[].class
            );
            byte[] isoAck = response.getBody();
            log.info("[SWITCH] Forwarded {} to IMPS Backend: {} | ACK Length: {} bytes", 
                type, response.getStatusCode(), isoAck != null ? isoAck.length : 0);
            System.out.println("[SWITCH] Forwarded " + type + " to IMPS Backend: " + response.getStatusCode()
                + (isoAck != null && isoAck.length > 0 ? " | ISO ACK received (" + isoAck.length + " bytes)" : ""));
            if (isoAck != null && isoAck.length > 0) {
                String ackDisplay = formatIsoForConsole(isoAck);
                log.info("[SWITCH] IMPS ACK received:\n{}", ackDisplay);
                System.out.println("[SWITCH] IMPS ACK received:");
                System.out.println(ackDisplay);
            }
        } catch (Exception e) {
            log.error("[SWITCH] Forward to IMPS Backend failed: {}", e.getMessage(), e);
        }
    }
}
