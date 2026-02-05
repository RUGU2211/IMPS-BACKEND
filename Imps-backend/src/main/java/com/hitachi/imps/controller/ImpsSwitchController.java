package com.hitachi.imps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.chktxn.respchktxn.SwitchRespChkTxnService;
import com.hitachi.imps.service.heartbeat.resphbt.SwitchRespHbtService;
import com.hitachi.imps.service.listaccpvd.resplistaccpvd.SwitchRespListAccPvdService;
import com.hitachi.imps.service.pay.resppay.SwitchRespPayService;
import com.hitachi.imps.service.valadd.respvaladd.SwitchRespValAddService;

/**
 * Receives responses FROM the Switch (dynamic URL with txnId).
 * Pattern: http://localhost:8081/switch/{resppay|respchktxn|respvaladd|resphbt|resplistaccpvd}/{txn_id}
 * Switch calls these after processing; same txnId for the whole flow.
 */
@RestController
@RequestMapping("/switch")
public class ImpsSwitchController {

    @Autowired private AckService ackService;
    @Autowired private SwitchRespPayService respPayService;
    @Autowired private SwitchRespChkTxnService respChkTxnService;
    @Autowired private SwitchRespHbtService respHbtService;
    @Autowired private SwitchRespListAccPvdService respListAccPvdService;
    @Autowired private SwitchRespValAddService respValAddService;

    @PostMapping(value = "/resppay/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String resppay(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("switch resppay/" + txnId + " receive");
        String ack = ackService.buildAck("RespPay", txnId);
        System.out.println("ack send to switch of resppay/" + txnId);
        respPayService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/respchktxn/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respchktxn(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("switch respchktxn/" + txnId + " receive");
        String ack = ackService.buildAck("RespChkTxn", txnId);
        System.out.println("ack send to switch of respchktxn/" + txnId);
        respChkTxnService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/resphbt/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String resphbt(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("switch resphbt/" + txnId + " receive");
        String ack = ackService.buildAck("RespHbt", txnId);
        System.out.println("ack send to switch of resphbt/" + txnId);
        respHbtService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/resplistaccpvd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String resplistaccpvd(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("switch resplistaccpvd/" + txnId + " receive");
        String ack = ackService.buildAck("RespListAccPvd", txnId);
        System.out.println("ack send to switch of resplistaccpvd/" + txnId);
        respListAccPvdService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/respvaladd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respvaladd(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("switch respvaladd/" + txnId + " receive");
        String ack = ackService.buildAck("RespValAdd", txnId);
        System.out.println("ack send to switch of respvaladd/" + txnId);
        respValAddService.processAsync(isoBytes, txnId);
        return ack;
    }
}
