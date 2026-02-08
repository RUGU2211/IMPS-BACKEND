package com.hitachi.mockswitch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MockSwitchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockSwitchApplication.class, args);
        
        System.out.println("[MOCK_SWITCH] Started on Port 8082 | Receives Req*/Resp* from IMPS, forwards to IMPS Backend");
    }
}
