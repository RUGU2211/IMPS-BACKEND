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
        System.out.println("  NPCI Mock Client Started");
        System.out.println("  Port: 8083");
        System.out.println("===========================================");
        System.out.println("\nAvailable Endpoints:");
        System.out.println("  POST /npci/reqpay/2.1");
        System.out.println("  POST /npci/resppay/2.1");
        System.out.println("  POST /npci/reqchktxn/2.1");
        System.out.println("  POST /npci/respchktxn/2.1");
        System.out.println("  POST /npci/reqhbt/2.1");
        System.out.println("  POST /npci/resphbt/2.1");
        System.out.println("  POST /npci/reqlistaccpvd/2.1");
        System.out.println("  POST /npci/resplistaccpvd/2.1");
        System.out.println("  POST /npci/reqvaladd/2.1");
        System.out.println("  POST /npci/respvaladd/2.1");
        System.out.println("\nAll endpoints receive XML and return ACK");
        System.out.println("===========================================\n");
    }
}
