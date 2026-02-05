package com.hitachi.npci;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NPCI Mock Client Application
 * 
 * Simulates NPCI endpoints for testing IMPS Backend.
 * Receives XML messages and sends ACK responses.
 * 
 * Port: 8083
 */
@SpringBootApplication
public class NpciMockClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(NpciMockClientApplication.class, args);
        
        System.out.println("\n===========================================");
        System.out.println("  NPCI Mock Client Started Port: 8083");
        System.out.println("===========================================");
    }
}
