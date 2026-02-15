package com.hitachi.imps.service.routing;

import java.util.Optional;

import com.hitachi.imps.entity.InstitutionMaster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;

import com.hitachi.imps.config.RoutingConfig;
import com.hitachi.imps.repository.InstitutionMasterRepository;

/**
 * Resolves switch address (host, port) from institution_master by request_org_id.
 * Flow: Receive ReqPay from NPCI → read Head @orgId (request_org_id) → lookup institution_master → get switch_ip, switch_port → forward.
 * Falls back to application.yml (routing.switch and imps.routing) when no matching institution or null switch_ip/port.
 */
@Service
public class SwitchAddressResolver {

    @Autowired
    private InstitutionMasterRepository institutionRepo;

    @Autowired
    private RoutingConfig routingConfig;

    @Value("${imps.routing.switch-default-host:localhost}")
    private String switchDefaultHost;

    @Value("${imps.routing.switch-default-port:9084}")
    private String switchDefaultPort;

    /**
     * True when institution exists and active=false (bank/switch down).
     * Caller should respond FAILURE errCode=BANK_DOWN without forwarding to switch.
     */
    public boolean isBankDown(String requestOrgId) {
        if (requestOrgId == null || requestOrgId.isBlank()) return false;
        return institutionRepo.findFirstByRequestOrgIdOrderByIdAsc(requestOrgId.trim())
            .filter(inst -> !Boolean.TRUE.equals(inst.getActive()))
            .isPresent();
    }

    /**
     * Log failed switch connection details from institution_master to console.
     * Used by ReqPay/ReqChkTxn/ReqValAdd when BANK_DOWN. Uses config fallbacks for null switch_ip/port.
     */
    public void logFailedSwitchToConsole(InstitutionMaster inst) {
        logFailedSwitchToConsoleStatic(inst, switchDefaultHost, switchDefaultPort);
    }

    private static void logFailedSwitchToConsoleStatic(InstitutionMaster inst, String defaultHost, String defaultPort) {
        if (inst == null) return;
        String host = inst.getSwitchIp() != null && !inst.getSwitchIp().isBlank() ? inst.getSwitchIp() : defaultHost;
        String port = inst.getSwitchPort() != null && !inst.getSwitchPort().isBlank() ? inst.getSwitchPort() : defaultPort;
        System.out.println("[IMPS] Switch connection FAILED (institution_master): id=" + inst.getId()
            + " name=\"" + (inst.getName() != null ? inst.getName() : "") + "\""
            + " request_org_id=" + (inst.getRequestOrgId() != null ? inst.getRequestOrgId() : "")
            + " bank_code=" + (inst.getBankCode() != null ? inst.getBankCode() : "")
            + " switch_ip=" + host + " switch_port=" + port
            + " | DB switch_status=FAILED");
    }

    /**
     * Build proper NPCI error message when bank switch is down.
     * Includes bank name, orgId, switch address, and contact details for NPCI XML ErrMsg. Uses config fallbacks for null switch_ip/port.
     */
    public String buildBankDownErrMsg(InstitutionMaster inst) {
        return buildBankDownErrMsgStatic(inst, switchDefaultHost, switchDefaultPort);
    }

    private static String buildBankDownErrMsgStatic(InstitutionMaster inst, String defaultHost, String defaultPort) {
        if (inst == null) return "Bank switch unreachable. Transaction failed.";
        String name = inst.getName() != null ? inst.getName() : "Unknown";
        String orgId = inst.getRequestOrgId() != null ? inst.getRequestOrgId() : "";
        String host = inst.getSwitchIp() != null && !inst.getSwitchIp().isBlank() ? inst.getSwitchIp() : defaultHost;
        String port = inst.getSwitchPort() != null && !inst.getSwitchPort().isBlank() ? inst.getSwitchPort() : defaultPort;
        StringBuilder sb = new StringBuilder();
        sb.append("Transaction failed: Bank switch is down. Bank: ").append(name);
        if (!orgId.isEmpty()) sb.append(" (").append(orgId).append(")");
        sb.append(", switch: ").append(host).append(":").append(port);
        sb.append(". To contact: ");
        boolean first = true;
        if (inst.getSpocName() != null && !inst.getSpocName().isBlank()) {
            sb.append("spoc=").append(inst.getSpocName());
            first = false;
        }
        if (inst.getSpocEmail() != null && !inst.getSpocEmail().isBlank()) {
            if (!first) sb.append(", ");
            sb.append("email=").append(inst.getSpocEmail());
            first = false;
        }
        if (inst.getSpocPhone() != null && !inst.getSpocPhone().isBlank()) {
            if (!first) sb.append(", ");
            sb.append("phone=").append(inst.getSpocPhone());
            first = false;
        }
        if (inst.getUrl() != null && !inst.getUrl().isBlank()) {
            if (!first) sb.append(", ");
            sb.append("url=").append(inst.getUrl());
        }
        if (first) sb.append("N/A");
        return sb.toString();
    }

    /**
     * Returns the institution when it exists and active=false (bank/switch down).
     * Use for logging failed switch connection details dynamically from institution_master.
     */
    public Optional<InstitutionMaster> findDownInstitution(String requestOrgId) {
        if (requestOrgId == null || requestOrgId.isBlank()) return Optional.empty();
        return institutionRepo.findFirstByRequestOrgIdOrderByIdAsc(requestOrgId.trim())
            .filter(inst -> !Boolean.TRUE.equals(inst.getActive()));
    }

    /**
     * Resolve switch address for given request_org_id (Head @orgId from NPCI).
     * Uses institution_master first; falls back to application.yml if not found.
     */
    public SwitchAddress resolve(String requestOrgId) {
        RoutingConfig.SwitchSocketConfig socket = routingConfig.getSwitch().getSocket();
        boolean useSsl = socket != null && socket.isSslEnabled();
        int sslPort = socket != null ? socket.getSslPort() : 9444;
        int tcpPort = socket != null ? socket.getPort() : 9084;

        if (requestOrgId != null && !requestOrgId.isBlank()) {
            Optional<InstitutionMaster> opt = institutionRepo.findFirstByRequestOrgIdAndActiveTrueOrderByIdAsc(requestOrgId.trim());
            if (opt.isPresent()) {
                InstitutionMaster inst = opt.get();
                String host = inst.getSwitchIp() != null && !inst.getSwitchIp().isBlank() ? inst.getSwitchIp().trim() : switchDefaultHost;
                int port = parsePort(inst.getSwitchPort(), useSsl ? sslPort : tcpPort);
                if (useSsl && port == 9084) port = sslPort;
                return new SwitchAddress(host, port, true);
            }
        }
        int port = useSsl ? sslPort : tcpPort;
        String host = socket != null && socket.getHost() != null && !socket.getHost().isBlank() ? socket.getHost() : switchDefaultHost;
        return new SwitchAddress(host, port, false);
    }

    private static int parsePort(String s, int defaultPort) {
        if (s == null || s.isBlank()) return defaultPort;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return defaultPort;
        }
    }

    /** Switch host and port (socket mode). From DB or application.yml fallback. */
    public static class SwitchAddress {
        private final String host;
        private final int port;
        private final boolean fromDb;

        public SwitchAddress(String host, int port, boolean fromDb) {
            this.host = host;
            this.port = port;
            this.fromDb = fromDb;
        }

        public String getHost() { return host; }
        public int getPort() { return port; }
        public boolean isFromDb() { return fromDb; }

        /** REST base URL (HTTP port). When socket port 9084, REST typically 8082. */
        public String getRestBaseUrl() {
            int restPort = (port == 9084) ? 8082 : port;
            return "http://" + host + ":" + restPort;
        }
    }
}
