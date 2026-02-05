package com.hitachi.imps.controller.npci;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.XmlParsingService;
import com.hitachi.imps.service.pay.reqpay.NpciReqPayService;
import com.hitachi.imps.service.pay.resppay.NpciRespPayService;
import com.hitachi.imps.service.chktxn.reqchktxn.NpciReqChkTxnService;
import com.hitachi.imps.service.chktxn.respchktxn.NpciRespChkTxnService;
import com.hitachi.imps.service.heartbeat.reqhbt.NpciReqHbtService;
import com.hitachi.imps.service.heartbeat.resphbt.NpciRespHbtService;
import com.hitachi.imps.service.listaccpvd.reqlistaccpvd.NpciReqListAccPvdService;
import com.hitachi.imps.service.listaccpvd.resplistaccpvd.NpciRespListAccPvdService;
import com.hitachi.imps.service.valadd.reqvaladd.NpciReqValAddService;
import com.hitachi.imps.service.valadd.respvaladd.NpciRespValAddService;
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
    @Autowired private NpciReqPayService reqPayService;
    @Autowired private NpciRespPayService respPayService;
    @Autowired private NpciReqChkTxnService reqChkTxnService;
    @Autowired private NpciRespChkTxnService respChkTxnService;
    @Autowired private NpciReqHbtService reqHbtService;
    @Autowired private NpciRespHbtService respHbtService;
    @Autowired private NpciReqListAccPvdService reqListAccPvdService;
    @Autowired private NpciRespListAccPvdService respListAccPvdService;
    @Autowired private NpciReqValAddService reqValAddService;
    @Autowired private NpciRespValAddService respValAddService;

    @PostMapping(value = "/reqpay/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqPay(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("imps reqpay receive");
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqPay", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        System.out.println("ack send to npci");
        reqPayService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/resppay/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respPay(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("=== NPCI RESPPAY RECEIVED txnId=" + txnId + " ===");
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespPay", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        respPayService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqChkTxn(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("imps reqchktxn receive");
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqChkTxn", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        System.out.println("ack send to npci");
        reqChkTxnService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/respchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respChkTxn(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("=== NPCI RESPCHKTXN RECEIVED txnId=" + txnId + " ===");
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespChkTxn", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        respChkTxnService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqHbt(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("imps reqhbt receive");
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqHbt", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        System.out.println("ack send to npci");
        reqHbtService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/resphbt/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respHbt(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("=== NPCI RESPHBT RECEIVED txnId=" + txnId + " ===");
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespHbt", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        respHbtService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqListAccPvd(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("imps reqlistaccpvd receive");
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqListAccPvd", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        System.out.println("ack send to npci");
        reqListAccPvdService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/resplistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respListAccPvd(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("=== NPCI RESPLISTACCPVD RECEIVED txnId=" + txnId + " ===");
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespListAccPvd", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        respListAccPvdService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqValAdd(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("imps reqvaladd receive");
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("ReqValAdd", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        System.out.println("ack send to npci");
        reqValAddService.processAsync(xml, txnId);
        return ack;
    }

    @PostMapping(value = "/respvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respValAdd(@PathVariable String txnId, @RequestBody String xml) {
        System.out.println("=== NPCI RESPVALADD RECEIVED txnId=" + txnId + " ===");
        String msgId = xmlParsingService.extractMsgId(xml);
        String ack = ackService.buildAck("RespValAdd", msgId);
        npciMockClient.sendAckToNpciMock(ack);
        respValAddService.processAsync(xml, txnId);
        return ack;
    }
}
