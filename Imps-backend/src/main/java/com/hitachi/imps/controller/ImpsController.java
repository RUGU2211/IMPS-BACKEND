package com.hitachi.imps.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hitachi.imps.client.npci.NpciMockClient;
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
        System.out.println("==========================================");
        System.out.println("[IMPS] REQUEST FROM NPCI (HTTP)");
        System.out.println("Message Type: ReqPay | TxnId: " + txnId + " | Format: XML");
        System.out.println("==========================================");
        System.out.println(xml);
        String response = impsInboundService.handleReqPay(xml, txnId, restAckSender());
        System.out.println("[IMPS] ACK sent to NPCI for ReqPay txnId=" + txnId);
        return response;
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqchktxn(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("==========================================");
        System.out.println("[IMPS] REQUEST FROM NPCI (HTTP)");
        System.out.println("Message Type: ReqChkTxn | TxnId: " + txnId + " | Format: XML");
        System.out.println("==========================================");
        System.out.println(xml);
        String response = impsInboundService.handleReqChkTxn(xml, txnId, restAckSender());
        System.out.println("[IMPS] ACK sent to NPCI for ReqChkTxn txnId=" + txnId);
        return response;
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqhbt(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("==========================================");
        System.out.println("[IMPS] REQUEST FROM NPCI (HTTP)");
        System.out.println("Message Type: ReqHbt | TxnId: " + txnId + " | Format: XML");
        System.out.println("==========================================");
        System.out.println(xml);
        String response = impsInboundService.handleReqHbt(xml, txnId, restAckSender());
        System.out.println("[IMPS] ACK sent to NPCI for ReqHbt txnId=" + txnId);
        return response;
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqlistaccpvd(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("==========================================");
        System.out.println("[IMPS] REQUEST FROM NPCI (HTTP)");
        System.out.println("Message Type: ReqListAccPvd | TxnId: " + txnId + " | Format: XML");
        System.out.println("==========================================");
        System.out.println(xml);
        String response = impsInboundService.handleReqListAccPvd(xml, txnId, restAckSender());
        System.out.println("[IMPS] ACK sent to NPCI for ReqListAccPvd txnId=" + txnId);
        return response;
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqvaladd(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("==========================================");
        System.out.println("[IMPS] REQUEST FROM NPCI (HTTP)");
        System.out.println("Message Type: ReqValAdd | TxnId: " + txnId + " | Format: XML");
        System.out.println("==========================================");
        System.out.println(xml);
        String response = impsInboundService.handleReqValAdd(xml, txnId, restAckSender());
        System.out.println("[IMPS] ACK sent to NPCI for ReqValAdd txnId=" + txnId);
        return response;
    }

    // ---------- Switch → IMPS (ISO, Req*): reverse flow – Switch sends Req ISO, IMPS forwards to NPCI, returns Resp ISO -----
    private static final String ISO_BODY_REQUIRED = "Request body required (binary ISO 8583). Use Content-Type: application/octet-stream and send ISO bytes. For XML flow use Content-Type: application/xml.";

    @PostMapping(value = "/reqpay/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqpayIso(@PathVariable String txnId, @RequestBody(required = false) byte[] isoBytes) {
        if (isoBytes == null || isoBytes.length == 0)
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ISO_BODY_REQUIRED.getBytes(StandardCharsets.UTF_8));
        System.out.println("==========================================");
        System.out.println("[IMPS] REQUEST FROM SWITCH (HTTP)");
        System.out.println("Message Type: ReqPay | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes | Format: ISO");
        System.out.println("==========================================");
        System.out.println(formatIsoForConsole(isoBytes));
        byte[] respIso = reqPayService.processFromSwitchSync(isoBytes, txnId);
        if (respIso != null && respIso.length > 0) {
            System.out.println("==========================================");
            System.out.println("[IMPS] RESPONSE TO SWITCH (HTTP)");
            System.out.println("Message Type: RespPay | TxnId: " + txnId + " | Length: " + respIso.length + " bytes | Format: ISO");
            System.out.println("==========================================");
            System.out.println(formatIsoForConsole(respIso));
        }
        return ResponseEntity.ok(orIsoAck(respIso, isoBytes));
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqchktxnIso(@PathVariable String txnId, @RequestBody(required = false) byte[] isoBytes) {
        if (isoBytes == null || isoBytes.length == 0)
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ISO_BODY_REQUIRED.getBytes(StandardCharsets.UTF_8));
        System.out.println("==========================================");
        System.out.println("[IMPS] REQUEST FROM SWITCH (HTTP)");
        System.out.println("Message Type: ReqChkTxn | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes | Format: ISO");
        System.out.println("==========================================");
        System.out.println(formatIsoForConsole(isoBytes));
        byte[] respIso = reqChkTxnService.processFromSwitchSync(isoBytes, txnId);
        if (respIso != null && respIso.length > 0) {
            System.out.println("==========================================");
            System.out.println("[IMPS] RESPONSE TO SWITCH (HTTP)");
            System.out.println("Message Type: RespChkTxn | TxnId: " + txnId + " | Length: " + respIso.length + " bytes | Format: ISO");
            System.out.println("==========================================");
            System.out.println(formatIsoForConsole(respIso));
        }
        return ResponseEntity.ok(orIsoAck(respIso, isoBytes));
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqhbtIso(@PathVariable String txnId, @RequestBody(required = false) byte[] isoBytes) {
        if (isoBytes == null || isoBytes.length == 0)
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ISO_BODY_REQUIRED.getBytes(StandardCharsets.UTF_8));
        System.out.println("==========================================");
        System.out.println("[IMPS] REQUEST FROM SWITCH (HTTP)");
        System.out.println("Message Type: ReqHbt | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes | Format: ISO");
        System.out.println("==========================================");
        System.out.println(formatIsoForConsole(isoBytes));
        byte[] respIso = reqHbtService.processFromSwitch(isoBytes, txnId);
        if (respIso != null && respIso.length > 0) {
            System.out.println("==========================================");
            System.out.println("[IMPS] RESPONSE TO SWITCH (HTTP)");
            System.out.println("Message Type: RespHbt | TxnId: " + txnId + " | Length: " + respIso.length + " bytes | Format: ISO");
            System.out.println("==========================================");
            System.out.println(formatIsoForConsole(respIso));
        }
        return ResponseEntity.ok(respIso != null ? respIso : new byte[0]);
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqlistaccpvdIso(@PathVariable String txnId, @RequestBody(required = false) byte[] isoBytes) {
        if (isoBytes == null || isoBytes.length == 0)
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ISO_BODY_REQUIRED.getBytes(StandardCharsets.UTF_8));
        System.out.println("==========================================");
        System.out.println("[IMPS] REQUEST FROM SWITCH (HTTP)");
        System.out.println("Message Type: ReqListAccPvd | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes | Format: ISO");
        System.out.println("==========================================");
        System.out.println(formatIsoForConsole(isoBytes));
        byte[] respIso = reqListAccPvdService.processFromSwitchSync(isoBytes, txnId);
        if (respIso != null && respIso.length > 0) {
            System.out.println("==========================================");
            System.out.println("[IMPS] RESPONSE TO SWITCH (HTTP)");
            System.out.println("Message Type: RespListAccPvd | TxnId: " + txnId + " | Length: " + respIso.length + " bytes | Format: ISO");
            System.out.println("==========================================");
            System.out.println(formatIsoForConsole(respIso));
        }
        return ResponseEntity.ok(orIsoAck(respIso, isoBytes));
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqvaladdIso(@PathVariable String txnId, @RequestBody(required = false) byte[] isoBytes) {
        if (isoBytes == null || isoBytes.length == 0)
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ISO_BODY_REQUIRED.getBytes(StandardCharsets.UTF_8));
        System.out.println("==========================================");
        System.out.println("[IMPS] REQUEST FROM SWITCH (HTTP)");
        System.out.println("Message Type: ReqValAdd | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes | Format: ISO");
        System.out.println("==========================================");
        System.out.println(formatIsoForConsole(isoBytes));
        byte[] respIso = reqValAddService.processFromSwitchSync(isoBytes, txnId);
        if (respIso != null && respIso.length > 0) {
            System.out.println("==========================================");
            System.out.println("[IMPS] RESPONSE TO SWITCH (HTTP)");
            System.out.println("Message Type: RespValAdd | TxnId: " + txnId + " | Length: " + respIso.length + " bytes | Format: ISO");
            System.out.println("==========================================");
            System.out.println(formatIsoForConsole(respIso));
        }
        return ResponseEntity.ok(orIsoAck(respIso, isoBytes));
    }

    private byte[] orIsoAck(byte[] respIso, byte[] reqIso) {
        if (respIso != null && respIso.length > 0) return respIso;
        byte[] ack = ackService.buildIsoAckFromResponse(reqIso);
        return ack != null ? ack : new byte[0];
    }

    // ---------- Switch → IMPS (ISO, Resp*): receive ISO response from Switch, process it, return ISO ACK ----------
    @PostMapping(value = "/resppay/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] resppay(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("==========================================");
        System.out.println("[IMPS] RESPONSE FROM SWITCH (HTTP)");
        System.out.println("Message Type: RespPay | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes | Format: ISO");
        System.out.println("==========================================");
        System.out.println(formatIsoForConsole(isoBytes));
        System.out.println("[IMPS] Processing RespPay - will update transaction and send to NPCI");
        respPayService.processAsync(isoBytes, txnId);
        byte[] isoAck = ackService.buildIsoAckFromResponse(isoBytes);
        System.out.println("[IMPS] ISO ACK sent to Switch for RespPay txnId=" + txnId);
        return isoAck != null ? isoAck : new byte[0];
    }

    @PostMapping(value = "/respchktxn/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] respchktxn(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("==========================================");
        System.out.println("[IMPS] RESPONSE FROM SWITCH (HTTP)");
        System.out.println("Message Type: RespChkTxn | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes | Format: ISO");
        System.out.println("==========================================");
        System.out.println(formatIsoForConsole(isoBytes));
        System.out.println("[IMPS] Processing RespChkTxn - will update transaction and send to NPCI");
        respChkTxnService.processAsync(isoBytes, txnId);
        byte[] isoAck = ackService.buildIsoAckFromResponse(isoBytes);
        System.out.println("[IMPS] ISO ACK sent to Switch for RespChkTxn txnId=" + txnId);
        return isoAck != null ? isoAck : new byte[0];
    }

    @PostMapping(value = "/resplistaccpvd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] resplistaccpvd(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("==========================================");
        System.out.println("[IMPS] RESPONSE FROM SWITCH (HTTP)");
        System.out.println("Message Type: RespListAccPvd | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes | Format: ISO");
        System.out.println("==========================================");
        System.out.println(formatIsoForConsole(isoBytes));
        System.out.println("[IMPS] Processing RespListAccPvd - will update transaction and send to NPCI");
        respListAccPvdService.processAsync(isoBytes, txnId);
        byte[] isoAck = ackService.buildIsoAckFromResponse(isoBytes);
        System.out.println("[IMPS] ISO ACK sent to Switch for RespListAccPvd txnId=" + txnId);
        return isoAck != null ? isoAck : new byte[0];
    }

    @PostMapping(value = "/respvaladd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] respvaladd(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        System.out.println("==========================================");
        System.out.println("[IMPS] RESPONSE FROM SWITCH (HTTP)");
        System.out.println("Message Type: RespValAdd | TxnId: " + txnId + " | Length: " + isoBytes.length + " bytes | Format: ISO");
        System.out.println("==========================================");
        System.out.println(formatIsoForConsole(isoBytes));
        System.out.println("[IMPS] Processing RespValAdd - will update transaction and send to NPCI");
        respValAddService.processAsync(isoBytes, txnId);
        byte[] isoAck = ackService.buildIsoAckFromResponse(isoBytes);
        System.out.println("[IMPS] ISO ACK sent to Switch for RespValAdd txnId=" + txnId);
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
            // Log hex of first 60 bytes to diagnose corrupted body (e.g. UTF-8 re-encoding)
            if (data != null) {
                StringBuilder hex = new StringBuilder();
                int len = Math.min(60, data.length);
                for (int i = 0; i < len; i++) {
                    hex.append(String.format("%02X ", data[i] & 0xFF));
                }
                System.err.println("[IMPS] ISO unpack failed. First " + len + " bytes (hex): " + hex.toString().trim());
            }
            return "ISO parse failed: " + e.getMessage();
        }
    }
}
