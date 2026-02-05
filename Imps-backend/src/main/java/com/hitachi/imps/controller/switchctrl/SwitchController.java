package com.hitachi.imps.controller.switchctrl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.pay.reqpay.SwitchReqPayService;
import com.hitachi.imps.service.pay.resppay.SwitchRespPayService;
import com.hitachi.imps.service.chktxn.reqchktxn.SwitchReqChkTxnService;
import com.hitachi.imps.service.chktxn.respchktxn.SwitchRespChkTxnService;
import com.hitachi.imps.service.heartbeat.reqhbt.SwitchReqHbtService;
import com.hitachi.imps.service.heartbeat.resphbt.SwitchRespHbtService;
import com.hitachi.imps.service.listaccpvd.reqlistaccpvd.SwitchReqListAccPvdService;
import com.hitachi.imps.service.listaccpvd.resplistaccpvd.SwitchRespListAccPvdService;
import com.hitachi.imps.service.valadd.reqvaladd.SwitchReqValAddService;
import com.hitachi.imps.service.valadd.respvaladd.SwitchRespValAddService;

/**
 * Handles Switch-initiated Req* (ISO) to IMPS. Dynamic paths only: /switch/reqpay/{txnId}, etc.
 * Resp* (Switch response back to IMPS) are in ImpsSwitchController.
 * Each request must use a different txn_id (e.g. each reqhbt a new txn_id).
 */
@RestController
@RequestMapping("/switch")
public class SwitchController {

    @Autowired private AckService ackService;
    @Autowired private SwitchReqPayService reqPayService;
    @Autowired private SwitchReqChkTxnService reqChkTxnService;
    @Autowired private SwitchReqHbtService reqHbtService;
    @Autowired private SwitchReqListAccPvdService reqListAccPvdService;
    @Autowired private SwitchReqValAddService reqValAddService;

    @PostMapping(value = "/reqpay/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqPay(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH REQPAY RECEIVED txnId=" + txnId + " ===");
        String ack = ackService.buildAck("ReqPay", txnId);
        reqPayService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqChkTxn(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH REQCHKTXN RECEIVED txnId=" + txnId + " ===");
        String ack = ackService.buildAck("ReqChkTxn", txnId);
        reqChkTxnService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqHbt(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH REQHBT RECEIVED txnId=" + txnId + " ===");
        String ack = ackService.buildAck("ReqHbt", txnId);
        reqHbtService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqListAccPvd(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH REQLISTACCPVD RECEIVED txnId=" + txnId + " ===");
        String ack = ackService.buildAck("ReqListAccPvd", txnId);
        reqListAccPvdService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqValAdd(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("=== SWITCH REQVALADD RECEIVED txnId=" + txnId + " ===");
        String ack = ackService.buildAck("ReqValAdd", txnId);
        reqValAddService.processAsync(isoBytes, txnId);
        return ack;
    }
}
