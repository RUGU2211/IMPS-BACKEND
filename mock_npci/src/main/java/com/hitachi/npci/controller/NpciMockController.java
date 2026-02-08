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
        System.out.println("[MOCK_NPCI] ACK received from IMPS (request landed, processing)");
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
        System.out.println("[MOCK_NPCI] REQPAY received from IMPS txnId=" + txnId + " | ACK sent to IMPS");
        String ack = buildAck("ReqPay", extractMsgId(xml));
        return ack;
    }

    /* ===============================
       RESPPAY - Payment Response (dynamic: IMPS → NPCI at /npci/resppay/{txnId})
       =============================== */
    @PostMapping(value = "/resppay/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespPay(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("[MOCK_NPCI] RESPPAY received from IMPS txnId=" + txnId + " | ACK sent to IMPS");
        String ack = buildAck("RespPay", extractMsgId(xml));
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
        System.out.println("[MOCK_NPCI] REQCHKTXN received from IMPS txnId=" + txnId + " | ACK sent to IMPS");
        String ack = buildAck("ReqChkTxn", extractMsgId(xml));
        return ack;
    }

    @PostMapping(value = "/respchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespChkTxn(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("[MOCK_NPCI] RESPCHKTXN received from IMPS txnId=" + txnId + " | ACK sent to IMPS");
        String ack = buildAck("RespChkTxn", extractMsgId(xml));
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
        System.out.println("[MOCK_NPCI] REQHBT received from IMPS txnId=" + txnId + " | ACK sent to IMPS");
        String ack = buildAck("ReqHbt", extractMsgId(xml));
        return ack;
    }

    @PostMapping(value = "/resphbt/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespHbt(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("[MOCK_NPCI] RESPHBT received from IMPS txnId=" + txnId + " | ACK sent to IMPS");
        String ack = buildAck("RespHbt", extractMsgId(xml));
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
        System.out.println("[MOCK_NPCI] REQLISTACCPVD received from IMPS txnId=" + txnId + " | ACK sent to IMPS");
        String ack = buildAck("ReqListAccPvd", extractMsgId(xml));
        return ack;
    }

    @PostMapping(value = "/resplistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespListAccPvd(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("[MOCK_NPCI] RESPLISTACCPVD received from IMPS txnId=" + txnId + " | ACK sent to IMPS");
        String ack = buildAck("RespListAccPvd", extractMsgId(xml));
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
        System.out.println("[MOCK_NPCI] REQVALADD received from IMPS txnId=" + txnId + " | ACK sent to IMPS");
        String ack = buildAck("ReqValAdd", extractMsgId(xml));
        return ack;
    }

    @PostMapping(value = "/respvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespValAdd(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("[MOCK_NPCI] RESPVALADD received from IMPS txnId=" + txnId + " | ACK sent to IMPS");
        String ack = buildAck("RespValAdd", extractMsgId(xml));
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
