package com.hitachi.imps.service.heartbeat;

import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitachi.imps.entity.InstitutionMaster;
import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.repository.InstitutionMasterRepository;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.audit.MessageAuditService;

/**
 * IMPS Internal: TCP ping to each bank switch every 3 min. Updates institution_master.active.
 * By default (no ReqHbt from NPCI/Switch) console shows: which banks UP, which DOWN, contact details.
 * Manual ReqHbt (socket, HTTP, SSL) triggers full flow: DB check, response, log to transaction + message_audit_log.
 */
@Service
@ConditionalOnProperty(prefix = "imps.heartbeat", name = "switch-check-enabled", havingValue = "true", matchIfMissing = true)
public class HeartbeatSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatSchedulerService.class);
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final DateTimeFormatter TXN_ID_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Comma-separated ports that use TLS (e.g. 9444). When switch_port is in this list, switch-check uses SSL. */
    @Value("${imps.heartbeat.switch-check-ssl-ports:9444,9443}")
    private String switchCheckSslPorts;

    @Autowired
    private InstitutionMasterRepository institutionRepo;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private MessageAuditService auditService;

    /**
     * Check switch connectivity for each institution (all, including active=false).
     * Down banks are re-checked every interval; recovery sets active=true.
     */
    @Scheduled(
        initialDelayString = "${imps.heartbeat.switch-check-initial-delay-ms:10000}",
        fixedDelayString = "${imps.heartbeat.switch-check-interval-ms:180000}"
    )
    @Transactional
    public void checkSwitchConnectivity() {
        List<InstitutionMaster> all = institutionRepo.findAll();
        if (all == null || all.isEmpty()) {
            log.info("[Switch Check] No institutions in institution_master – skipping");
            return;
        }

        String txnId = "SWITCH_CHECK_" + LocalDateTime.now().format(TXN_ID_FMT);
        List<Map<String, Object>> banksToCheck = new ArrayList<>();
        for (InstitutionMaster inst : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", inst.getId());
            m.put("name", inst.getName());
            m.put("request_org_id", inst.getRequestOrgId());
            m.put("switch_ip", inst.getSwitchIp() != null ? inst.getSwitchIp() : "localhost");
            m.put("switch_port", inst.getSwitchPort() != null ? inst.getSwitchPort() : "9084");
            banksToCheck.add(m);
        }
        String reqJson = buildJson(Map.of("banks", banksToCheck, "checked_at", LocalDateTime.now().toString()));
        TransactionEntity txn = transactionService.createRequest(txnId, reqJson, "SWITCH_CHECK");
        transactionService.setReqOutDateTime(txn);

        log.info("[IMPS Switch Check] Checking connectivity for {} bank(s) (every 3 min)...", all.size());
        int upCount = 0;
        int downCount = 0;
        List<Map<String, Object>> upBanks = new ArrayList<>();
        List<Map<String, Object>> downBanks = new ArrayList<>();
        Map<String, Object> contactDetails = new LinkedHashMap<>();

        for (InstitutionMaster inst : all) {
            String host = inst.getSwitchIp() != null && !inst.getSwitchIp().isBlank() ? inst.getSwitchIp().trim() : "localhost";
            int port = parsePort(inst.getSwitchPort(), 9084);
            String addr = host + ":" + port;
            ConnectResult result = tryConnect(host, port, port);
            boolean reachable = result.success;
            boolean wasActive = Boolean.TRUE.equals(inst.getActive());

            String name = inst.getName() != null ? inst.getName() : "";
            String orgId = inst.getRequestOrgId() != null ? inst.getRequestOrgId() : "";
            String contact = formatContactDetails(inst);

            if (reachable) {
                upCount++;
                upBanks.add(Map.of("name", name, "request_org_id", orgId, "address", addr));
            } else {
                downCount++;
                downBanks.add(Map.of("name", name, "request_org_id", orgId, "address", addr,
                    "reason", result.errorMessage != null ? result.errorMessage : "unknown",
                    "contact", contact.isEmpty() ? "N/A" : contact));
                contactDetails.put(orgId, Map.of("name", name, "spoc", contact));
            }

            inst.setActive(reachable);
            if (reachable != wasActive) {
                institutionRepo.save(inst);
            }

            if (reachable) {
                log.info("[IMPS] BANK UP: {} ({}) at {} | To contact: {}", name, orgId, addr, contact.isEmpty() ? "N/A" : contact);
            } else {
                log.info("[IMPS] BANK DOWN: {} ({}) at {} | Reason: {} | To contact: {}",
                    name, orgId, addr, result.errorMessage != null ? result.errorMessage : "unknown",
                    contact.isEmpty() ? "N/A" : contact);
            }
        }

        log.info("[IMPS Switch Check] Complete: {} UP, {} DOWN", upCount, downCount);

        // Log to transaction and message_audit_log
        String respJson = buildJson(Map.of(
            "up_count", upCount,
            "down_count", downCount,
            "up_banks", upBanks,
            "down_banks", downBanks,
            "contact_details", contactDetails,
            "overall_status", downCount == 0 ? "SUCCESS" : "FAILED"
        ));
        if (downCount == 0) {
            transactionService.markSuccess(txn, respJson, null, null);
        } else {
            transactionService.markFailure(txn, respJson);
        }
        auditService.saveRaw(txnId, "SWITCH_CHECK_RESULT", respJson);
    }

    private String buildJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private static class ConnectResult {
        boolean success;
        String errorMessage;
    }

    private ConnectResult tryConnect(String host, int port, int portForSslCheck) {
        if (host == null || host.isBlank()) host = "localhost";
        ConnectResult r = new ConnectResult();
        boolean useSsl = isSslPort(portForSslCheck);
        try {
            if (useSsl) {
                try (SSLSocket ssl = createSslSocket(host.trim(), port)) {
                    r.success = true;
                    return r;
                }
            } else {
                try (Socket s = new Socket()) {
                    s.connect(new java.net.InetSocketAddress(host.trim(), port), CONNECT_TIMEOUT_MS);
                    r.success = true;
                    return r;
                }
            }
        } catch (Exception e) {
            r.success = false;
            r.errorMessage = e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "connection failed");
            return r;
        }
    }

    private boolean isSslPort(int port) {
        if (switchCheckSslPorts == null || switchCheckSslPorts.isBlank()) return false;
        Set<Integer> sslPorts = Arrays.stream(switchCheckSslPorts.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> { try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; } })
            .filter(p -> p > 0)
            .collect(Collectors.toSet());
        return sslPorts.contains(port);
    }

    /** Create and connect SSLSocket with trust-all (dev only) for switch-check. */
    private SSLSocket createSslSocket(String host, int port) throws Exception {
        TrustManager[] trustAll = new TrustManager[] {
            new X509TrustManager() {
                @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
            }
        };
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, trustAll, new java.security.SecureRandom());
        SSLSocket ssl = (SSLSocket) ctx.getSocketFactory().createSocket();
        ssl.connect(new java.net.InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        ssl.startHandshake();
        return ssl;
    }

    private static int parsePort(String s, int defaultPort) {
        if (s == null || s.isBlank()) return defaultPort;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return defaultPort;
        }
    }

    private static String formatContactDetails(InstitutionMaster inst) {
        StringBuilder sb = new StringBuilder();
        if (inst.getSpocName() != null && !inst.getSpocName().isBlank()) sb.append("spoc=").append(inst.getSpocName());
        if (inst.getSpocEmail() != null && !inst.getSpocEmail().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("email=").append(inst.getSpocEmail());
        }
        if (inst.getSpocPhone() != null && !inst.getSpocPhone().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("phone=").append(inst.getSpocPhone());
        }
        if (inst.getUrl() != null && !inst.getUrl().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("url=").append(inst.getUrl());
        }
        return sb.toString();
    }
}
