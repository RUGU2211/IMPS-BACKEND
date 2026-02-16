package com.hitachi.imps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.hitachi.imps.config.ImpsServerDisplayInfo;
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

    private static final Logger log = LoggerFactory.getLogger(ImpsBackendApplication.class);

    @Bean
    public ApplicationRunner startupBanner(ImpsServerDisplayInfo displayInfo, Environment env) {
        return args -> {
            String profile = env.getProperty("spring.profiles.active", "default");
            log.info("===========================================");
            log.info("  IMPS Backend Application Started");
            log.info("  {} | Profile: {}", displayInfo.getBaseUrl(), profile);
            log.info("===========================================");
        };
    }
}
