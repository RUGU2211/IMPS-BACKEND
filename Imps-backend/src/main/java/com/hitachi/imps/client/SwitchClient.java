package com.hitachi.imps.client;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.hitachi.imps.config.RoutingConfig;
import com.hitachi.imps.util.IsoUtil;

/**
 * Client for sending ISO 8583 messages to Switch endpoints.
 * Handles all request forwards to Switch after processing NPCI requests.
 */
@Component
public class SwitchClient {

    @Autowired
    private RoutingConfig routingConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    /* ===============================
       GENERIC SEND METHOD (byte[])
       =============================== */
    public byte[] send(String endpointKey, byte[] isoBytes) {
        try {
            String url = routingConfig.getSwitch().getFullUrl(endpointKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> request = new HttpEntity<>(isoBytes, headers);

            byte[] response = restTemplate.postForObject(url, request, byte[].class);

            System.out.println("=== SWITCH RESPONSE FROM [" + endpointKey + "] ===");
            System.out.println("Response length: " + (response != null ? response.length : 0) + " bytes");
            System.out.println("================================================");

            return response;

        } catch (Exception e) {
            System.err.println("SWITCH SEND FAILED [" + endpointKey + "]: " + e.getMessage());
            return null;
        }
    }

    /* ===============================
       SEND ISOMsg (Convenience Method)
       =============================== */
    public byte[] send(String endpointKey, ISOMsg iso) {
        return send(endpointKey, IsoUtil.pack(iso));
    }

    /* ===============================
       SPECIFIC ENDPOINT METHODS
       =============================== */

    /**
     * Send ReqPay ISO to Switch (0200)
     */
    public byte[] sendReqPay(ISOMsg iso) {
        return send("reqpay", iso);
    }

    public byte[] sendReqPay(byte[] isoBytes) {
        return send("reqpay", isoBytes);
    }

    /**
     * Send RespPay ISO to Switch (0210)
     */
    public byte[] sendRespPay(ISOMsg iso) {
        return send("resppay", iso);
    }

    public byte[] sendRespPay(byte[] isoBytes) {
        return send("resppay", isoBytes);
    }

    /**
     * Send ReqChkTxn ISO to Switch
     */
    public byte[] sendReqChkTxn(ISOMsg iso) {
        return send("reqchktxn", iso);
    }

    public byte[] sendReqChkTxn(byte[] isoBytes) {
        return send("reqchktxn", isoBytes);
    }

    /**
     * Send RespChkTxn ISO to Switch
     */
    public byte[] sendRespChkTxn(ISOMsg iso) {
        return send("respchktxn", iso);
    }

    public byte[] sendRespChkTxn(byte[] isoBytes) {
        return send("respchktxn", isoBytes);
    }

    /**
     * Send ReqHbt ISO to Switch (0800)
     */
    public byte[] sendReqHbt(ISOMsg iso) {
        return send("reqhbt", iso);
    }

    public byte[] sendReqHbt(byte[] isoBytes) {
        return send("reqhbt", isoBytes);
    }

    /**
     * Send RespHbt ISO to Switch (0810)
     */
    public byte[] sendRespHbt(ISOMsg iso) {
        return send("resphbt", iso);
    }

    public byte[] sendRespHbt(byte[] isoBytes) {
        return send("resphbt", isoBytes);
    }

    /**
     * Send ReqListAccPvd ISO to Switch
     */
    public byte[] sendReqListAccPvd(ISOMsg iso) {
        return send("reqlistaccpvd", iso);
    }

    public byte[] sendReqListAccPvd(byte[] isoBytes) {
        return send("reqlistaccpvd", isoBytes);
    }

    /**
     * Send RespListAccPvd ISO to Switch
     */
    public byte[] sendRespListAccPvd(ISOMsg iso) {
        return send("resplistaccpvd", iso);
    }

    public byte[] sendRespListAccPvd(byte[] isoBytes) {
        return send("resplistaccpvd", isoBytes);
    }

    /**
     * Send ReqValAdd ISO to Switch
     */
    public byte[] sendReqValAdd(ISOMsg iso) {
        return send("reqvaladd", iso);
    }

    public byte[] sendReqValAdd(byte[] isoBytes) {
        return send("reqvaladd", isoBytes);
    }

    /**
     * Send RespValAdd ISO to Switch
     */
    public byte[] sendRespValAdd(ISOMsg iso) {
        return send("respvaladd", iso);
    }

    public byte[] sendRespValAdd(byte[] isoBytes) {
        return send("respvaladd", isoBytes);
    }
}
