package com.hitachi.mockswitch.controller;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.hitachi.mockswitch.converter.XmlToIsoConverter;
import com.hitachi.mockswitch.service.MockResponseService;
import com.hitachi.mockswitch.service.ValidationService;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

/**
 * Mock Switch Controller
 * 
 * Accepts both ISO 8583 binary (application/octet-stream) and NPCI XML (application/xml).
 * When XML is received, converts to ISO internally then processes.
 * IMPS Backend sends ISO to Mock Switch; Mock Switch sends ISO to IMPS Backend.
 */
@RestController
@RequestMapping("/imps")
public class MockSwitchController {

    @Autowired
    private MockResponseService responseService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private XmlToIsoConverter xmlToIsoConverter;

    /** Resolve body: if Content-Type is XML, convert to ISO bytes; else use body as ISO. */
    private byte[] resolveBody(byte[] body, String contentType, String apiType) {
        if (contentType != null && contentType.toLowerCase().contains("xml")) {
            String xml = new String(body, StandardCharsets.UTF_8);
            try {
                switch (apiType) {
                    case "REQPAY": return xmlToIsoConverter.convertReqPay(xml);
                    case "REQCHKTXN": return xmlToIsoConverter.convertReqChkTxn(xml);
                    case "REQVALADD": return xmlToIsoConverter.convertReqValAdd(xml);
                    case "REQLISTACCPVD": return xmlToIsoConverter.convertReqListAccPvd(xml);
                    case "RESPPAY": return xmlToIsoConverter.convertRespPay(xml);
                    case "RESPCHKTXN": return xmlToIsoConverter.convertRespChkTxn(xml);
                    case "RESPVALADD": return xmlToIsoConverter.convertRespValAdd(xml);
                    case "RESPLISTACCPVD": return xmlToIsoConverter.convertRespListAccPvd(xml);
                    default: return body;
                }
            } catch (ISOException e) {
                throw new RuntimeException("XML to ISO conversion failed for " + apiType, e);
            }
        }
        return body;
    }

    /* ===============================
       REQ* endpoints: dynamic in ImpsSwitchMockController (/imps/reqpay/{txnId}, etc.)
       No static /2.1 — each request must use a unique txn_id in the path.
       =============================== */

    /* RESPPAY - Dynamic: /imps/resppay/{txnId}. Forwards to IMPS at /imps/resppay/{txnId}. */
    @PostMapping(value = "/resppay/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespPay(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPPAY");
        String isoDisplay = responseService.formatIsoForConsole(isoBytes);
        System.out.println("==========================================");
        System.out.println("[MOCK_SWITCH] RESPONSE FROM IMPS (HTTP)");
        System.out.println("Message Type: RespPay | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes");
        System.out.println("==========================================");
        System.out.println(isoDisplay);
        System.out.println("[MOCK_SWITCH] Forwarding RespPay to IMPS Backend");
        responseService.logIsoMessage(isoBytes, "RESPPAY");
        responseService.forwardIsoToBackend(isoBytes, responseService.buildEndpointPath("resppay", txnId), "RESPPAY");
        return buildAck("RespPay");
    }

    @PostMapping(value = "/respchktxn/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespChkTxn(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPCHKTXN");
        String isoDisplay = responseService.formatIsoForConsole(isoBytes);
        System.out.println("==========================================");
        System.out.println("[MOCK_SWITCH] RESPONSE FROM IMPS (HTTP)");
        System.out.println("Message Type: RespChkTxn | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes");
        System.out.println("==========================================");
        System.out.println(isoDisplay);
        System.out.println("[MOCK_SWITCH] Forwarding RespChkTxn to IMPS Backend");
        responseService.logIsoMessage(isoBytes, "RESPCHKTXN");
        responseService.forwardIsoToBackend(isoBytes, responseService.buildEndpointPath("respchktxn", txnId), "RESPCHKTXN");
        return buildAck("RespChkTxn");
    }

    /* RespHbt removed: IMPS no longer has /imps/resphbt endpoint. */

    @PostMapping(value = "/respvaladd/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespValAdd(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPVALADD");
        String isoDisplay = responseService.formatIsoForConsole(isoBytes);
        System.out.println("==========================================");
        System.out.println("[MOCK_SWITCH] RESPONSE FROM IMPS (HTTP)");
        System.out.println("Message Type: RespValAdd | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes");
        System.out.println("==========================================");
        System.out.println(isoDisplay);
        System.out.println("[MOCK_SWITCH] Forwarding RespValAdd to IMPS Backend");
        responseService.logIsoMessage(isoBytes, "RESPVALADD");
        responseService.forwardIsoToBackend(isoBytes, responseService.buildEndpointPath("respvaladd", txnId), "RESPVALADD");
        return buildAck("RespValAdd");
    }

    @PostMapping(value = "/resplistaccpvd/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespListAccPvd(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPLISTACCPVD");
        String isoDisplay = responseService.formatIsoForConsole(isoBytes);
        System.out.println("==========================================");
        System.out.println("[MOCK_SWITCH] RESPONSE FROM IMPS (HTTP)");
        System.out.println("Message Type: RespListAccPvd | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes");
        System.out.println("==========================================");
        System.out.println(isoDisplay);
        System.out.println("[MOCK_SWITCH] Forwarding RespListAccPvd to IMPS Backend");
        responseService.logIsoMessage(isoBytes, "RESPLISTACCPVD");
        responseService.forwardIsoToBackend(isoBytes, responseService.buildEndpointPath("resplistaccpvd", txnId), "RESPLISTACCPVD");
        return buildAck("RespListAccPvd");
    }

    /* ===============================
       Build ACK Response
       =============================== */
    private String buildAck(String api) {
        String ts = java.time.OffsetDateTime.now().toString();
        String msgId = "MOCK_" + System.currentTimeMillis();
        
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <ns2:Ack xmlns:ns2="http://npci.org/upi/schema/"
                     api="%s"
                     reqMsgId="%s"
                     ts="%s">
            </ns2:Ack>
            """.formatted(api, msgId, ts);
    }
}
