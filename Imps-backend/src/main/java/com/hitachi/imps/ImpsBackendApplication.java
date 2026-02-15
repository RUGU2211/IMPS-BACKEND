package com.hitachi.imps;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.hitachi.imps.config.RoutingConfig;

@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(RoutingConfig.class)
@EnableJpaRepositories(basePackages = "com.hitachi.imps.repository")
@SpringBootApplication
public class ImpsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImpsBackendApplication.class, args);
    }

    @Bean
    public ApplicationRunner startupBanner(Environment env) {
        return args -> {
            String port = env.getProperty("local.server.port", "?");
            String profile = env.getProperty("spring.profiles.active", "default");
            String protocol = env.getProperty("server.ssl.key-store") != null ? "https" : "http";
            System.out.println("===========================================");
            System.out.println("  IMPS Backend Application Started");
            String host = env.getProperty("server.address", "localhost");
            System.out.println("  " + protocol.toUpperCase() + " : " + host + ":" + port);
            System.out.println("  Profile: " + profile);
            System.out.println("===========================================");
        };
    }
}
