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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic: http://localhost:8082/imps/{reqpay|reqchktxn|...}/{txn_id}. IMPS sends here; Switch calls IMPS at http://localhost:8081/imps/{resptype}/{txn_id}.
 */
@RestController
@RequestMapping("/imps")
public class ImpsSwitchMockController {

    private static final Logger log = LoggerFactory.getLogger(ImpsSwitchMockController.class);

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
        String isoDisplay = formatIsoForConsole(isoBytes);
        log.info("[SWITCH] ========== REQUEST FROM IMPS (HTTP) ==========");
        log.info("[SWITCH] Message Type: ReqPay | TxnId: {} | Length: {} bytes", txnId, isoBytes.length);
        log.info("[SWITCH] ISO received from IMPS:\n{}", isoDisplay);
        System.out.println("==========================================");
        System.out.println("[SWITCH] REQUEST FROM IMPS (HTTP)");
        System.out.println("Message Type: ReqPay | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes");
        System.out.println("==========================================");
        System.out.println(isoDisplay);
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQPAY");
        if (isoMsg != null) {
            ValidationService.ValidationResult vr = validationService.validateReqPay(isoMsg);
            System.out.println("[SWITCH] Validation: " + (vr.isValid() ? "VALID" : "INVALID"));
        }
        System.out.println("[SWITCH] Processing ReqPay - will update account_master and send response");
        responseService.sendRespPayAsync(isoBytes, txnId);
        return buildAck("ReqPay");
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqchktxn(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQCHKTXN");
        String isoDisplay = formatIsoForConsole(isoBytes);
        log.info("[SWITCH] ========== REQUEST FROM IMPS (HTTP) ==========");
        log.info("[SWITCH] Message Type: ReqChkTxn | TxnId: {} | Length: {} bytes", txnId, isoBytes.length);
        log.info("[SWITCH] ISO received from IMPS:\n{}", isoDisplay);
        System.out.println("==========================================");
        System.out.println("[SWITCH] REQUEST FROM IMPS (HTTP)");
        System.out.println("Message Type: ReqChkTxn | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes");
        System.out.println("==========================================");
        System.out.println(isoDisplay);
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQCHKTXN");
        responseService.sendRespChkTxnAsync(isoBytes, txnId);
        return buildAck("ReqChkTxn");
    }

    /* ReqHbt removed: IMPS 60-sec check uses TCP connect only, does not send ReqHbt to Switch. */

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqlistaccpvd(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQLISTACCPVD");
        String isoDisplay = formatIsoForConsole(isoBytes);
        log.info("[SWITCH] ========== REQUEST FROM IMPS (HTTP) ==========");
        log.info("[SWITCH] Message Type: ReqListAccPvd | TxnId: {} | Length: {} bytes", txnId, isoBytes.length);
        log.info("[SWITCH] ISO received from IMPS:\n{}", isoDisplay);
        System.out.println("==========================================");
        System.out.println("[SWITCH] REQUEST FROM IMPS (HTTP)");
        System.out.println("Message Type: ReqListAccPvd | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes");
        System.out.println("==========================================");
        System.out.println(isoDisplay);
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQLISTACCPVD");
        responseService.sendRespListAccPvdAsync(isoBytes, txnId);
        return buildAck("ReqListAccPvd");
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_XML_VALUE }, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqvaladd(@PathVariable String txnId, @RequestBody byte[] body, HttpServletRequest request) {
        byte[] isoBytes = resolveBody(body, request.getContentType(), "REQVALADD");
        String isoDisplay = formatIsoForConsole(isoBytes);
        log.info("[SWITCH] ========== REQUEST FROM IMPS (HTTP) ==========");
        log.info("[SWITCH] Message Type: ReqValAdd | TxnId: {} | Length: {} bytes", txnId, isoBytes.length);
        log.info("[SWITCH] ISO received from IMPS:\n{}", isoDisplay);
        System.out.println("==========================================");
        System.out.println("[SWITCH] REQUEST FROM IMPS (HTTP)");
        System.out.println("Message Type: ReqValAdd | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes");
        System.out.println("==========================================");
        System.out.println(isoDisplay);
        ISOMsg isoMsg = responseService.logIsoMessage(isoBytes, "REQVALADD");
        System.out.println("[SWITCH] Processing ReqValAdd - will query account_master and send response");
        responseService.sendRespValAddAsync(isoBytes, txnId);
        return buildAck("ReqValAdd");
    }
}
