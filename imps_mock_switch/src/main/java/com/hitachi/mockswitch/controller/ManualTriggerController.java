package com.hitachi.mockswitch.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hitachi.mockswitch.dto.ManualRespPayRequest;
import com.hitachi.mockswitch.dto.ManualTriggerResponse;
import com.hitachi.mockswitch.service.ManualTriggerService;

/**
 * Controller for manually triggering ISO 8583 messages to IMPS Backend.
 * DISABLED by default: set mock.trigger.enabled=true to use /trigger/* endpoints.
 * When disabled, Switch uses AUTO-REPLY (responses sent automatically after receiving requests).
 *
 * Manual mode is ready to do what auto does: if you do not send txnId (DE120) in the request body,
 * the Switch uses the last received request's DE120 (RespPay/RespChkTxn/RespHbt/RespValAdd/RespListAccPvd)
 * so IMPS can update the correct transaction. Use auto only for now; switch to manual when needed.
 *
 * Use Cases (when enabled):
 * 1. /trigger/resppay - Simulate beneficiary bank sending RespPay (0210)
 * 2. /trigger/reqpay - Simulate Switch-initiated ReqPay (0200)
 */
@RestController
@RequestMapping("/trigger")
@ConditionalOnProperty(name = "mock.trigger.enabled", havingValue = "true")
public class ManualTriggerController {

    @Autowired
    private ManualTriggerService triggerService;

    /**
     * Manually trigger a RespPay ISO message to IMPS Backend.
     * 
     * This simulates the scenario where the beneficiary bank (via Switch) 
     * sends a fund transfer response back to IMPS → NPCI.
     * 
     * Example JSON body:
     * {
     *   "rrn": "769371630886",
     *   "amount": "1000.00",
     *   "payerAccount": "1009492174134",
     *   "payeeAccount": "119551457076",
     *   "txnId": "NPCI000000005t2Dk18UFMIMFENLBgb",
     *   "responseCode": "00",
     *   "approvalNumber": "123456"
     * }
     */
    @PostMapping(
        value = "/resppay",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ManualTriggerResponse> triggerRespPay(@RequestBody ManualRespPayRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  MANUAL TRIGGER: RESPPAY (JSON → ISO)");
        System.out.println("  Request: " + request);
        System.out.println("===========================================");

        ManualTriggerResponse response = triggerService.sendRespPay(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Manually trigger a ReqPay ISO message to IMPS Backend.
     * 
     * This simulates a Switch-initiated fund transfer request 
     * (rare case where transaction originates from Switch).
     */
    @PostMapping(
        value = "/reqpay",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ManualTriggerResponse> triggerReqPay(@RequestBody ManualRespPayRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  MANUAL TRIGGER: REQPAY (JSON → ISO)");
        System.out.println("  Request: " + request);
        System.out.println("===========================================");

        ManualTriggerResponse response = triggerService.sendReqPay(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Manually trigger a RespChkTxn ISO message to IMPS Backend.
     */
    @PostMapping(
        value = "/respchktxn",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ManualTriggerResponse> triggerRespChkTxn(@RequestBody ManualRespPayRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  MANUAL TRIGGER: RESPCHKTXN (JSON → ISO)");
        System.out.println("  Request: " + request);
        System.out.println("===========================================");

        ManualTriggerResponse response = triggerService.sendRespChkTxn(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Manually trigger a RespHbt ISO message to IMPS Backend.
     */
    @PostMapping(
        value = "/resphbt",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ManualTriggerResponse> triggerRespHbt(@RequestBody ManualRespPayRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  MANUAL TRIGGER: RESPHBT (JSON → ISO)");
        System.out.println("  Request: " + request);
        System.out.println("===========================================");

        ManualTriggerResponse response = triggerService.sendRespHbt(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Manually trigger a RespValAdd ISO message to IMPS Backend.
     */
    @PostMapping(
        value = "/respvaladd",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ManualTriggerResponse> triggerRespValAdd(@RequestBody ManualRespPayRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  MANUAL TRIGGER: RESPVALADD (JSON → ISO)");
        System.out.println("  Request: " + request);
        System.out.println("===========================================");

        ManualTriggerResponse response = triggerService.sendRespValAdd(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Manually trigger a RespListAccPvd ISO message to IMPS Backend.
     */
    @PostMapping(
        value = "/resplistaccpvd",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ManualTriggerResponse> triggerRespListAccPvd(@RequestBody ManualRespPayRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  MANUAL TRIGGER: RESPLISTACCPVD (JSON → ISO)");
        System.out.println("  Request: " + request);
        System.out.println("===========================================");

        ManualTriggerResponse response = triggerService.sendRespListAccPvd(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Health check for manual trigger endpoint.
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ManualTriggerResponse> health() {
        ManualTriggerResponse response = ManualTriggerResponse.success("Manual Trigger Service is healthy");
        return ResponseEntity.ok(response);
    }

    /**
     * Get sample request JSON for RespPay.
     */
    @GetMapping(value = "/sample/resppay", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ManualRespPayRequest> getSampleRespPay() {
        ManualRespPayRequest sample = new ManualRespPayRequest();
        sample.setRrn("769371630886");
        sample.setAmount("1000.00");
        sample.setPayerAccount("1009492174134");
        sample.setPayeeAccount("119551457076");
        sample.setTxnId("NPCI000000005t2Dk18UFMIMFENLBgb");
        sample.setResponseCode("00");
        sample.setApprovalNumber("123456");
        sample.setProcessingCode("400000");
        sample.setCurrency("356");
        return ResponseEntity.ok(sample);
    }
}
