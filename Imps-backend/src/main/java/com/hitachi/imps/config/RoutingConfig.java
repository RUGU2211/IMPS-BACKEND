package com.hitachi.imps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for routing endpoints between NPCI and Switch.
 * Loads configuration from application.yml under 'routing' prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "routing")
public class RoutingConfig {

    private NpciConfig npci = new NpciConfig();
    private SwitchConfig switchConfig = new SwitchConfig();

    public NpciConfig getNpci() {
        return npci;
    }

    public void setNpci(NpciConfig npci) {
        this.npci = npci;
    }

    public SwitchConfig getSwitch() {
        return switchConfig;
    }

    public void setSwitch(SwitchConfig switchConfig) {
        this.switchConfig = switchConfig;
    }

    /**
     * NPCI routing configuration
     */
    public static class NpciConfig {
        private String baseUrl;
        private Map<String, String> endpoints = new HashMap<>();

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Map<String, String> getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(Map<String, String> endpoints) {
            this.endpoints = endpoints;
        }

        public String getFullUrl(String endpointKey) {
            return baseUrl + endpoints.getOrDefault(endpointKey, "");
        }
    }

    /**
     * Switch routing configuration
     */
    public static class SwitchConfig {
        private String baseUrl;
        private Map<String, String> endpoints = new HashMap<>();
        private SwitchSocketConfig socket = new SwitchSocketConfig();
        private SwitchRestConfig rest = new SwitchRestConfig();

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Map<String, String> getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(Map<String, String> endpoints) {
            this.endpoints = endpoints;
        }

        public SwitchSocketConfig getSocket() {
            return socket;
        }

        public void setSocket(SwitchSocketConfig socket) {
            this.socket = socket;
        }

        public SwitchRestConfig getRest() { return rest; }
        public void setRest(SwitchRestConfig rest) { this.rest = rest; }

        public String getFullUrl(String endpointKey) {
            return baseUrl + endpoints.getOrDefault(endpointKey, "");
        }
    }

    public static class SwitchRestConfig {
        private boolean enabled = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class SwitchSocketConfig {
        private boolean enabled = false;
        private String host = "localhost";
        private int port = 9084;
        private boolean sslEnabled = false;
        private String trustStore;
        private String trustStorePassword = "IMPS-Backend";
        private String keyStore;
        private String keyStorePassword = "IMPS-Backend";
        private String keyAlias = "imps-client";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public boolean isSslEnabled() { return sslEnabled; }
        public void setSslEnabled(boolean sslEnabled) { this.sslEnabled = sslEnabled; }
        public String getTrustStore() { return trustStore; }
        public void setTrustStore(String trustStore) { this.trustStore = trustStore; }
        public String getTrustStorePassword() { return trustStorePassword; }
        public void setTrustStorePassword(String trustStorePassword) { this.trustStorePassword = trustStorePassword; }
        public String getKeyStore() { return keyStore; }
        public void setKeyStore(String keyStore) { this.keyStore = keyStore; }
        public String getKeyStorePassword() { return keyStorePassword; }
        public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }
        public String getKeyAlias() { return keyAlias; }
        public void setKeyAlias(String keyAlias) { this.keyAlias = keyAlias; }
    }
}
