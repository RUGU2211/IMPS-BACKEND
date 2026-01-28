package com.hitachi.imps.controller.npci;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.XmlParsingService;
import com.hitachi.imps.service.pay.reqpay.NpciReqPayService;
import com.hitachi.imps.service.pay.resppay.NpciRespPayService;
import com.hitachi.imps.service.chktxn.reqchktxn.NpciReqChkTxnService;
import com.hitachi.imps.service.chktxn.respchktxn.NpciRespChkTxnService;
import com.hitachi.imps.service.heartbeat.reqhbt.NpciReqHbtService;
import com.hitachi.imps.service.heartbeat.resphbt.NpciRespHbtService;
import com.hitachi.imps.service.listaccpvd.reqlistaccpvd.NpciReqListAccPvdService;
import com.hitachi.imps.service.listaccpvd.resplistaccpvd.NpciRespListAccPvdService;
import com.hitachi.imps.service.valadd.reqvaladd.NpciReqValAddService;
import com.hitachi.imps.service.valadd.respvaladd.NpciRespValAddService;

/**
 * Controller for handling all NPCI XML requests.
 * 
 * Flow:
 * - Request endpoints: NPCI sends XML → App returns ACK → App converts to ISO → Sends to Switch
 * - Response endpoints: NPCI sends XML response → App returns ACK → App converts to ISO → Sends to Switch
 */
@RestController
@RequestMapping("/npci")
public class NpciController {

    @Autowired private AckService ackService;
    @Autowired private XmlParsingService xmlParsingService;

    // Pay services
    @Autowired private NpciReqPayService reqPayService;
    @Autowired private NpciRespPayService respPayService;

    // ChkTxn services
    @Autowired private NpciReqChkTxnService reqChkTxnService;
    @Autowired private NpciRespChkTxnService respChkTxnService;

    // Heartbeat services
    @Autowired private NpciReqHbtService reqHbtService;
    @Autowired private NpciRespHbtService respHbtService;

    // ListAccPvd services
    @Autowired private NpciReqListAccPvdService reqListAccPvdService;
    @Autowired private NpciRespListAccPvdService respListAccPvdService;

    // ValAdd services
    @Autowired private NpciReqValAddService reqValAddService;
    @Autowired private NpciRespValAddService respValAddService;

    /* ===============================
       1. REQPAY - Funds Transfer Request
       =============================== */
    @PostMapping(
        value = "/reqpay/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String reqPay(@RequestBody String xml) {
        System.out.println("=== NPCI REQPAY RECEIVED ===");
        System.out.println(xml);
        System.out.println("============================");

        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqPay", msgId);

        // Async: Convert XML to ISO and send to Switch
        reqPayService.processAsync(xml);

        return ack;
    }

    /* ===============================
       2. RESPPAY - Funds Transfer Response
       (When NPCI sends back RespPay)
       =============================== */
    @PostMapping(
        value = "/resppay/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String respPay(@RequestBody String xml) {
        System.out.println("=== NPCI RESPPAY RECEIVED ===");
        System.out.println(xml);
        System.out.println("=============================");

        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespPay", msgId);

        // Async: Convert XML to ISO and send to Switch
        respPayService.processAsync(xml);

        return ack;
    }

    /* ===============================
       3. REQCHKTXN - Check Transaction Status Request
       =============================== */
    @PostMapping(
        value = "/reqchktxn/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String reqChkTxn(@RequestBody String xml) {
        System.out.println("=== NPCI REQCHKTXN RECEIVED ===");
        System.out.println(xml);
        System.out.println("===============================");

        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqChkTxn", msgId);

        // Async: Convert XML to ISO and send to Switch
        reqChkTxnService.processAsync(xml);

        return ack;
    }

    /* ===============================
       4. RESPCHKTXN - Check Transaction Status Response
       =============================== */
    @PostMapping(
        value = "/respchktxn/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String respChkTxn(@RequestBody String xml) {
        System.out.println("=== NPCI RESPCHKTXN RECEIVED ===");
        System.out.println(xml);
        System.out.println("================================");

        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespChkTxn", msgId);

        // Async: Convert XML to ISO and send to Switch
        respChkTxnService.processAsync(xml);

        return ack;
    }

    /* ===============================
       5. REQHBT - Heartbeat Request
       =============================== */
    @PostMapping(
        value = "/reqhbt/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String reqHbt(@RequestBody String xml) {
        System.out.println("=== NPCI REQHBT RECEIVED ===");
        System.out.println(xml);
        System.out.println("============================");

        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqHbt", msgId);

        // Async: Process heartbeat and respond
        reqHbtService.processAsync(xml);

        return ack;
    }

    /* ===============================
       6. RESPHBT - Heartbeat Response
       =============================== */
    @PostMapping(
        value = "/resphbt/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String respHbt(@RequestBody String xml) {
        System.out.println("=== NPCI RESPHBT RECEIVED ===");
        System.out.println(xml);
        System.out.println("=============================");

        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespHbt", msgId);

        // Async: Convert XML to ISO and send to Switch
        respHbtService.processAsync(xml);

        return ack;
    }

    /* ===============================
       7. REQLISTACCPVD - List Account Providers Request
       =============================== */
    @PostMapping(
        value = "/reqlistaccpvd/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String reqListAccPvd(@RequestBody String xml) {
        System.out.println("=== NPCI REQLISTACCPVD RECEIVED ===");
        System.out.println(xml);
        System.out.println("===================================");

        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqListAccPvd", msgId);

        // Async: Build response from database
        reqListAccPvdService.processAsync(xml);

        return ack;
    }

    /* ===============================
       8. RESPLISTACCPVD - List Account Providers Response
       =============================== */
    @PostMapping(
        value = "/resplistaccpvd/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String respListAccPvd(@RequestBody String xml) {
        System.out.println("=== NPCI RESPLISTACCPVD RECEIVED ===");
        System.out.println(xml);
        System.out.println("====================================");

        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespListAccPvd", msgId);

        // Async: Process response
        respListAccPvdService.processAsync(xml);

        return ack;
    }

    /* ===============================
       9. REQVALADD - Name Enquiry Request
       =============================== */
    @PostMapping(
        value = "/reqvaladd/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String reqValAdd(@RequestBody String xml) {
        System.out.println("=== NPCI REQVALADD RECEIVED ===");
        System.out.println(xml);
        System.out.println("===============================");

        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqValAdd", msgId);

        // Async: Convert XML to ISO and send to Switch
        reqValAddService.processAsync(xml);

        return ack;
    }

    /* ===============================
       10. RESPVALADD - Name Enquiry Response
       =============================== */
    @PostMapping(
        value = "/respvaladd/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String respValAdd(@RequestBody String xml) {
        System.out.println("=== NPCI RESPVALADD RECEIVED ===");
        System.out.println(xml);
        System.out.println("================================");

        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespValAdd", msgId);

        // Async: Convert XML to ISO and send to Switch
        respValAddService.processAsync(xml);

        return ack;
    }
}
