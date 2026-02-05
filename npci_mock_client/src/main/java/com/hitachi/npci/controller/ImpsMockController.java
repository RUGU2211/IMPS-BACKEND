package com.hitachi.npci.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Dynamic IMPS API mock: /imps/{apiType}/req|resp/{txnId}
 * Receives XML from IMPS and returns ACK (same txnId used for whole flow).
 */
@RestController
@RequestMapping("/imps")
public class ImpsMockController {

    @PostMapping(value = "/pay/req/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String payReq(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("ReqPay", txnId, xml, request);
    }

    @PostMapping(value = "/pay/resp/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String payResp(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("RespPay", txnId, xml, request);
    }

    @PostMapping(value = "/chktxn/req/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String chktxnReq(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("ReqChkTxn", txnId, xml, request);
    }

    @PostMapping(value = "/chktxn/resp/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String chktxnResp(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("RespChkTxn", txnId, xml, request);
    }

    @PostMapping(value = "/hbt/req/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String hbtReq(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("ReqHbt", txnId, xml, request);
    }

    @PostMapping(value = "/hbt/resp/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String hbtResp(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("RespHbt", txnId, xml, request);
    }

    @PostMapping(value = "/listaccpvd/req/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String listaccpvdReq(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("ReqListAccPvd", txnId, xml, request);
    }

    @PostMapping(value = "/listaccpvd/resp/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String listaccpvdResp(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("RespListAccPvd", txnId, xml, request);
    }

    @PostMapping(value = "/valadd/req/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String valaddReq(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("ReqValAdd", txnId, xml, request);
    }

    @PostMapping(value = "/valadd/resp/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String valaddResp(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("RespValAdd", txnId, xml, request);
    }

    private String logAndAck(String api, String txnId, String xml, HttpServletRequest request) {
        System.out.println("\n===========================================");
        System.out.println("  NPCI MOCK: " + api + " (dynamic) txnId=" + txnId);
        System.out.println("  From: " + request.getRemoteAddr());
        System.out.println("===========================================");
        System.out.println(xml);
        String msgId = extractMsgId(xml);
        String ack = buildAck(api, msgId != null ? msgId : txnId);
        System.out.println("  ACK SENT TO IMPS:");
        System.out.println(ack);
        System.out.println("===========================================\n");
        return ack;
    }

    private String extractMsgId(String xml) {
        try {
            int i = xml.indexOf("msgId=\"");
            if (i != -1) {
                i += 7;
                int j = xml.indexOf("\"", i);
                if (j != -1) return xml.substring(i, j);
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    private String buildAck(String api, String reqMsgId) {
        String ts = java.time.OffsetDateTime.now().toString();
        return String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<ns2:Ack xmlns:ns2=\"http://npci.org/upi/schema/\" xmlns:ns3=\"http://npci.org/cm/schema/\" api=\"%s\" reqMsgId=\"%s\" ts=\"%s\">\n</ns2:Ack>",
            api, reqMsgId != null ? reqMsgId : "", ts
        );
    }
}
