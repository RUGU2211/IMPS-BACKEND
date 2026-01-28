package com.hitachi.imps.config;

import java.util.concurrent.Executor;

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
        
        System.out.println("=== Async Executor Initialized ===");
        System.out.println("Core Pool: " + executor.getCorePoolSize());
        System.out.println("Max Pool: " + executor.getMaxPoolSize());
        System.out.println("Queue Capacity: " + executor.getQueueCapacity());
        System.out.println("==================================");
        
        return executor;
    }
}
