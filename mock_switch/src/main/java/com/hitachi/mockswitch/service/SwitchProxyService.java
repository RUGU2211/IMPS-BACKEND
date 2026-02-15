package com.hitachi.mockswitch.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.hitachi.mockswitch.iso.MockIsoPackager;
import com.hitachi.mockswitch.util.Iso8583PrettyFormatter;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/**
 * Proxies Switch → IMPS. Receives req* from client (as Switch), forwards to IMPS, returns response.
 * Flow 2 (via mock_switch): ReqPay performs debit/credit on switch_db.account_master here before forwarding to IMPS.
 * For account_master updates use this path only. Socket path (IMPS → switch) does not update account_master.
 * Prints to console for visibility.
 */
@Service
public class SwitchProxyService {

    private final RestTemplate restTemplate;
    private static final MockIsoPackager packager = new MockIsoPackager();

    @Autowired
    private AccountLedgerService accountLedgerService;

    @Value("${imps.enabled:true}")
    private boolean impsEnabled;

    @Value("${imps.imps_ip:localhost}")
    private String impsIp;

    @Value("${imps.imps_port:8081}")
    private int impsPort;

    @Value("${imps.imps_ssl_port:8443}")
    private int impsSslPort;

    @Value("${imps.imps_use_ssl:false}")
    private boolean impsUseSsl;

    @Value("${imps.base-path:/imps}")
    private String basePath;

    public SwitchProxyService(@Qualifier("proxyRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String getImpsBaseUrl() {
        if (impsUseSsl) {
            return "https://" + impsIp + ":" + impsSslPort;
        }
        return "http://" + impsIp + ":" + impsPort;
    }

    private static final DateTimeFormatter LIFECYCLE_TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public byte[] forwardToImps(String apiType, String txnId, byte[] isoBytes, String messageType) {
        if (!impsEnabled) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                "IMPS forwarding is disabled. Set imps.enabled=true in application.yml to use the proxy.");
        }
        long t0 = System.currentTimeMillis();
        long ledgerMs = 0;
        if ("reqpay".equalsIgnoreCase(apiType) && isoBytes != null && isoBytes.length > 0) {
            long tLedger = System.currentTimeMillis();
            doDebitCredit(isoBytes, txnId);
            ledgerMs = System.currentTimeMillis() - tLedger;
        }
        long t1 = System.currentTimeMillis();

        String path = basePath.endsWith("/") ? basePath : basePath + "/";
        String url = getImpsBaseUrl() + path + apiType.toLowerCase() + "/" + txnId;

        logRequest(apiType, txnId, isoBytes, messageType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<byte[]> request = new HttpEntity<>(isoBytes, headers);
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, request, byte[].class);
            byte[] body = response.getBody();
            long t2 = System.currentTimeMillis();
            logResponse(apiType, txnId, body, messageType);
            printLifecycle(t0, t1, t2);
            printPerformance(ledgerMs, t2 - t0);
            return body != null ? body : new byte[0];
        } catch (HttpClientErrorException e) {
            System.err.println("[SWITCH] PROXY: IMPS returned " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            throw e;
        }
    }

    private void printLifecycle(long t0, long t1, long t2) {
        ZoneId zone = ZoneId.systemDefault();
        String ts0 = LocalDateTime.ofInstant(Instant.ofEpochMilli(t0), zone).format(LIFECYCLE_TIME);
        String ts1 = LocalDateTime.ofInstant(Instant.ofEpochMilli(t1), zone).format(LIFECYCLE_TIME);
        String ts2 = LocalDateTime.ofInstant(Instant.ofEpochMilli(t2), zone).format(LIFECYCLE_TIME);
        double totalSec = (t2 - t0) / 1000.0;
        System.out.println("──────────── TRANSACTION LIFECYCLE ────────────");
        System.out.println("[" + ts0 + "] Client → SWITCH");
        System.out.println("[" + ts1 + "] SWITCH → IMPS");
        System.out.println("[" + ts2 + "] IMPS → SWITCH");
        System.out.println("[" + ts2 + "] SWITCH → Client (Approved)");
        System.out.println("Total Processing Time: " + String.format("%.2f", totalSec) + " sec");
        System.out.println("───────────────────────────────────────────────");
    }

    private void printPerformance(long ledgerMs, long totalMs) {
        System.out.println("========= PERFORMANCE =========");
        if (ledgerMs > 0) {
            System.out.println("Ledger Update: " + ledgerMs + "ms");
        }
        System.out.println("Total Time: " + totalMs + "ms");
        System.out.println("===============================");
    }

    private void logRequest(String apiType, String txnId, byte[] isoBytes, String messageType) {
        System.out.println("========== [SWITCH] Client (as Switch) → IMPS | REQ | " + messageType + " ==========");
        System.out.println("  REQ received from: Client (as Switch)");
        System.out.println("  TxnId: " + txnId + " | Length: " + (isoBytes != null ? isoBytes.length : 0) + " bytes");
        System.out.println("  REQ sent to: IMPS at " + getImpsBaseUrl() + basePath + "/" + apiType.toLowerCase() + "/" + txnId);
        if (isoBytes != null && isoBytes.length > 0) {
            System.out.println(formatIsoPretty(isoBytes));
        }
        System.out.println("==========================================");
    }

    private void logResponse(String apiType, String txnId, byte[] body, String messageType) {
        String respType = messageType.replace("Req", "Resp");
        System.out.println("========== [SWITCH] IMPS → Client (as Switch) | RESP | " + respType + " ==========");
        System.out.println("  RESP received from: IMPS");
        System.out.println("  TxnId: " + txnId + " | Length: " + (body != null ? body.length : 0) + " bytes");
        System.out.println("  RESP sent to: Client (as Switch) | connection will close");
        if (body != null && body.length > 0) {
            System.out.println(formatIsoPretty(body));
        }
        System.out.println("==========================================");
    }

    private String formatIsoPretty(byte[] isoBytes) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(packager);
            iso.unpack(isoBytes);
            return Iso8583PrettyFormatter.format(iso);
        } catch (ISOException e) {
            return "ISO parse failed: " + e.getMessage();
        }
    }

    private void doDebitCredit(byte[] isoBytes, String txnId) {
        try {
            ISOMsg reqIso = new ISOMsg();
            reqIso.setPackager(packager);
            reqIso.unpack(isoBytes);
            String responseCode = accountLedgerService.debitAndCredit(reqIso);
            System.out.println("[SWITCH] Ledger: Debit/Credit completed for ReqPay | TxnId: " + txnId + " | ResponseCode: " + responseCode);
        } catch (ISOException e) {
            System.err.println("[SWITCH] PROXY: Debit/Credit failed (ISO parse error): " + e.getMessage());
        }
    }
}
