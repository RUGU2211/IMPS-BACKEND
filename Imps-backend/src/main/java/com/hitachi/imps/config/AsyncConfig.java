package com.hitachi.imps.config;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for async processing to handle multiple concurrent requests.
 * 
 * This allows the application to:
 * - Handle N number of concurrent IMPS requests
 * - Send immediate ACKs while processing continues in background
 * - Scale based on load
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - minimum threads always kept alive
        executor.setCorePoolSize(10);
        
        // Max pool size - maximum threads that can be created
        executor.setMaxPoolSize(50);
        
        // Queue capacity - requests waiting when all threads busy
        executor.setQueueCapacity(500);
        
        // Thread name prefix for debugging
        executor.setThreadNamePrefix("IMPS-Async-");
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();

        Logger log = LoggerFactory.getLogger(AsyncConfig.class);
        log.info("Async Executor Initialized: core={}, max={}, queue={}", executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}
