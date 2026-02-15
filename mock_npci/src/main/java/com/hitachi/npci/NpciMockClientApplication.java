package com.hitachi.npci;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * NPCI Mock Client Application
 *
 * Simulates NPCI endpoints for testing IMPS Backend.
 * Receives XML messages and sends ACK responses.
 */
@SpringBootApplication
public class NpciMockClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(NpciMockClientApplication.class, args);
    }

    @Bean
    public ApplicationRunner startupBanner(Environment env) {
        return args -> {
            String port = env.getProperty("local.server.port", "?");
            String profile = env.getProperty("spring.profiles.active", "default");
            String protocol = env.getProperty("server.ssl.key-store") != null ? "https" : "http";
            System.out.println("===========================================");
            System.out.println("  Mock NPCI Application Started");
            System.out.println("  " + protocol.toUpperCase() + " : localhost:" + port);
            System.out.println("  Profile: " + profile);
            System.out.println("  Sends Req*/Resp* to IMPS, receives responses");
            System.out.println("===========================================");
        };
    }
}
