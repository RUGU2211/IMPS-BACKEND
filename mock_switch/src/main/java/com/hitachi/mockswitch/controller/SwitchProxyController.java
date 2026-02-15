package com.hitachi.mockswitch.controller;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import com.hitachi.mockswitch.service.SwitchProxyService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Proxy for client (as Switch) → IMPS.
 * Reads body from request input stream so binary (application/octet-stream) is never lost.
 * Path: /switch/proxy/imps/{reqpay|reqchktxn|reqhbt|reqlistaccpvd|reqvaladd}/{txnId}
 */
@RestController
@RequestMapping("/switch/proxy/imps")
public class SwitchProxyController {

    @Autowired
    private SwitchProxyService proxyService;

    private static byte[] readRequestBody(HttpServletRequest request) throws IOException {
        try (InputStream in = request.getInputStream()) {
            return in.readAllBytes();
        }
    }

    @PostMapping(value = "/reqpay/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqpay(@PathVariable String txnId, HttpServletRequest request) throws IOException {
        byte[] body = readRequestBody(request);
        if (body == null || body.length == 0) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN)
                .body("Request body required (binary ISO). Use Body → binary and select iso_reqpay.bin.".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            byte[] resp = proxyService.forwardToImps("reqpay", txnId, body, "ReqPay");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resp);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.TEXT_PLAIN)
                .body(e.getResponseBodyAsByteArray());
        }
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqchktxn(@PathVariable String txnId, HttpServletRequest request) throws IOException {
        byte[] body = readRequestBody(request);
        if (body == null || body.length == 0) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN)
                .body("Request body required (binary ISO). Body → binary, select iso_reqchktxn.bin.".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            byte[] resp = proxyService.forwardToImps("reqchktxn", txnId, body, "ReqChkTxn");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resp);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.TEXT_PLAIN).body(e.getResponseBodyAsByteArray());
        }
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqhbt(@PathVariable String txnId, HttpServletRequest request) throws IOException {
        byte[] body = readRequestBody(request);
        if (body == null || body.length == 0) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN)
                .body("Request body required (binary ISO). Body → binary, select iso_reqhbt.bin.".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            byte[] resp = proxyService.forwardToImps("reqhbt", txnId, body, "ReqHbt");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resp);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.TEXT_PLAIN).body(e.getResponseBodyAsByteArray());
        }
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqlistaccpvd(@PathVariable String txnId, HttpServletRequest request) throws IOException {
        byte[] body = readRequestBody(request);
        if (body == null || body.length == 0) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN)
                .body("Request body required (binary ISO). Body → binary, select iso_reqlistaccpvd.bin.".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            byte[] resp = proxyService.forwardToImps("reqlistaccpvd", txnId, body, "ReqListAccPvd");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resp);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.TEXT_PLAIN).body(e.getResponseBodyAsByteArray());
        }
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> reqvaladd(@PathVariable String txnId, HttpServletRequest request) throws IOException {
        byte[] body = readRequestBody(request);
        if (body == null || body.length == 0) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN)
                .body("Request body required (binary ISO). Body → binary, select iso_reqvaladd.bin.".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            byte[] resp = proxyService.forwardToImps("reqvaladd", txnId, body, "ReqValAdd");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resp);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.TEXT_PLAIN).body(e.getResponseBodyAsByteArray());
        }
    }
}
