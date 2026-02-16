package com.hitachi.imps.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hitachi.imps.client.npci.NpciRestClient;
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
import com.hitachi.imps.config.ImpsServerDisplayInfo;

import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.util.IsoUtil;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single IMPS API – all under /imps. Dynamic paths only (no /switch).
 * NPCI → IMPS: POST /imps/{reqtype}/{txn_id} (XML). Switch → IMPS: POST /imps/{reqtype|resptype}/{txn_id} (ISO).
 * Flow: NPCI → IMPS (ACK) → IMPS → Switch; Switch → IMPS /imps/{resptype}/{txn_id} → IMPS → NPCI.
 */
@RestController
@RequestMapping("/imps")
public class ImpsController {

    private static final Logger log = LoggerFactory.getLogger(ImpsController.class);

    @Autowired
    private ImpsInboundService impsInboundService;
    @Autowired
    private NpciRestClient npciRestClient;
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
    @Autowired
    private ImpsServerDisplayInfo impsServerDisplay;

    private AckSender restAckSender() {
        return ack -> npciRestClient.sendAck(ack);
    }

    // ---------- NPCI → IMPS (XML) ----------
    @PostMapping(value = "/reqpay/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqpay(@PathVariable String txnId, @RequestBody String xml) throws ReqPayValidationException {
        log.info("[IMPS] REQUEST FROM NPCI (HTTP) | ReqPay | TxnId: {}", txnId);
        log.debug("ReqPay XML: {}", xml);
        String response = impsInboundService.handleReqPay(xml, txnId, restAckSender());
        log.info("[IMPS] ACK sent to NPCI for ReqPay txnId={}", txnId);
        return response;
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqchktxn(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        log.info("[IMPS] REQUEST FROM NPCI (HTTP) | ReqChkTxn | TxnId: {}", txnId);
        log.debug("ReqChkTxn XML: {}", xml);
        String response = impsInboundService.handleReqChkTxn(xml, txnId, restAckSender());
        log.info("[IMPS] ACK sent to NPCI for ReqChkTxn txnId={}", txnId);
        return response;
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqhbt(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        log.info("[IMPS] REQUEST FROM NPCI (HTTP) | ReqHbt | TxnId: {}", txnId);
        log.debug("ReqHbt XML: {}", xml);
        String response = impsInboundService.handleReqHbt(xml, txnId, restAckSender());
        log.info("[IMPS] ACK sent to NPCI for ReqHbt txnId={}", txnId);
        return response;
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqlistaccpvd(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        log.info("[IMPS] REQUEST FROM NPCI (HTTP) | ReqListAccPvd | TxnId: {}", txnId);
        log.debug("ReqListAccPvd XML: {}", xml);
        String response = impsInboundService.handleReqListAccPvd(xml, txnId, restAckSender());
        log.info("[IMPS] ACK sent to NPCI for ReqListAccPvd txnId={}", txnId);
        return response;
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqvaladd(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        log.info("[IMPS] REQUEST FROM NPCI (HTTP) | ReqValAdd | TxnId: {}", txnId);
        log.debug("ReqValAdd XML: {}", xml);
        String response = impsInboundService.handleReqValAdd(xml, txnId, restAckSender());
        log.info("[IMPS] ACK sent to NPCI for ReqValAdd txnId={}", txnId);
        return response;
    }

    // ---------- Switch → IMPS (ISO, Req*): reverse flow – Switch sends Req ISO, IMPS forwards to NPCI, returns Resp ISO -----
    private static final String ISO_BODY_REQUIRED = "Request body required (binary ISO 8583). Use Content-Type: application/octet-stream and send ISO bytes. For XML flow use Content-Type: application/xml.";

    @PostMapping(value = "/reqpay/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqpayIso(@PathVariable String txnId, @RequestBody(required = false) byte[] isoBytes) {
        if (isoBytes == null || isoBytes.length == 0)
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ISO_BODY_REQUIRED.getBytes(StandardCharsets.UTF_8));
        log.info("[IMPS] Switch → IMPS | REQ | ReqPay | TxnId: {} | Length: {} bytes", txnId, isoBytes.length);
        log.debug("ReqPay ISO: {}", formatIsoForConsole(isoBytes));
        byte[] respIso = reqPayService.processFromSwitchSync(isoBytes, txnId);
        if (respIso != null && respIso.length > 0) {
            log.info("[IMPS] IMPS → Switch | RESP | RespPay | TxnId: {} | Length: {} bytes", txnId, respIso.length);
            log.debug("RespPay ISO: {}", formatIsoForConsole(respIso));
        }
        return ResponseEntity.ok(orIsoAck(respIso, isoBytes));
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqchktxnIso(@PathVariable String txnId, @RequestBody(required = false) byte[] isoBytes) {
        if (isoBytes == null || isoBytes.length == 0)
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ISO_BODY_REQUIRED.getBytes(StandardCharsets.UTF_8));
        log.info("[IMPS] Switch → IMPS | REQ | ReqChkTxn | TxnId: {} | Length: {} bytes", txnId, isoBytes.length);
        log.debug("ReqChkTxn ISO: {}", formatIsoForConsole(isoBytes));
        byte[] respIso = reqChkTxnService.processFromSwitchSync(isoBytes, txnId);
        if (respIso != null && respIso.length > 0) {
            log.info("[IMPS] IMPS → Switch | RESP | RespChkTxn | TxnId: {} | Length: {} bytes", txnId, respIso.length);
            log.debug("RespChkTxn ISO: {}", formatIsoForConsole(respIso));
        }
        return ResponseEntity.ok(orIsoAck(respIso, isoBytes));
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqhbtIso(@PathVariable String txnId, @RequestBody(required = false) byte[] isoBytes) {
        if (isoBytes == null || isoBytes.length == 0)
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ISO_BODY_REQUIRED.getBytes(StandardCharsets.UTF_8));
        log.info("[IMPS] Switch → IMPS | REQ | ReqHbt | TxnId: {} | Length: {} bytes", txnId, isoBytes.length);
        log.debug("ReqHbt ISO: {}", formatIsoForConsole(isoBytes));
        byte[] respIso = reqHbtService.processFromSwitch(isoBytes, txnId);
        if (respIso != null && respIso.length > 0) {
            log.info("[IMPS] IMPS → Switch | RESP | RespHbt | TxnId: {} | Length: {} bytes", txnId, respIso.length);
            log.debug("RespHbt ISO: {}", formatIsoForConsole(respIso));
        }
        return ResponseEntity.ok(respIso != null ? respIso : new byte[0]);
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqlistaccpvdIso(@PathVariable String txnId, @RequestBody(required = false) byte[] isoBytes) {
        if (isoBytes == null || isoBytes.length == 0)
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ISO_BODY_REQUIRED.getBytes(StandardCharsets.UTF_8));
        log.info("[IMPS] Switch → IMPS | REQ | ReqListAccPvd | TxnId: {} | Length: {} bytes", txnId, isoBytes.length);
        log.debug("ReqListAccPvd ISO: {}", formatIsoForConsole(isoBytes));
        byte[] respIso = reqListAccPvdService.processFromSwitchSync(isoBytes, txnId);
        if (respIso != null && respIso.length > 0) {
            log.info("[IMPS] IMPS → Switch | RESP | RespListAccPvd | TxnId: {} | Length: {} bytes", txnId, respIso.length);
            log.debug("RespListAccPvd ISO: {}", formatIsoForConsole(respIso));
        }
        return ResponseEntity.ok(orIsoAck(respIso, isoBytes));
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqvaladdIso(@PathVariable String txnId, @RequestBody(required = false) byte[] isoBytes) {
        if (isoBytes == null || isoBytes.length == 0)
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ISO_BODY_REQUIRED.getBytes(StandardCharsets.UTF_8));
        log.info("[IMPS] Switch → IMPS | REQ | ReqValAdd | TxnId: {} | Length: {} bytes", txnId, isoBytes.length);
        log.debug("ReqValAdd ISO: {}", formatIsoForConsole(isoBytes));
        byte[] respIso = reqValAddService.processFromSwitchSync(isoBytes, txnId);
        if (respIso != null && respIso.length > 0) {
            log.info("[IMPS] IMPS → Switch | RESP | RespValAdd | TxnId: {} | Length: {} bytes", txnId, respIso.length);
            log.debug("RespValAdd ISO: {}", formatIsoForConsole(respIso));
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
        log.info("[IMPS] Switch → IMPS | RESP received | RespPay | TxnId: {} | Length: {} bytes", txnId, isoBytes.length);
        log.debug("RespPay ISO: {}", formatIsoForConsole(isoBytes));
        respPayService.processAsync(isoBytes, txnId);
        byte[] isoAck = ackService.buildIsoAckFromResponse(isoBytes);
        return isoAck != null ? isoAck : new byte[0];
    }

    @PostMapping(value = "/respchktxn/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] respchktxn(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        log.info("[IMPS] Switch → IMPS | RESP received | RespChkTxn | TxnId: {} | Length: {} bytes", txnId, isoBytes.length);
        log.debug("RespChkTxn ISO: {}", formatIsoForConsole(isoBytes));
        respChkTxnService.processAsync(isoBytes, txnId);
        byte[] isoAck = ackService.buildIsoAckFromResponse(isoBytes);
        return isoAck != null ? isoAck : new byte[0];
    }

    @PostMapping(value = "/resplistaccpvd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] resplistaccpvd(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        log.info("[IMPS] Switch → IMPS | RESP received | RespListAccPvd | TxnId: {} | Length: {} bytes", txnId, isoBytes.length);
        log.debug("RespListAccPvd ISO: {}", formatIsoForConsole(isoBytes));
        respListAccPvdService.processAsync(isoBytes, txnId);
        byte[] isoAck = ackService.buildIsoAckFromResponse(isoBytes);
        return isoAck != null ? isoAck : new byte[0];
    }

    @PostMapping(value = "/respvaladd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] respvaladd(@PathVariable String txnId, @RequestBody byte[] isoBytes) {
        log.info("[IMPS] Switch → IMPS | RESP received | RespValAdd | TxnId: {} | Length: {} bytes", txnId, isoBytes.length);
        log.debug("RespValAdd ISO: {}", formatIsoForConsole(isoBytes));
        respValAddService.processAsync(isoBytes, txnId);
        byte[] isoAck = ackService.buildIsoAckFromResponse(isoBytes);
        return isoAck != null ? isoAck : new byte[0];
    }

    private static String formatIsoForConsole(byte[] data) {
        try {
            ISOMsg iso = IsoUtil.unpack(data, new ImpsIsoPackager());
            return com.hitachi.imps.util.Iso8583PrettyFormatter.format(iso);
        } catch (Exception e) {
            // Log hex of first 60 bytes to diagnose corrupted body (e.g. UTF-8 re-encoding)
            if (data != null) {
                StringBuilder hex = new StringBuilder();
                int len = Math.min(60, data.length);
                for (int i = 0; i < len; i++) {
                    hex.append(String.format("%02X ", data[i] & 0xFF));
                }
                log.warn("[IMPS] ISO unpack failed. First {} bytes (hex): {} | error: {}", len, hex.toString().trim(), e.getMessage());
            }
            return "ISO parse failed: " + e.getMessage();
        }
    }
}
