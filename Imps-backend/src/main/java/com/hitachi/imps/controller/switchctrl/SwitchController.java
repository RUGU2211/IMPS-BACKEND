package com.hitachi.imps.controller.switchctrl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.pay.reqpay.SwitchReqPayService;
import com.hitachi.imps.service.pay.resppay.SwitchRespPayService;
import com.hitachi.imps.service.chktxn.reqchktxn.SwitchReqChkTxnService;
import com.hitachi.imps.service.chktxn.respchktxn.SwitchRespChkTxnService;
import com.hitachi.imps.service.heartbeat.reqhbt.SwitchReqHbtService;
import com.hitachi.imps.service.heartbeat.resphbt.SwitchRespHbtService;
import com.hitachi.imps.service.listaccpvd.reqlistaccpvd.SwitchReqListAccPvdService;
import com.hitachi.imps.service.listaccpvd.resplistaccpvd.SwitchRespListAccPvdService;
import com.hitachi.imps.service.valadd.reqvaladd.SwitchReqValAddService;
import com.hitachi.imps.service.valadd.respvaladd.SwitchRespValAddService;

/**
 * Controller for handling ISO messages FROM the Switch.
 * 
 * PRODUCTION FLOW:
 * 
 * Normal Transaction (NPCI initiated):
 *   1. NPCI sends ReqPay XML to /npci/reqpay/2.1
 *   2. App converts to ISO and sends to Switch (port 8082)
 *   3. Switch processes, sends RespPay ISO to /switch/resppay/2.1
 *   4. App converts to XML and sends to NPCI
 * 
 * Reverse Transaction (Switch initiated - rare):
 *   1. Switch sends ReqPay ISO to /switch/reqpay/2.1
 *   2. App converts to XML and sends to NPCI
 *   3. NPCI responds, app converts to ISO and sends back to Switch
 */
@RestController
@RequestMapping("/switch")
public class SwitchController {

    @Autowired private AckService ackService;

    // Request services (Switch-initiated requests → forward to NPCI)
    @Autowired private SwitchReqPayService reqPayService;
    @Autowired private SwitchReqChkTxnService reqChkTxnService;
    @Autowired private SwitchReqHbtService reqHbtService;
    @Autowired private SwitchReqListAccPvdService reqListAccPvdService;
    @Autowired private SwitchReqValAddService reqValAddService;

    // Response services (Switch responses → forward to NPCI)
    @Autowired private SwitchRespPayService respPayService;
    @Autowired private SwitchRespChkTxnService respChkTxnService;
    @Autowired private SwitchRespHbtService respHbtService;
    @Autowired private SwitchRespListAccPvdService respListAccPvdService;
    @Autowired private SwitchRespValAddService respValAddService;

    /* ===============================
       REQUEST ENDPOINTS
       Switch-initiated requests → Forward to NPCI
       =============================== */

    @PostMapping(value = "/reqpay/2.1", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqPay(@RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH REQPAY RECEIVED ===");
        System.out.println("ISO bytes length: " + isoBytes.length);
        System.out.println("Processing: Will forward to NPCI");
        System.out.println("==============================");

        String ack = ackService.buildAck("ReqPay", "SWITCH");
        reqPayService.processAsync(isoBytes);
        return ack;
    }

    @PostMapping(value = "/reqchktxn/2.1", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqChkTxn(@RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH REQCHKTXN RECEIVED ===");
        System.out.println("ISO bytes length: " + isoBytes.length);
        System.out.println("=================================");

        String ack = ackService.buildAck("ReqChkTxn", "SWITCH");
        reqChkTxnService.processAsync(isoBytes);
        return ack;
    }

    @PostMapping(value = "/reqhbt/2.1", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqHbt(@RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH REQHBT RECEIVED ===");
        System.out.println("ISO bytes length: " + isoBytes.length);
        System.out.println("==============================");

        String ack = ackService.buildAck("ReqHbt", "SWITCH");
        reqHbtService.processAsync(isoBytes);
        return ack;
    }

    @PostMapping(value = "/reqlistaccpvd/2.1", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqListAccPvd(@RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH REQLISTACCPVD RECEIVED ===");
        System.out.println("ISO bytes length: " + isoBytes.length);
        System.out.println("=====================================");

        String ack = ackService.buildAck("ReqListAccPvd", "SWITCH");
        reqListAccPvdService.processAsync(isoBytes);
        return ack;
    }

    @PostMapping(value = "/reqvaladd/2.1", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqValAdd(@RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH REQVALADD RECEIVED ===");
        System.out.println("ISO bytes length: " + isoBytes.length);
        System.out.println("=================================");

        String ack = ackService.buildAck("ReqValAdd", "SWITCH");
        reqValAddService.processAsync(isoBytes);
        return ack;
    }

    /* ===============================
       RESPONSE ENDPOINTS
       Switch responses → Forward to NPCI
       =============================== */

    @PostMapping(value = "/resppay/2.1", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respPay(@RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH RESPPAY RECEIVED ===");
        System.out.println("ISO bytes length: " + isoBytes.length);
        System.out.println("Processing: Will forward to NPCI");
        System.out.println("================================");

        String ack = ackService.buildAck("RespPay", "SWITCH");
        respPayService.processAsync(isoBytes);
        return ack;
    }

    @PostMapping(value = "/respchktxn/2.1", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respChkTxn(@RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH RESPCHKTXN RECEIVED ===");
        System.out.println("ISO bytes length: " + isoBytes.length);
        System.out.println("==================================");

        String ack = ackService.buildAck("RespChkTxn", "SWITCH");
        respChkTxnService.processAsync(isoBytes);
        return ack;
    }

    @PostMapping(value = "/resphbt/2.1", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respHbt(@RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH RESPHBT RECEIVED ===");
        System.out.println("ISO bytes length: " + isoBytes.length);
        System.out.println("===============================");

        String ack = ackService.buildAck("RespHbt", "SWITCH");
        respHbtService.processAsync(isoBytes);
        return ack;
    }

    @PostMapping(value = "/resplistaccpvd/2.1", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respListAccPvd(@RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH RESPLISTACCPVD RECEIVED ===");
        System.out.println("ISO bytes length: " + isoBytes.length);
        System.out.println("======================================");

        String ack = ackService.buildAck("RespListAccPvd", "SWITCH");
        respListAccPvdService.processAsync(isoBytes);
        return ack;
    }

    @PostMapping(value = "/respvaladd/2.1", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respValAdd(@RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH RESPVALADD RECEIVED ===");
        System.out.println("ISO bytes length: " + isoBytes.length);
        System.out.println("==================================");

        String ack = ackService.buildAck("RespValAdd", "SWITCH");
        respValAddService.processAsync(isoBytes);
        return ack;
    }
}
