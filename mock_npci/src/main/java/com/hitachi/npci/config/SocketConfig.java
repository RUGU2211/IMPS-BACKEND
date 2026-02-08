package com.hitachi.npci.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Socket server configuration for IMPS TCP connection.
 * Format: [4-byte length big-endian][XML]. IMPS sends RespPay, RespHbt, etc.
 */
@Configuration
@ConfigurationProperties(prefix = "socket")
public class SocketConfig {

    private boolean enabled = false;
    private Server server = new Server();

    public static class Server {
        private String bindHost = "0.0.0.0";
        private int port = 9085;
        private int sslPort = 9445;

        public String getBindHost() { return bindHost; }
        public void setBindHost(String bindHost) { this.bindHost = bindHost; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public int getSslPort() { return sslPort; }
        public void setSslPort(int sslPort) { this.sslPort = sslPort; }
    }

    private boolean sslEnabled = false;

    public boolean isSslEnabled() { return sslEnabled; }
    public void setSslEnabled(boolean sslEnabled) { this.sslEnabled = sslEnabled; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }
}
