package com.hitachi.imps.controller.npci;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(NpciController.class);

    @Autowired private ImpsInboundService impsInboundService;
    @Autowired private NpciRestClient npciRestClient;

    private AckSender restAckSender() {
        return ack -> npciRestClient.sendAck(ack);
    }

    @PostMapping(value = "/reqpay/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqPay(@PathVariable String txnId, @RequestBody String xml) throws ReqPayValidationException {
        log.info("[IMPS] ReqPay received from NPCI (REST) txnId={}", txnId);
        log.debug("ReqPay XML: {}", xml);
        return impsInboundService.handleReqPay(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/resppay/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respPay(@PathVariable String txnId, @RequestBody String xml) {
        log.info("[IMPS] RespPay received from NPCI (REST) txnId={}", txnId);
        log.debug("RespPay XML: {}", xml);
        return impsInboundService.handleRespPay(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqChkTxn(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        log.info("[IMPS] ReqChkTxn received from NPCI (REST) txnId={}", txnId);
        log.debug("ReqChkTxn XML: {}", xml);
        return impsInboundService.handleReqChkTxn(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/respchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respChkTxn(@PathVariable String txnId, @RequestBody String xml) {
        log.info("[IMPS] RespChkTxn received from NPCI (REST) txnId={}", txnId);
        log.debug("RespChkTxn XML: {}", xml);
        return impsInboundService.handleRespChkTxn(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqHbt(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        log.info("[IMPS] ReqHbt received from NPCI (REST) txnId={}", txnId);
        log.debug("ReqHbt XML: {}", xml);
        return impsInboundService.handleReqHbt(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqListAccPvd(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        log.info("[IMPS] ReqListAccPvd received from NPCI (REST) txnId={}", txnId);
        log.debug("ReqListAccPvd XML: {}", xml);
        return impsInboundService.handleReqListAccPvd(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/resplistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respListAccPvd(@PathVariable String txnId, @RequestBody String xml) {
        log.info("[IMPS] RespListAccPvd received from NPCI (REST) txnId={}", txnId);
        log.debug("RespListAccPvd XML: {}", xml);
        return impsInboundService.handleRespListAccPvd(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqValAdd(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        log.info("[IMPS] ReqValAdd received from NPCI (REST) txnId={}", txnId);
        log.debug("ReqValAdd XML: {}", xml);
        return impsInboundService.handleReqValAdd(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/respvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respValAdd(@PathVariable String txnId, @RequestBody String xml) {
        log.info("[IMPS] RespValAdd received from NPCI (REST) txnId={}", txnId);
        log.debug("RespValAdd XML: {}", xml);
        return impsInboundService.handleRespValAdd(xml, txnId, restAckSender());
    }
}
