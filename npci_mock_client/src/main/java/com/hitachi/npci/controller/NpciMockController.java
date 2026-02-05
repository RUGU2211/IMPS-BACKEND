package com.hitachi.npci.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * NPCI Mock Controller
 *
 * Receives XML messages from IMPS Backend and sends ACK responses.
 * Also receives ACK from IMPS (when request lands in IMPS) and logs to console.
 * Simulates NPCI behavior for testing.
 */
@RestController
@RequestMapping("/npci")
public class NpciMockController {

    /* ===============================
       ACK FROM IMPS - Log to console
       (IMPS sends ACK here so it is visible in NPCI Mock console)
       =============================== */
    @PostMapping(
        value = "/ack",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String handleAckFromImps(@RequestBody String ackXml, HttpServletRequest request) {
        System.out.println("\n###########################################");
        System.out.println("  ACK RECEIVED FROM IMPS (request landed, processing)");
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("###########################################");
        System.out.println(ackXml);
        System.out.println("###########################################\n");
        return "OK";
    }

    /* ===============================
       REQPAY - Payment Request (dynamic path only: /reqpay/{txnId})
       =============================== */
    @PostMapping(
        value = "/reqpay/{txnId}",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqPay(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: REQPAY RECEIVED txnId=" + txnId);
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        String ack = buildAck("ReqPay", extractMsgId(xml));
        System.out.println("  ACK SENT TO IMPS:");
        System.out.println(ack);
        System.out.println("===========================================\n");
        return ack;
    }

    /* ===============================
       RESPPAY - Payment Response (dynamic: IMPS → NPCI at /npci/resppay/{txnId})
       =============================== */
    @PostMapping(value = "/resppay/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespPay(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: RESPPAY RECEIVED txnId=" + txnId);
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");
        String ack = buildAck("RespPay", extractMsgId(xml));
        System.out.println("  ACK SENT TO IMPS:\n" + ack + "\n===========================================\n");
        return ack;
    }

    /* ===============================
       REQCHKTXN - Check Transaction (dynamic path only: /reqchktxn/{txnId})
       =============================== */
    @PostMapping(
        value = "/reqchktxn/{txnId}",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqChkTxn(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: REQCHKTXN RECEIVED txnId=" + txnId);
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        String ack = buildAck("ReqChkTxn", extractMsgId(xml));
        System.out.println("  ACK SENT TO IMPS:");
        System.out.println(ack);
        System.out.println("===========================================\n");
        return ack;
    }

    @PostMapping(value = "/respchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespChkTxn(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================\n  NPCI MOCK: RESPCHKTXN RECEIVED txnId=" + txnId + "\n  From: " + request.getRemoteAddr() + "\n===========================================\n" + xml + "\n===========================================\n");
        String ack = buildAck("RespChkTxn", extractMsgId(xml));
        System.out.println("  ACK SENT TO IMPS:\n" + ack + "\n===========================================\n");
        return ack;
    }

    /* ===============================
       REQHBT - Heartbeat Request (dynamic path only: /reqhbt/{txnId})
       =============================== */
    @PostMapping(
        value = "/reqhbt/{txnId}",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqHbt(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: REQHBT RECEIVED txnId=" + txnId);
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        String ack = buildAck("ReqHbt", extractMsgId(xml));
        System.out.println("  ACK SENT TO IMPS:");
        System.out.println(ack);
        System.out.println("===========================================\n");
        return ack;
    }

    @PostMapping(value = "/resphbt/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespHbt(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================\n  NPCI MOCK: RESPHBT RECEIVED txnId=" + txnId + "\n  From: " + request.getRemoteAddr() + "\n===========================================\n" + xml + "\n===========================================\n");
        String ack = buildAck("RespHbt", extractMsgId(xml));
        System.out.println("  ACK SENT TO IMPS:\n" + ack + "\n===========================================\n");
        return ack;
    }

    /* ===============================
       REQLISTACCPVD - List Account Providers (dynamic path only: /reqlistaccpvd/{txnId})
       =============================== */
    @PostMapping(
        value = "/reqlistaccpvd/{txnId}",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqListAccPvd(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: REQLISTACCPVD RECEIVED txnId=" + txnId);
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        String ack = buildAck("ReqListAccPvd", extractMsgId(xml));
        System.out.println("  ACK SENT TO IMPS:");
        System.out.println(ack);
        System.out.println("===========================================\n");
        return ack;
    }

    @PostMapping(value = "/resplistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespListAccPvd(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================\n  NPCI MOCK: RESPLISTACCPVD RECEIVED txnId=" + txnId + "\n  From: " + request.getRemoteAddr() + "\n===========================================\n" + xml + "\n===========================================\n");
        String ack = buildAck("RespListAccPvd", extractMsgId(xml));
        System.out.println("  ACK SENT TO IMPS:\n" + ack + "\n===========================================\n");
        return ack;
    }

    /* ===============================
       REQVALADD - Account Validation Request (dynamic path only: /reqvaladd/{txnId})
       =============================== */
    @PostMapping(
        value = "/reqvaladd/{txnId}",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqValAdd(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: REQVALADD RECEIVED txnId=" + txnId);
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        System.out.println("===========================================\n");

        String ack = buildAck("ReqValAdd", extractMsgId(xml));
        System.out.println("  ACK SENT TO IMPS:");
        System.out.println(ack);
        System.out.println("===========================================\n");
        return ack;
    }

    @PostMapping(value = "/respvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespValAdd(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("\n===========================================\n  NPCI MOCK: RESPVALADD RECEIVED txnId=" + txnId + "\n  From: " + request.getRemoteAddr() + "\n===========================================\n" + xml + "\n===========================================\n");
        String ack = buildAck("RespValAdd", extractMsgId(xml));
        System.out.println("  ACK SENT TO IMPS:\n" + ack + "\n===========================================\n");
        return ack;
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
