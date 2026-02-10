package com.hitachi.imps.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Provides RestTemplate for outbound REST calls (NPCI mock, Switch).
 * When profile "ssl" is active, uses an SSLContext that trusts self-signed certs (dev only)
 * so that HTTPS calls to https://localhost:8445 (NPCI) and https://localhost:8082 (Switch) succeed.
 */
@Configuration
public class RestTemplateConfig {

    @Autowired
    private Environment environment;

    @Bean
    @Primary
    public RestTemplate restTemplate() throws Exception {
        if (Arrays.stream(environment.getActiveProfiles()).anyMatch("ssl"::equals)) {
            return createSslRestTemplate();
        }
        return new RestTemplate();
    }

    private static RestTemplate createSslRestTemplate() throws Exception {
        TrustManager[] trustAll = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAll, new java.security.SecureRandom());

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                if (connection instanceof HttpsURLConnection) {
                    try {
                        ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                        ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
                    } catch (Exception e) {
                        throw new IllegalStateException("SSL setup failed", e);
                    }
                }
            }
        };
        return new RestTemplate(factory);
    }
}
