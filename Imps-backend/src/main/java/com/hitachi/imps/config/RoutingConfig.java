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
}
