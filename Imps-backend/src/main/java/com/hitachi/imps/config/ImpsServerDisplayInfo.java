package com.hitachi.imps.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Provides IMPS server host, port, and protocol for console output (banner, flow logs).
 * All values from application.yml: imps.server.host, imps.server.protocol, server.port / local.server.port.
 */
@Component
public class ImpsServerDisplayInfo {

    @Value("${imps.server.host:localhost}")
    private String host;

    @Value("${imps.server.protocol:http}")
    private String protocol;

    @Value("${server.port:8081}")
    private String defaultPort;

    private final Environment env;

    public ImpsServerDisplayInfo(Environment env) {
        this.env = env;
    }

    public String getHost() {
        return host;
    }

    /** Protocol from application.yml (http or https). */
    public String getProtocol() {
        return (protocol != null && protocol.trim().toLowerCase().startsWith("https")) ? "https" : "http";
    }

    /** Actual port (from local.server.port when available, else server.port from yml). */
    public String getPort() {
        return env.getProperty("local.server.port", defaultPort);
    }

    public String getHostPort() {
        return host + ":" + getPort();
    }

    /** e.g. https://192.168.1.38:8443 – protocol from imps.server.protocol in application.yml */
    public String getBaseUrl() {
        return getProtocol() + "://" + getHostPort();
    }
}
