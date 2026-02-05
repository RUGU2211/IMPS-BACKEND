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
 * Dynamic Switch endpoints: http://localhost:8082/switch/{reqpay|reqchktxn|reqvaladd|reqhbt|reqlistaccpvd}/{txn_id}
 * IMPS sends requests here; Mock Switch responds to IMPS at http://localhost:8081/switch/{resppay|...}/{txn_id}.
 */
@RestController
@RequestMapping("/switch")
public class ImpsSwitchMockController {

    @Autowired private MockResponseService responseService;
    @Autowired private AuditService auditService;
    @Autowired private ValidationService validationService;
    @Autowired private XmlToIsoConverter xmlToIsoConverter;

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
                    default: return body;
                }
            } catch (ISOException e) {
                throw new RuntimeException("XML to ISO conversion failed for " + apiType, e);
            }
        }
        return body;
    }

    private String buildAck(String api) {
        return "<ns2:Ack xmlns:ns2=\"http://npci.org/upi/schema/\" api=\"" + api + "\" reqMsgId=\"SWITCH\" ts=\"" + java.time.OffsetDateTime.now() + "\"></ns2:Ack>";
    }

    @PostMapping(value = "/reqpay/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqpay(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQPAY");
        System.out.println("\n===========================================");
        System.out.println("  MOCK SWITCH: REQPAY txnId=" + txnId);
        System.out.println("===========================================");
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQPAY");
        if (isoMsg != null) {
            ValidationService.ValidationResult vr = validationService.validateReqPay(isoMsg);
            System.out.println("Validation: " + (vr.isValid() ? "VALID" : "INVALID"));
        }
        AuditLog audit = auditService.logInboundRequest(isoBytes, isoMsg, "REQPAY", request.getRemoteAddr() + ":" + request.getRemotePort());
        responseService.sendRespPayAsync(isoBytes, audit != null ? audit.getId() : null, txnId);
        return buildAck("ReqPay");
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqchktxn(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQCHKTXN");
        System.out.println("\n  MOCK SWITCH: REQCHKTXN txnId=" + txnId);
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQCHKTXN");
        AuditLog audit = auditService.logInboundRequest(isoBytes, isoMsg, "REQCHKTXN", request.getRemoteAddr() + ":" + request.getRemotePort());
        responseService.sendRespChkTxnAsync(isoBytes, audit != null ? audit.getId() : null, txnId);
        return buildAck("ReqChkTxn");
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqhbt(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQHBT");
        System.out.println("\n  MOCK SWITCH: REQHBT txnId=" + txnId);
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQHBT");
        AuditLog audit = auditService.logInboundRequest(isoBytes, isoMsg, "REQHBT", request.getRemoteAddr() + ":" + request.getRemotePort());
        responseService.sendRespHbtAsync(isoBytes, audit != null ? audit.getId() : null, txnId);
        return buildAck("ReqHbt");
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqlistaccpvd(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQLISTACCPVD");
        System.out.println("\n  MOCK SWITCH: REQLISTACCPVD txnId=" + txnId);
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQLISTACCPVD");
        AuditLog audit = auditService.logInboundRequest(isoBytes, isoMsg, "REQLISTACCPVD", request.getRemoteAddr() + ":" + request.getRemotePort());
        responseService.sendRespListAccPvdAsync(isoBytes, audit != null ? audit.getId() : null, txnId);
        return buildAck("ReqListAccPvd");
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqvaladd(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQVALADD");
        System.out.println("\n  MOCK SWITCH: REQVALADD txnId=" + txnId);
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQVALADD");
        AuditLog audit = auditService.logInboundRequest(isoBytes, isoMsg, "REQVALADD", request.getRemoteAddr() + ":" + request.getRemotePort());
        responseService.sendRespValAddAsync(isoBytes, audit != null ? audit.getId() : null, txnId);
        return buildAck("ReqValAdd");
    }
}
