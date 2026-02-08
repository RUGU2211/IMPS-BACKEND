package com.hitachi.mockswitch.controller;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.hitachi.mockswitch.converter.XmlToIsoConverter;
import com.hitachi.mockswitch.iso.MockIsoPackager;
import com.hitachi.mockswitch.service.MockResponseService;
import com.hitachi.mockswitch.service.ValidationService;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

/**
 * Dynamic: http://localhost:8082/imps/{reqpay|reqchktxn|...}/{txn_id}. IMPS sends here; Switch calls IMPS at http://localhost:8081/imps/{resptype}/{txn_id}.
 */
@RestController
@RequestMapping("/imps")
public class ImpsSwitchMockController {

    @Autowired private MockResponseService responseService;
    @Autowired private ValidationService validationService;
    @Autowired private XmlToIsoConverter xmlToIsoConverter;

    private byte[] resolveBody(byte[] body, String contentType, String apiType) {
        if (contentType != null && contentType.toLowerCase().contains("xml")) {
            String xml = new String(body, StandardCharsets.UTF_8);
            try {
                switch (apiType) {
                    case "REQPAY": return xmlToIsoConverter.convertReqPay(xml);
                    case "REQCHKTXN": return xmlToIsoConverter.convertReqChkTxn(xml);
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

    private String formatIsoForConsole(byte[] isoBytes) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(new MockIsoPackager());
            iso.unpack(isoBytes);
            StringBuilder sb = new StringBuilder();
            sb.append("MTI=").append(iso.getMTI()).append("\n");
            for (int i = 1; i <= 128; i++) {
                if (iso.hasField(i)) {
                    sb.append("DE").append(i).append("=").append(iso.getString(i)).append("\n");
                }
            }
            return sb.toString();
        } catch (ISOException e) {
            return "ISO parse failed: " + e.getMessage();
        }
    }

    @PostMapping(value = "/reqpay/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqpay(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQPAY");
        System.out.println("[MOCK_SWITCH] REQPAY ISO received from IMPS txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQPAY");
        if (isoMsg != null) {
            ValidationService.ValidationResult vr = validationService.validateReqPay(isoMsg);
            System.out.println("Validation: " + (vr.isValid() ? "VALID" : "INVALID"));
        }
        responseService.sendRespPayAsync(isoBytes, txnId);
        return buildAck("ReqPay");
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqchktxn(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQCHKTXN");
        System.out.println("[MOCK_SWITCH] REQCHKTXN ISO received from IMPS txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQCHKTXN");
        responseService.sendRespChkTxnAsync(isoBytes, txnId);
        return buildAck("ReqChkTxn");
    }

    /* ReqHbt removed: IMPS 60-sec check uses TCP connect only, does not send ReqHbt to Switch. */

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqlistaccpvd(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQLISTACCPVD");
        System.out.println("[MOCK_SWITCH] REQLISTACCPVD ISO received from IMPS txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQLISTACCPVD");
        responseService.sendRespListAccPvdAsync(isoBytes, txnId);
        return buildAck("ReqListAccPvd");
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqvaladd(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQVALADD");
        System.out.println("[MOCK_SWITCH] REQVALADD ISO received from IMPS txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQVALADD");
        responseService.sendRespValAddAsync(isoBytes, txnId);
        return buildAck("ReqValAdd");
    }
}
