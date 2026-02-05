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

    /**
     * Build dynamic Switch URL: http://localhost:8082/switch/{reqpay|reqchktxn|reqvaladd|reqhbt|reqlistaccpvd}/{txn_id}
     */
    private String buildDynamicUrl(String apiType, String txnId) {
        String base = routingConfig.getSwitch().getBaseUrl();
        return base + "/switch/" + apiType + "/" + (txnId != null ? txnId : "");
    }

    /** Send to dynamic URL: /switch/{apiType}/{txnId} */
    private byte[] sendDynamic(String apiType, String txnId, byte[] isoBytes) {
        if (txnId == null || txnId.isBlank()) return null;
        try {
            String url = buildDynamicUrl(apiType, txnId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            HttpEntity<byte[]> request = new HttpEntity<>(isoBytes, headers);
            byte[] response = restTemplate.postForObject(url, request, byte[].class);
            return response;
        } catch (Exception e) {
            System.err.println("SWITCH SEND FAILED [" + apiType + "/" + txnId + "]: " + e.getMessage());
            return null;
        }
    }

    /* ===============================
       SPECIFIC ENDPOINT METHODS
       =============================== */

    /**
     * Send ReqPay ISO to Switch: POST /switch/reqpay/{txnId}
     */
    public byte[] sendReqPay(ISOMsg iso, String txnId) {
        return sendDynamic("reqpay", txnId, IsoUtil.pack(iso));
    }

    public byte[] sendReqPay(byte[] isoBytes, String txnId) {
        return sendDynamic("reqpay", txnId, isoBytes);
    }

    /**
     * Send ReqChkTxn ISO to Switch: POST /switch/reqchktxn/{txnId}
     */
    public byte[] sendReqChkTxn(ISOMsg iso, String txnId) {
        return sendDynamic("reqchktxn", txnId, IsoUtil.pack(iso));
    }

    public byte[] sendReqChkTxn(byte[] isoBytes, String txnId) {
        return sendDynamic("reqchktxn", txnId, isoBytes);
    }

    /**
     * Send ReqHbt ISO to Switch: POST /switch/reqhbt/{txnId}
     */
    public byte[] sendReqHbt(ISOMsg iso, String txnId) {
        return sendDynamic("reqhbt", txnId, IsoUtil.pack(iso));
    }

    public byte[] sendReqHbt(byte[] isoBytes, String txnId) {
        return sendDynamic("reqhbt", txnId, isoBytes);
    }

    /**
     * Send ReqListAccPvd ISO to Switch: POST /switch/reqlistaccpvd/{txnId}
     */
    public byte[] sendReqListAccPvd(ISOMsg iso, String txnId) {
        return sendDynamic("reqlistaccpvd", txnId, IsoUtil.pack(iso));
    }

    public byte[] sendReqListAccPvd(byte[] isoBytes, String txnId) {
        return sendDynamic("reqlistaccpvd", txnId, isoBytes);
    }

    /**
     * Send ReqValAdd ISO to Switch: POST /switch/reqvaladd/{txnId}
     */
    public byte[] sendReqValAdd(ISOMsg iso, String txnId) {
        return sendDynamic("reqvaladd", txnId, IsoUtil.pack(iso));
    }

    public byte[] sendReqValAdd(byte[] isoBytes, String txnId) {
        return sendDynamic("reqvaladd", txnId, isoBytes);
    }

    /**
     * Send RespPay ISO to Switch (when IMPS forwards NPCI response): POST /switch/resppay/{txnId}
     */
    public byte[] sendRespPay(ISOMsg iso, String txnId) {
        return txnId != null && !txnId.isBlank() ? sendDynamic("resppay", txnId, IsoUtil.pack(iso)) : null;
    }

    public byte[] sendRespChkTxn(ISOMsg iso, String txnId) {
        return txnId != null && !txnId.isBlank() ? sendDynamic("respchktxn", txnId, IsoUtil.pack(iso)) : null;
    }

    public byte[] sendRespHbt(ISOMsg iso, String txnId) {
        return txnId != null && !txnId.isBlank() ? sendDynamic("resphbt", txnId, IsoUtil.pack(iso)) : null;
    }

    public byte[] sendRespValAdd(ISOMsg iso, String txnId) {
        return txnId != null && !txnId.isBlank() ? sendDynamic("respvaladd", txnId, IsoUtil.pack(iso)) : null;
    }
}
