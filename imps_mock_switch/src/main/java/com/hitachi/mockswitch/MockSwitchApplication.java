package com.hitachi.mockswitch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MockSwitchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockSwitchApplication.class, args);
        
        System.out.println("===========================================");
        System.out.println("  IMPS MOCK SWITCH Started on Port 8082");
        System.out.println("===========================================");
    }
}
