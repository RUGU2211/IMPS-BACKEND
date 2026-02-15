package com.hitachi.imps.controller.npci;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.hitachi.imps.client.npci.NpciRestClient;
import com.hitachi.imps.exception.CommonCodeValidationException;
import com.hitachi.imps.exception.ReqPayValidationException;
import com.hitachi.imps.service.ImpsInboundService;
import com.hitachi.imps.service.ack.AckSender;

/**
 * Handles NPCI XML requests under /npci. Delegates to ImpsInboundService so msg_id is
 * extracted and validated only once at entry (same as /imps REST path).
 */
@RestController
@RequestMapping("/npci")
public class NpciController {

    @Autowired private ImpsInboundService impsInboundService;
    @Autowired private NpciRestClient npciRestClient;

    private AckSender restAckSender() {
        return ack -> npciRestClient.sendAck(ack);
    }

    @PostMapping(value = "/reqpay/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqPay(@PathVariable String txnId, @RequestBody String xml) throws ReqPayValidationException {
        System.out.println("[IMPS] ReqPay received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        return impsInboundService.handleReqPay(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/resppay/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respPay(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("[IMPS] RespPay received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        return impsInboundService.handleRespPay(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqChkTxn(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("[IMPS] ReqChkTxn received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        return impsInboundService.handleReqChkTxn(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/respchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respChkTxn(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("[IMPS] RespChkTxn received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        return impsInboundService.handleRespChkTxn(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqHbt(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("[IMPS] ReqHbt received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        return impsInboundService.handleReqHbt(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqListAccPvd(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("[IMPS] ReqListAccPvd received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        return impsInboundService.handleReqListAccPvd(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/resplistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respListAccPvd(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("[IMPS] RespListAccPvd received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        return impsInboundService.handleRespListAccPvd(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqValAdd(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("[IMPS] ReqValAdd received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        return impsInboundService.handleReqValAdd(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/respvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respValAdd(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("[IMPS] RespValAdd received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        return impsInboundService.handleRespValAdd(xml, txnId, restAckSender());
    }
}
