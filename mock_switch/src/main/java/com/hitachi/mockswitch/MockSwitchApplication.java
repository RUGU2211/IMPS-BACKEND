package com.hitachi.mockswitch;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MockSwitchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockSwitchApplication.class, args);
    }

    @Bean
    public ApplicationRunner startupBanner(Environment env) {
        return args -> {
            String port = env.getProperty("local.server.port", "?");
            String profile = env.getProperty("spring.profiles.active", "default");
            String protocol = env.getProperty("server.ssl.key-store") != null ? "https" : "http";
            System.out.println("===========================================");
            System.out.println("  Mock Switch Application Started");
            System.out.println("  " + protocol.toUpperCase() + " : localhost:" + port);
            System.out.println("  Profile: " + profile);
            String impsEnabled = env.getProperty("imps.enabled", "true");
            if ("true".equalsIgnoreCase(impsEnabled)) {
                System.out.println("  Receives Req*/Resp* from IMPS, forwards to IMPS Backend");
            } else {
                System.out.println("  Standalone: imps.enabled=false (outbound to IMPS disabled)");
            }
            System.out.println("===========================================");
        };
    }
}
