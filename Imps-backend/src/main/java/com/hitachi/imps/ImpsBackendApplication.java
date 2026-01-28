package com.hitachi.imps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import com.hitachi.imps.config.RoutingConfig;

@EnableAsync
@EnableConfigurationProperties(RoutingConfig.class)
@SpringBootApplication
public class ImpsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImpsBackendApplication.class, args);
        System.out.println("===========================================");
        System.out.println("  IMPS Backend Application Started");
        System.out.println("===========================================");
    }
}
