package com.hitachi.mockswitch.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SSL configuration for Switch socket server. Used when socket.ssl-enabled=true.
 */
@Configuration
@ConditionalOnProperty(name = "socket.ssl-enabled", havingValue = "true")
@ConfigurationProperties(prefix = "socket.ssl")
public class SslConfig {

    private static final Logger log = LoggerFactory.getLogger(SslConfig.class);
    private static final String DEFAULT_KEYSTORE_TYPE = "JKS";
    private static final String DEFAULT_PROTOCOL = "TLS";

    private String keyStore = "file:../certs/switch-keystore.jks";
    private String keyStorePassword = "IMPS-Backend";
    private String keyStoreType = DEFAULT_KEYSTORE_TYPE;
    private String keyAlias = "switch";

    public String getKeyStore() { return keyStore; }
    public void setKeyStore(String keyStore) { this.keyStore = keyStore; }
    public String getKeyStorePassword() { return keyStorePassword; }
    public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }
    public String getKeyStoreType() { return keyStoreType; }
    public void setKeyStoreType(String keyStoreType) { this.keyStoreType = keyStoreType; }
    public String getKeyAlias() { return keyAlias; }
    public void setKeyAlias(String keyAlias) { this.keyAlias = keyAlias; }

    public SSLServerSocketFactory createServerSocketFactory() throws Exception {
        KeyStore ks = loadKeystore(keyStore, keyStorePassword, keyStoreType);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyStorePassword.toCharArray());
        SSLContext ctx = SSLContext.getInstance(DEFAULT_PROTOCOL);
        ctx.init(kmf.getKeyManagers(), null, new SecureRandom());
        return ctx.getServerSocketFactory();
    }

    private static KeyStore loadKeystore(String path, String password, String type) throws Exception {
        KeyStore ks = KeyStore.getInstance(type);
        try (InputStream is = openResource(path)) {
            ks.load(is, password != null ? password.toCharArray() : null);
        }
        return ks;
    }

    private static InputStream openResource(String path) throws IOException {
        if (path == null || path.isBlank()) throw new IOException("Keystore path is empty");
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
