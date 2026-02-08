package com.hitachi.imps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * NPCI outbound socket configuration for IMPS → NPCI (Phase 3).
 * Used when npci.compliant-flow is true: IMPS opens new connection to NPCI to send Resp.
 */
@Configuration
@ConfigurationProperties(prefix = "npci")
public class NpciSocketConfig {

    /** When true: ACK-only on request socket; IMPS opens outbound socket to NPCI for Resp. */
    private boolean compliantFlow = false;
    private Socket socket = new Socket();

    public static class Socket {
        private String host = "localhost";
        private int port = 9085;
        private boolean sslEnabled = false;
        private String trustStore;
        private String trustStorePassword = "IMPS-Backend";
        private String keyStore;
        private String keyStorePassword = "IMPS-Backend";
        private String keyAlias = "imps-client";
        private int connectTimeoutMs = 10000;
        private int readTimeoutMs = 30000;

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
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    }

    public boolean isCompliantFlow() { return compliantFlow; }
    public void setCompliantFlow(boolean compliantFlow) { this.compliantFlow = compliantFlow; }
    public Socket getSocket() { return socket; }
    public void setSocket(Socket socket) { this.socket = socket; }
}
