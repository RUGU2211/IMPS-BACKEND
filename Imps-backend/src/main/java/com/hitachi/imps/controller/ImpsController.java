package com.hitachi.imps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.hitachi.imps.client.NpciMockClient;
import com.hitachi.imps.exception.CommonCodeValidationException;
import com.hitachi.imps.exception.ReqPayValidationException;
import com.hitachi.imps.service.ImpsInboundService;
import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.ack.AckSender;
import com.hitachi.imps.service.chktxn.ReqChkTxnService;
import com.hitachi.imps.service.chktxn.RespChkTxnService;
import com.hitachi.imps.service.heartbeat.ReqHbtService;
import com.hitachi.imps.service.listaccpvd.ReqListAccPvdService;
import com.hitachi.imps.service.listaccpvd.RespListAccPvdService;
import com.hitachi.imps.service.pay.ReqPayService;
import com.hitachi.imps.service.pay.RespPayService;
import com.hitachi.imps.service.valadd.ReqValAddService;
import com.hitachi.imps.service.valadd.RespValAddService;

import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.util.IsoUtil;
import org.jpos.iso.ISOMsg;

/**
 * Single IMPS API – all under /imps. Dynamic paths only (no /switch).
 * NPCI → IMPS: POST /imps/{reqtype}/{txn_id} (XML). Switch → IMPS: POST /imps/{reqtype|resptype}/{txn_id} (ISO).
 * Flow: NPCI → IMPS (ACK) → IMPS → Switch; Switch → IMPS /imps/{resptype}/{txn_id} → IMPS → NPCI.
 */
@RestController
@RequestMapping("/imps")
public class ImpsController {

    @Autowired
    private ImpsInboundService impsInboundService;
    @Autowired
    private NpciMockClient npciMockClient;
    @Autowired
    private AckService ackService;
    @Autowired
    private ReqPayService reqPayService;
    @Autowired
    private RespPayService respPayService;
    @Autowired
    private ReqChkTxnService reqChkTxnService;
    @Autowired
    private RespChkTxnService respChkTxnService;
    @Autowired
    private ReqHbtService reqHbtService;
    @Autowired
    private ReqListAccPvdService reqListAccPvdService;
    @Autowired
    private RespListAccPvdService respListAccPvdService;
    @Autowired
    private ReqValAddService reqValAddService;
    @Autowired
    private RespValAddService respValAddService;

    private AckSender restAckSender() {
        return ack -> npciMockClient.sendAckToNpciMock(ack);
    }

    // ---------- NPCI → IMPS (XML) ----------
    @PostMapping(value = "/reqpay/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqpay(@PathVariable String txnId, @RequestBody String xml) throws ReqPayValidationException {
        System.out.println("[IMPS] ReqPay received from NPCI (REST):");
        System.out.println(xml);
        return impsInboundService.handleReqPay(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqchktxn(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("[IMPS] ReqChkTxn received from NPCI (REST):");
        System.out.println(xml);
        return impsInboundService.handleReqChkTxn(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqhbt(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("[IMPS] ReqHbt received from NPCI (REST):");
        System.out.println(xml);
        return impsInboundService.handleReqHbt(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqlistaccpvd(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("[IMPS] ReqListAccPvd received from NPCI (REST):");
        System.out.println(xml);
        return impsInboundService.handleReqListAccPvd(xml, txnId, restAckSender());
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqvaladd(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("[IMPS] ReqValAdd received from NPCI (REST):");
        System.out.println(xml);
        return impsInboundService.handleReqValAdd(xml, txnId, restAckSender());
    }

    // ---------- Switch → IMPS (ISO, Req*) ----------
    @PostMapping(value = "/reqpay/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqpayIso(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] ReqPay ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        String ack = ackService.buildAck("ReqPay", txnId);
        reqPayService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqchktxnIso(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] ReqChkTxn ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        String ack = ackService.buildAck("ReqChkTxn", txnId);
        reqChkTxnService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] reqhbtIso(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] ReqHbt ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        return reqHbtService.processFromSwitch(isoBytes, txnId);
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqlistaccpvdIso(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] ReqListAccPvd ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        String ack = ackService.buildAck("ReqListAccPvd", txnId);
        reqListAccPvdService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqvaladdIso(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] ReqValAdd ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        String ack = ackService.buildAck("ReqValAdd", txnId);
        reqValAddService.processAsync(isoBytes, txnId);
        return ack;
    }

    // ---------- Switch → IMPS (ISO, Resp*) ----------
    @PostMapping(value = "/resppay/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String resppay(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] RespPay ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        String ack = ackService.buildAck("RespPay", txnId);
        respPayService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/respchktxn/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respchktxn(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] RespChkTxn ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        String ack = ackService.buildAck("RespChkTxn", txnId);
        respChkTxnService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/resplistaccpvd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String resplistaccpvd(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] RespListAccPvd ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        String ack = ackService.buildAck("RespListAccPvd", txnId);
        respListAccPvdService.processAsync(isoBytes, txnId);
        return ack;
    }

    @PostMapping(value = "/respvaladd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String respvaladd(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] RespValAdd ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        String ack = ackService.buildAck("RespValAdd", txnId);
        respValAddService.processAsync(isoBytes, txnId);
        return ack;
    }

    private static String formatIsoForConsole(byte[] data) {
        try {
            ISOMsg iso = IsoUtil.unpack(data, new ImpsIsoPackager());
            StringBuilder sb = new StringBuilder();
            sb.append("MTI=").append(iso.getMTI()).append("\n");
            for (int i = 1; i <= 128; i++) {
                if (iso.hasField(i)) {
                    sb.append("DE").append(i).append("=").append(iso.getString(i)).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "ISO parse failed: " + e.getMessage();
        }
    }
}
