package com.hitachi.imps.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.hitachi.imps.config.RoutingConfig;

/**
 * Client for sending XML messages to NPCI Mock Client (port 8083).
 * Replaces NpciClient - all NPCI communication now goes through mock client.
 */
@Component
public class NpciMockClient {

    @Autowired
    private RoutingConfig routingConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    /* ===============================
       GENERIC SEND METHOD
       =============================== */
    public String send(String endpointKey, String xml) {
        try {
            String url = routingConfig.getNpci().getFullUrl(endpointKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);

            HttpEntity<String> request = new HttpEntity<>(xml, headers);

            String response = restTemplate.postForObject(url, request, String.class);

            System.out.println("=== NPCI MOCK CLIENT RESPONSE FROM [" + endpointKey + "] ===");
            System.out.println(response);
            System.out.println("==============================================");

            return response;

        } catch (Exception e) {
            System.err.println("NPCI MOCK CLIENT SEND FAILED [" + endpointKey + "]: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /* ===============================
       SPECIFIC ENDPOINT METHODS
       =============================== */

    /**
     * Send ReqPay XML to NPCI Mock Client
     */
    public String sendReqPay(String xml) {
        return send("reqpay", xml);
    }

    /**
     * Send RespPay XML to NPCI Mock Client
     */
    public String sendRespPay(String xml) {
        return send("resppay", xml);
    }

    /**
     * Send ReqChkTxn XML to NPCI Mock Client
     */
    public String sendReqChkTxn(String xml) {
        return send("reqchktxn", xml);
    }

    /**
     * Send RespChkTxn XML to NPCI Mock Client
     */
    public String sendRespChkTxn(String xml) {
        return send("respchktxn", xml);
    }

    /**
     * Send ReqHbt XML to NPCI Mock Client
     */
    public String sendReqHbt(String xml) {
        return send("reqhbt", xml);
    }

    /**
     * Send RespHbt XML to NPCI Mock Client
     */
    public String sendRespHbt(String xml) {
        return send("resphbt", xml);
    }

    /**
     * Send ReqListAccPvd XML to NPCI Mock Client
     */
    public String sendReqListAccPvd(String xml) {
        return send("reqlistaccpvd", xml);
    }

    /**
     * Send RespListAccPvd XML to NPCI Mock Client
     */
    public String sendRespListAccPvd(String xml) {
        return send("resplistaccpvd", xml);
    }

    /**
     * Send ReqValAdd XML to NPCI Mock Client
     */
    public String sendReqValAdd(String xml) {
        return send("reqvaladd", xml);
    }

    /**
     * Send RespValAdd XML to NPCI Mock Client
     */
    public String sendRespValAdd(String xml) {
        return send("respvaladd", xml);
    }
}
