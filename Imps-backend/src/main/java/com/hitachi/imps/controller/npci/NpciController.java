package com.hitachi.imps.controller.npci;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.XmlParsingService;
import com.hitachi.imps.service.pay.ReqPayService;
import com.hitachi.imps.service.pay.RespPayService;
import com.hitachi.imps.service.chktxn.ReqChkTxnService;
import com.hitachi.imps.service.chktxn.RespChkTxnService;
import com.hitachi.imps.service.heartbeat.ReqHbtService;
import com.hitachi.imps.service.listaccpvd.ReqListAccPvdService;
import com.hitachi.imps.service.listaccpvd.RespListAccPvdService;
import com.hitachi.imps.service.valadd.ReqValAddService;
import com.hitachi.imps.service.valadd.RespValAddService;
import com.hitachi.imps.client.NpciMockClient;

/**
 * Handles NPCI XML requests. Dynamic paths only: /npci/{reqpay|resppay|...}/{txnId}.
 * Each request must use a unique txn_id (different per request type and per consecutive request e.g. each reqhbt).
 */
@RestController
@RequestMapping("/npci")
public class NpciController {

    @Autowired private AckService ackService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private NpciMockClient npciMockClient;
    @Autowired private ReqPayService reqPayService;
    @Autowired private RespPayService respPayService;
    @Autowired private ReqChkTxnService reqChkTxnService;
    @Autowired private RespChkTxnService respChkTxnService;
    @Autowired private ReqHbtService reqHbtService;
    @Autowired private ReqListAccPvdService reqListAccPvdService;
    @Autowired private RespListAccPvdService respListAccPvdService;
    @Autowired private ReqValAddService reqValAddService;
    @Autowired private RespValAddService respValAddService;

    @PostMapping(value = "/reqpay/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqPay(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("[IMPS] ReqPay received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqPay", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        reqPayService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/resppay/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respPay(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("[IMPS] RespPay received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespPay", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        respPayService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqChkTxn(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("[IMPS] ReqChkTxn received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqChkTxn", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        reqChkTxnService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/respchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respChkTxn(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("[IMPS] RespChkTxn received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespChkTxn", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        respChkTxnService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqHbt(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("[IMPS] ReqHbt received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqHbt", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        reqHbtService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqListAccPvd(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("[IMPS] ReqListAccPvd received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqListAccPvd", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        reqListAccPvdService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/resplistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respListAccPvd(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("[IMPS] RespListAccPvd received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespListAccPvd", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        respListAccPvdService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqValAdd(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("[IMPS] ReqValAdd received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqValAdd", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        reqValAddService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/respvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respValAdd(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("[IMPS] RespValAdd received from NPCI (REST) txnId=" + txnId + ":");
        System.out.println(xml);
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespValAdd", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        respValAddService.processAsync(xml, txnId);
        return ack;
    }
}
