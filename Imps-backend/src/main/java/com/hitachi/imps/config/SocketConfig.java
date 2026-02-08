package com.hitachi.imps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Socket server configuration for NPCI TCP connection.
 * Format: [4-byte length big-endian][XML]. Response: same.
 */
@Configuration
@ConfigurationProperties(prefix = "socket")
public class SocketConfig {

    private boolean enabled = true;
    private Server server = new Server();
    private int maxXmlSize = 1024 * 1024;
    private int responseTimeoutSeconds = 60;

    public static class Server {
        private String bindHost = "0.0.0.0";
        private Npci npci = new Npci();

        public static class Npci {
            private int port = 9083;
            /** When ssl.enabled: true, use this port (default 9443) if set > 0 */
            private int sslPort = 9443;
            public int getPort() { return port; }
            public void setPort(int port) { this.port = port; }
            public int getSslPort() { return sslPort; }
            public void setSslPort(int sslPort) { this.sslPort = sslPort; }
        }
        public String getBindHost() { return bindHost; }
        public void setBindHost(String bindHost) { this.bindHost = bindHost; }
        public Npci getNpci() { return npci; }
        public void setNpci(Npci npci) { this.npci = npci; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }
    public int getMaxXmlSize() { return maxXmlSize; }
    public void setMaxXmlSize(int maxXmlSize) { this.maxXmlSize = maxXmlSize; }
    public int getResponseTimeoutSeconds() { return responseTimeoutSeconds; }
    public void setResponseTimeoutSeconds(int responseTimeoutSeconds) { this.responseTimeoutSeconds = responseTimeoutSeconds; }
}
