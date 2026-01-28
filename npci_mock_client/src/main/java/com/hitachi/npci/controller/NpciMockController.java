package com.hitachi.npci.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * NPCI Mock Controller
 * 
 * Receives XML messages from IMPS Backend and sends ACK responses.
 * Simulates NPCI behavior for testing.
 */
@RestController
@RequestMapping("/npci")
public class NpciMockController {

    /* ===============================
       REQPAY - Payment Request
       =============================== */
    @PostMapping(
        value = "/reqpay/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqPay(@RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: REQPAY RECEIVED");
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        return buildAck("ReqPay", extractMsgId(xml));
    }

    /* ===============================
       RESPPAY - Payment Response
       =============================== */
    @PostMapping(
        value = "/resppay/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleRespPay(@RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: RESPPAY RECEIVED");
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        return buildAck("RespPay", extractMsgId(xml));
    }

    /* ===============================
       REQCHKTXN - Check Transaction
       =============================== */
    @PostMapping(
        value = "/reqchktxn/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqChkTxn(@RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: REQCHKTXN RECEIVED");
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        return buildAck("ReqChkTxn", extractMsgId(xml));
    }

    /* ===============================
       RESPCHKTXN - Check Transaction Response
       =============================== */
    @PostMapping(
        value = "/respchktxn/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleRespChkTxn(@RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: RESPCHKTXN RECEIVED");
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        return buildAck("RespChkTxn", extractMsgId(xml));
    }

    /* ===============================
       REQHBT - Heartbeat Request
       =============================== */
    @PostMapping(
        value = "/reqhbt/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqHbt(@RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: REQHBT RECEIVED");
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        return buildAck("ReqHbt", extractMsgId(xml));
    }

    /* ===============================
       RESPHBT - Heartbeat Response
       =============================== */
    @PostMapping(
        value = "/resphbt/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleRespHbt(@RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: RESPHBT RECEIVED");
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        return buildAck("RespHbt", extractMsgId(xml));
    }

    /* ===============================
       REQLISTACCPVD - List Account Providers
       =============================== */
    @PostMapping(
        value = "/reqlistaccpvd/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqListAccPvd(@RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: REQLISTACCPVD RECEIVED");
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        return buildAck("ReqListAccPvd", extractMsgId(xml));
    }

    /* ===============================
       RESPLISTACCPVD - List Account Providers Response
       =============================== */
    @PostMapping(
        value = "/resplistaccpvd/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleRespListAccPvd(@RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: RESPLISTACCPVD RECEIVED");
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        return buildAck("RespListAccPvd", extractMsgId(xml));
    }

    /* ===============================
       REQVALADD - Account Validation Request
       =============================== */
    @PostMapping(
        value = "/reqvaladd/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqValAdd(@RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: REQVALADD RECEIVED");
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        return buildAck("ReqValAdd", extractMsgId(xml));
    }

    /* ===============================
       RESPVALADD - Account Validation Response
       =============================== */
    @PostMapping(
        value = "/respvaladd/2.1",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleRespValAdd(@RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: RESPVALADD RECEIVED");
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        return buildAck("RespValAdd", extractMsgId(xml));
    }

    /* ===============================
       Build ACK Response
       =============================== */
    private String buildAck(String api, String reqMsgId) {
        String ts = java.time.OffsetDateTime.now().toString();
        String msgId = reqMsgId != null && !reqMsgId.isEmpty() ? reqMsgId : "NPCI_" + System.currentTimeMillis();
        
        return String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<ns2:Ack xmlns:ns2=\"http://npci.org/upi/schema/\" " +
            "xmlns:ns3=\"http://npci.org/cm/schema/\" " +
            "api=\"%s\" " +
            "reqMsgId=\"%s\" " +
            "ts=\"%s\">\n" +
            "</ns2:Ack>",
            api, msgId, ts
        );
    }

    /* ===============================
       Extract Message ID from XML
       =============================== */
    private String extractMsgId(String xml) {
        try {
            int startIdx = xml.indexOf("msgId=\"");
            if (startIdx != -1) {
                startIdx += 7; // length of "msgId=\""
                int endIdx = xml.indexOf("\"", startIdx);
                if (endIdx != -1) {
                    return xml.substring(startIdx, endIdx);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}
