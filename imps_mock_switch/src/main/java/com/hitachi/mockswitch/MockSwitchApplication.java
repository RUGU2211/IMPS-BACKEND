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
        System.out.println("Request Endpoints (ISO 8583 IN):");
        System.out.println("  POST /switch/reqpay/2.1        - Receive ReqPay ISO 0200");
        System.out.println("  POST /switch/reqchktxn/2.1     - Receive ReqChkTxn ISO");
        System.out.println("  POST /switch/reqhbt/2.1        - Receive ReqHbt ISO 0800");
        System.out.println("  POST /switch/reqvaladd/2.1     - Receive ReqValAdd ISO");
        System.out.println("  POST /switch/reqlistaccpvd/2.1 - Receive ReqListAccPvd ISO");
        System.out.println("-------------------------------------------");
        System.out.println("Response Endpoints (ISO 8583 IN from IMPS):");
        System.out.println("  POST /switch/resppay/2.1       - Receive RespPay ISO 0210");
        System.out.println("  POST /switch/respchktxn/2.1    - Receive RespChkTxn ISO");
        System.out.println("  POST /switch/resphbt/2.1       - Receive RespHbt ISO 0810");
        System.out.println("  POST /switch/respvaladd/2.1    - Receive RespValAdd ISO");
        System.out.println("  POST /switch/resplistaccpvd/2.1- Receive RespListAccPvd ISO");
        System.out.println("-------------------------------------------");
        System.out.println("*** AUTO-REPLY MODE ***");
        System.out.println("Requests are auto-responded (RespPay/RespChkTxn/RespHbt/RespValAdd/RespListAccPvd).");
        System.out.println("Trigger (/trigger/*) is DISABLED. Set mock.trigger.enabled=true to enable.");
        System.out.println("===========================================");
        System.out.println("Switch Endpoints: application/octet-stream or application/xml");
        System.out.println("Database: imps_db (audit_log table)");
        System.out.println("===========================================");
    }
}
