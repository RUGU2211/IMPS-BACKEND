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

    // ---------- Switch → IMPS (ISO, Req*): reverse flow – Switch sends Req ISO, IMPS forwards to NPCI, returns Resp ISO -----
    @PostMapping(value = "/reqpay/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] reqpayIso(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] ReqPay ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        byte[] respIso = reqPayService.processFromSwitchSync(isoBytes, txnId);
        return orIsoAck(respIso, isoBytes);
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] reqchktxnIso(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] ReqChkTxn ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        byte[] respIso = reqChkTxnService.processFromSwitchSync(isoBytes, txnId);
        return orIsoAck(respIso, isoBytes);
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] reqhbtIso(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] ReqHbt ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        byte[] respIso = reqHbtService.processFromSwitch(isoBytes, txnId);
        return respIso != null ? respIso : new byte[0];
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] reqlistaccpvdIso(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] ReqListAccPvd ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        byte[] respIso = reqListAccPvdService.processFromSwitchSync(isoBytes, txnId);
        return orIsoAck(respIso, isoBytes);
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] reqvaladdIso(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] ReqValAdd ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        byte[] respIso = reqValAddService.processFromSwitchSync(isoBytes, txnId);
        return orIsoAck(respIso, isoBytes);
    }

    private byte[] orIsoAck(byte[] respIso, byte[] reqIso) {
        if (respIso != null && respIso.length > 0) return respIso;
        byte[] ack = ackService.buildIsoAckFromResponse(reqIso);
        return ack != null ? ack : new byte[0];
    }

    // ---------- Switch → IMPS (ISO, Resp*): receive ISO response from Switch, process it, return ISO ACK ----------
    @PostMapping(value = "/resppay/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] resppay(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] RespPay ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        respPayService.processAsync(isoBytes, txnId);
        byte[] isoAck = ackService.buildIsoAckFromResponse(isoBytes);
        return isoAck != null ? isoAck : new byte[0];
    }

    @PostMapping(value = "/respchktxn/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] respchktxn(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] RespChkTxn ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        respChkTxnService.processAsync(isoBytes, txnId);
        byte[] isoAck = ackService.buildIsoAckFromResponse(isoBytes);
        return isoAck != null ? isoAck : new byte[0];
    }

    @PostMapping(value = "/resplistaccpvd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] resplistaccpvd(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] RespListAccPvd ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        respListAccPvdService.processAsync(isoBytes, txnId);
        byte[] isoAck = ackService.buildIsoAckFromResponse(isoBytes);
        return isoAck != null ? isoAck : new byte[0];
    }

    @PostMapping(value = "/respvaladd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] respvaladd(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("[IMPS] RespValAdd ISO received from Switch txnId=" + txnId + ":");
        System.out.println(formatIsoForConsole(isoBytes));
        respValAddService.processAsync(isoBytes, txnId);
        byte[] isoAck = ackService.buildIsoAckFromResponse(isoBytes);
        return isoAck != null ? isoAck : new byte[0];
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
