package com.hitachi.mockswitch.controller;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.hitachi.mockswitch.converter.XmlToIsoConverter;
import com.hitachi.mockswitch.entity.AuditLog;
import com.hitachi.mockswitch.service.AuditService;
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
@RequestMapping("/switch")
public class MockSwitchController {

    @Autowired
    private MockResponseService responseService;

    @Autowired
    private AuditService auditService;

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
                    case "REQHBT": return xmlToIsoConverter.convertReqHbt(xml);
                    case "REQVALADD": return xmlToIsoConverter.convertReqValAdd(xml);
                    case "REQLISTACCPVD": return xmlToIsoConverter.convertReqListAccPvd(xml);
                    case "RESPPAY": return xmlToIsoConverter.convertRespPay(xml);
                    case "RESPCHKTXN": return xmlToIsoConverter.convertRespChkTxn(xml);
                    case "RESPHBT": return xmlToIsoConverter.convertRespHbt(xml);
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
       REQ* endpoints: dynamic only in ImpsSwitchMockController (/switch/reqpay/{txnId}, etc.)
       No static /2.1 — each request must use a unique txn_id in the path.
       =============================== */

    /* RESPPAY - Dynamic path only: /switch/resppay/{txnId}. Forwards to IMPS with same txnId. */
    @PostMapping(value = "/resppay/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespPay(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPPAY");
        System.out.println("\n===========================================");
        System.out.println("  MOCK SWITCH: RESPPAY RECEIVED txnId=" + txnId);
        System.out.println("===========================================");
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "RESPPAY");
        auditService.logInboundRequest(isoBytes, isoMsg, "RESPPAY", request.getRemoteAddr() + ":" + request.getRemotePort());
        responseService.forwardIsoToBackend(isoBytes, "/switch/resppay/" + txnId, "RESPPAY");
        return buildAck("RespPay");
    }

    @PostMapping(value = "/respchktxn/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespChkTxn(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPCHKTXN");
        System.out.println("\n  MOCK SWITCH: RESPCHKTXN RECEIVED txnId=" + txnId);
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "RESPCHKTXN");
        auditService.logInboundRequest(isoBytes, isoMsg, "RESPCHKTXN", request.getRemoteAddr() + ":" + request.getRemotePort());
        responseService.forwardIsoToBackend(isoBytes, "/switch/respchktxn/" + txnId, "RESPCHKTXN");
        return buildAck("RespChkTxn");
    }

    @PostMapping(value = "/resphbt/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespHbt(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPHBT");
        System.out.println("\n  MOCK SWITCH: RESPHBT RECEIVED txnId=" + txnId);
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "RESPHBT");
        auditService.logInboundRequest(isoBytes, isoMsg, "RESPHBT", request.getRemoteAddr() + ":" + request.getRemotePort());
        responseService.forwardIsoToBackend(isoBytes, "/switch/resphbt/" + txnId, "RESPHBT");
        return buildAck("RespHbt");
    }

    @PostMapping(value = "/respvaladd/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespValAdd(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPVALADD");
        System.out.println("\n  MOCK SWITCH: RESPVALADD RECEIVED txnId=" + txnId);
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "RESPVALADD");
        auditService.logInboundRequest(isoBytes, isoMsg, "RESPVALADD", request.getRemoteAddr() + ":" + request.getRemotePort());
        responseService.forwardIsoToBackend(isoBytes, "/switch/respvaladd/" + txnId, "RESPVALADD");
        return buildAck("RespValAdd");
    }

    @PostMapping(value = "/resplistaccpvd/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleRespListAccPvd(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPLISTACCPVD");
        System.out.println("\n  MOCK SWITCH: RESPLISTACCPVD RECEIVED txnId=" + txnId);
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "RESPLISTACCPVD");
        auditService.logInboundRequest(isoBytes, isoMsg, "RESPLISTACCPVD", request.getRemoteAddr() + ":" + request.getRemotePort());
        responseService.forwardIsoToBackend(isoBytes, "/switch/resplistaccpvd/" + txnId, "RESPLISTACCPVD");
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
