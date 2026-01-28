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
       REQPAY - Fund Transfer Request
       Accepts: application/octet-stream (ISO) or application/xml (converted to ISO)
       =============================== */
    @PostMapping(
        value = "/reqpay/2.1",
        consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE },
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqPay(@RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQPAY");
        System.out.println("\n===========================================");
        System.out.println("  MOCK SWITCH: REQPAY RECEIVED");
        System.out.println("  " + (request.getContentType() != null && request.getContentType().toLowerCase().contains("xml") ? "XML (converted to ISO)" : "ISO") + " Length: " + isoBytes.length + " bytes");
        System.out.println("===========================================");

        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQPAY");
        if (isoMsg != null) {
            ValidationService.ValidationResult validationResult = validationService.validateReqPay(isoMsg);
            System.out.println("Validation Result: " + (validationResult.isValid() ? "VALID" : "INVALID"));
            if (!validationResult.isValid()) System.out.println("Validation Errors: " + validationResult.getValidations());
        }
        String sourceEndpoint = request.getRemoteAddr() + ":" + request.getRemotePort();
        AuditLog audit = auditService.logInboundRequest(isoBytes, isoMsg, "REQPAY", sourceEndpoint);
        // Auto-reply: send RespPay back to IMPS Backend (DE120 echoed so transaction can be updated)
        responseService.sendRespPayAsync(isoBytes, audit != null ? audit.getId() : null);
        return buildAck("ReqPay");
    }

    @PostMapping(
        value = "/reqchktxn/2.1",
        consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE },
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqChkTxn(@RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQCHKTXN");
        System.out.println("\n===========================================");
        System.out.println("  MOCK SWITCH: REQCHKTXN RECEIVED (" + (request.getContentType() != null && request.getContentType().toLowerCase().contains("xml") ? "XML→ISO" : "ISO") + ")");
        System.out.println("===========================================");
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQCHKTXN");
        String sourceEndpoint = request.getRemoteAddr() + ":" + request.getRemotePort();
        AuditLog audit = auditService.logInboundRequest(isoBytes, isoMsg, "REQCHKTXN", sourceEndpoint);
        responseService.sendRespChkTxnAsync(isoBytes, audit != null ? audit.getId() : null);
        return buildAck("ReqChkTxn");
    }

    @PostMapping(
        value = "/reqhbt/2.1",
        consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE },
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqHbt(@RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQHBT");
        System.out.println("\n===========================================");
        System.out.println("  MOCK SWITCH: REQHBT RECEIVED (" + (request.getContentType() != null && request.getContentType().toLowerCase().contains("xml") ? "XML→ISO" : "ISO") + ")");
        System.out.println("===========================================");
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQHBT");
        String sourceEndpoint = request.getRemoteAddr() + ":" + request.getRemotePort();
        AuditLog audit = auditService.logInboundRequest(isoBytes, isoMsg, "REQHBT", sourceEndpoint);
        responseService.sendRespHbtAsync(isoBytes, audit != null ? audit.getId() : null);
        return buildAck("ReqHbt");
    }

    @PostMapping(
        value = "/reqvaladd/2.1",
        consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE },
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqValAdd(@RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQVALADD");
        System.out.println("\n===========================================");
        System.out.println("  MOCK SWITCH: REQVALADD RECEIVED (" + (request.getContentType() != null && request.getContentType().toLowerCase().contains("xml") ? "XML→ISO" : "ISO") + ")");
        System.out.println("===========================================");
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQVALADD");
        String sourceEndpoint = request.getRemoteAddr() + ":" + request.getRemotePort();
        AuditLog audit = auditService.logInboundRequest(isoBytes, isoMsg, "REQVALADD", sourceEndpoint);
        responseService.sendRespValAddAsync(isoBytes, audit != null ? audit.getId() : null);
        return buildAck("ReqValAdd");
    }

    @PostMapping(
        value = "/reqlistaccpvd/2.1",
        consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE },
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleReqListAccPvd(@RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQLISTACCPVD");
        System.out.println("\n===========================================");
        System.out.println("  MOCK SWITCH: REQLISTACCPVD RECEIVED (" + (request.getContentType() != null && request.getContentType().toLowerCase().contains("xml") ? "XML→ISO" : "ISO") + ")");
        System.out.println("===========================================");
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQLISTACCPVD");
        String sourceEndpoint = request.getRemoteAddr() + ":" + request.getRemotePort();
        AuditLog audit = auditService.logInboundRequest(isoBytes, isoMsg, "REQLISTACCPVD", sourceEndpoint);
        responseService.sendRespListAccPvdAsync(isoBytes, audit != null ? audit.getId() : null);
        return buildAck("ReqListAccPvd");
    }

    /* RESPPAY - Accepts ISO or XML; if XML converts to ISO and forwards to IMPS Backend */
    @PostMapping(
        value = "/resppay/2.1",
        consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE },
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleRespPay(@RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPPAY");
        System.out.println("\n===========================================");
        System.out.println("  MOCK SWITCH: RESPPAY RECEIVED (" + (request.getContentType() != null && request.getContentType().toLowerCase().contains("xml") ? "XML→ISO" : "ISO") + ")");
        System.out.println("===========================================");
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "RESPPAY");
        String sourceEndpoint = request.getRemoteAddr() + ":" + request.getRemotePort();
        auditService.logInboundRequest(isoBytes, isoMsg, "RESPPAY", sourceEndpoint);
        responseService.forwardIsoToBackend(isoBytes, "/switch/resppay/2.1", "RESPPAY");
        return buildAck("RespPay");
    }

    @PostMapping(
        value = "/respchktxn/2.1",
        consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE },
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleRespChkTxn(@RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPCHKTXN");
        System.out.println("\n===========================================");
        System.out.println("  MOCK SWITCH: RESPCHKTXN RECEIVED (" + (request.getContentType() != null && request.getContentType().toLowerCase().contains("xml") ? "XML→ISO" : "ISO") + ")");
        System.out.println("===========================================");
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "RESPCHKTXN");
        String sourceEndpoint = request.getRemoteAddr() + ":" + request.getRemotePort();
        auditService.logInboundRequest(isoBytes, isoMsg, "RESPCHKTXN", sourceEndpoint);
        responseService.forwardIsoToBackend(isoBytes, "/switch/respchktxn/2.1", "RESPCHKTXN");
        return buildAck("RespChkTxn");
    }

    @PostMapping(
        value = "/resphbt/2.1",
        consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE },
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleRespHbt(@RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPHBT");
        System.out.println("\n===========================================");
        System.out.println("  MOCK SWITCH: RESPHBT RECEIVED (" + (request.getContentType() != null && request.getContentType().toLowerCase().contains("xml") ? "XML→ISO" : "ISO") + ")");
        System.out.println("===========================================");
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "RESPHBT");
        String sourceEndpoint = request.getRemoteAddr() + ":" + request.getRemotePort();
        auditService.logInboundRequest(isoBytes, isoMsg, "RESPHBT", sourceEndpoint);
        responseService.forwardIsoToBackend(isoBytes, "/switch/resphbt/2.1", "RESPHBT");
        return buildAck("RespHbt");
    }

    @PostMapping(
        value = "/respvaladd/2.1",
        consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE },
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleRespValAdd(@RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPVALADD");
        System.out.println("\n===========================================");
        System.out.println("  MOCK SWITCH: RESPVALADD RECEIVED (" + (request.getContentType() != null && request.getContentType().toLowerCase().contains("xml") ? "XML→ISO" : "ISO") + ")");
        System.out.println("===========================================");
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "RESPVALADD");
        String sourceEndpoint = request.getRemoteAddr() + ":" + request.getRemotePort();
        auditService.logInboundRequest(isoBytes, isoMsg, "RESPVALADD", sourceEndpoint);
        responseService.forwardIsoToBackend(isoBytes, "/switch/respvaladd/2.1", "RESPVALADD");
        return buildAck("RespValAdd");
    }

    @PostMapping(
        value = "/resplistaccpvd/2.1",
        consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE },
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handleRespListAccPvd(@RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "RESPLISTACCPVD");
        System.out.println("\n===========================================");
        System.out.println("  MOCK SWITCH: RESPLISTACCPVD RECEIVED (" + (request.getContentType() != null && request.getContentType().toLowerCase().contains("xml") ? "XML→ISO" : "ISO") + ")");
        System.out.println("===========================================");
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "RESPLISTACCPVD");
        String sourceEndpoint = request.getRemoteAddr() + ":" + request.getRemotePort();
        auditService.logInboundRequest(isoBytes, isoMsg, "RESPLISTACCPVD", sourceEndpoint);
        responseService.forwardIsoToBackend(isoBytes, "/switch/resplistaccpvd/2.1", "RESPLISTACCPVD");
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
