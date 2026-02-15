package com.hitachi.imps.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SSL/TLS configuration for IMPS socket server (NPCI inbound).
 * Used when ssl.enabled=true. Supports one-way TLS and mTLS (client-auth: need).
 */
@Configuration
@ConfigurationProperties(prefix = "ssl")
public class SslConfig {

    private static final Logger log = LoggerFactory.getLogger(SslConfig.class);
    private static final String DEFAULT_KEYSTORE_TYPE = "PKCS12";
    private static final String DEFAULT_PROTOCOL = "TLS";

    private boolean enabled = false;
    private String keyStore;
    private String keyStorePassword = "IMPS-Backend";
    private String keyStoreType = DEFAULT_KEYSTORE_TYPE;
    private String keyAlias = "imps";
    private String trustStore;
    private String trustStorePassword = "IMPS-Backend";
    private String trustStoreType = DEFAULT_KEYSTORE_TYPE;
    /** none, want, need. need = mTLS mandatory */
    private String clientAuth = "none";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getKeyStore() { return keyStore; }
    public void setKeyStore(String keyStore) { this.keyStore = keyStore; }
    public String getKeyStorePassword() { return keyStorePassword; }
    public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }
    public String getKeyStoreType() { return keyStoreType; }
    public void setKeyStoreType(String keyStoreType) { this.keyStoreType = keyStoreType; }
    public String getKeyAlias() { return keyAlias; }
    public void setKeyAlias(String keyAlias) { this.keyAlias = keyAlias; }
    public String getTrustStore() { return trustStore; }
    public void setTrustStore(String trustStore) { this.trustStore = trustStore; }
    public String getTrustStorePassword() { return trustStorePassword; }
    public void setTrustStorePassword(String trustStorePassword) { this.trustStorePassword = trustStorePassword; }
    public String getTrustStoreType() { return trustStoreType; }
    public void setTrustStoreType(String trustStoreType) { this.trustStoreType = trustStoreType; }
    public String getClientAuth() { return clientAuth; }
    public void setClientAuth(String clientAuth) { this.clientAuth = clientAuth; }

    public boolean isNeedClientAuth() { return "need".equalsIgnoreCase(clientAuth); }
    public boolean isWantClientAuth() { return "want".equalsIgnoreCase(clientAuth); }

    /**
     * Build SSLContext for server (NpciSocketServer).
     */
    public SSLContext buildServerContext() throws Exception {
        KeyManagerFactory kmf = loadKeyManagerFactory();
        TrustManagerFactory tmf = trustStore != null && !trustStore.isBlank()
            ? loadTrustManagerFactory() : null;

        SSLContext ctx = SSLContext.getInstance(DEFAULT_PROTOCOL);
        if (tmf != null) {
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        } else {
            ctx.init(kmf.getKeyManagers(), null, new SecureRandom());
        }
        return ctx;
    }

    /**
     * Build SSLContext for client (trust server cert; optional client cert).
     */
    public static SSLContext buildClientContext(String trustStorePath, String trustStorePassword,
                                                String keyStorePath, String keyStorePassword, String keyStoreType) throws Exception {
        TrustManagerFactory tmf = null;
        if (trustStorePath != null && !trustStorePath.isBlank()) {
            KeyStore ts = loadKeystore(trustStorePath, trustStorePassword, "PKCS12");
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
        }

        KeyManagerFactory kmf = null;
        if (keyStorePath != null && !keyStorePath.isBlank() && keyStorePassword != null) {
            KeyStore ks = loadKeystore(keyStorePath, keyStorePassword, keyStoreType != null ? keyStoreType : "PKCS12");
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyStorePassword.toCharArray());
        }

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf != null ? kmf.getKeyManagers() : null, tmf != null ? tmf.getTrustManagers() : null, new SecureRandom());
        return ctx;
    }

    private KeyManagerFactory loadKeyManagerFactory() throws Exception {
        KeyStore ks = loadKeystore(keyStore, keyStorePassword, keyStoreType);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyStorePassword.toCharArray());
        return kmf;
    }

    private TrustManagerFactory loadTrustManagerFactory() throws Exception {
        KeyStore ts = loadKeystore(trustStore, trustStorePassword, trustStoreType);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
        return tmf;
    }

    private static KeyStore loadKeystore(String path, String password, String type) throws Exception {
        KeyStore ks = KeyStore.getInstance(type);
        try (InputStream is = openResource(path)) {
            ks.load(is, password != null ? password.toCharArray() : null);
        }
        return ks;
    }

    private static InputStream openResource(String path) throws IOException {
        if (path == null || path.isBlank()) throw new IOException("Keystore/truststore path is empty");
        if (path.startsWith("classpath:")) {
            String resource = path.substring("classpath:".length()).trim();
            InputStream is = SslConfig.class.getClassLoader().getResourceAsStream(resource);
            if (is == null) throw new IOException("Classpath resource not found: " + resource);
            return is;
        }
        String filePath = path.startsWith("file:") ? path.substring(5).trim() : path;
        return Files.newInputStream(Paths.get(filePath));
    }
}
