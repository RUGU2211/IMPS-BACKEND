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

    /**
     * Send ACK to NPCI Mock Client so it is visible in npci_mock console.
     * Called by IMPS when a request lands; then further processing (ISO, Switch) follows.
     */
    public void sendAckToNpciMock(String ackXml) {
        try {
            String url = routingConfig.getNpci().getFullUrl("ack");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            HttpEntity<String> request = new HttpEntity<>(ackXml, headers);
            restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            System.err.println("NPCI MOCK: Send ACK failed (npci_mock may be down): " + e.getMessage());
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

    public String sendReqPay(String xml, String txnId) {
        return sendToImpsDynamic("reqpay", txnId, xml);
    }

    /**
     * Send RespPay XML to NPCI Mock Client (dynamic URL with txnId when provided)
     */
    public String sendRespPay(String xml) {
        return send("resppay", xml);
    }

    public String sendRespPay(String xml, String txnId) {
        return sendToNpciDynamic("resppay", txnId, xml);
    }

    /**
     * Send to NPCI mock at /imps/{reqpay|reqchktxn|...}/{txnId} (for Switch-initiated requests).
     */
    private String sendToImpsDynamic(String apiType, String txnId, String xml) {
        if (txnId == null || txnId.isBlank()) return send(apiType, xml);
        try {
            String url = routingConfig.getNpci().getBaseUrl() + "/imps/" + apiType + "/" + txnId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            HttpEntity<String> request = new HttpEntity<>(xml, headers);
            String response = restTemplate.postForObject(url, request, String.class);
            System.out.println("=== NPCI MOCK CLIENT RESPONSE FROM [" + apiType + "/" + txnId + "] ===");
            System.out.println(response != null ? response : "");
            return response;
        } catch (Exception e) {
            System.err.println("NPCI MOCK SEND FAILED [" + apiType + "/" + txnId + "]: " + e.getMessage());
            return null;
        }
    }

    /**
     * Build dynamic NPCI URL: http://localhost:8083/npci/{resppay|respchktxn|...}/{txn_id}
     */
    private String sendToNpciDynamic(String apiType, String txnId, String xml) {
        if (txnId == null || txnId.isBlank()) return null;
        try {
            String url = routingConfig.getNpci().getBaseUrl() + "/npci/" + apiType + "/" + txnId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            HttpEntity<String> request = new HttpEntity<>(xml, headers);
            String response = restTemplate.postForObject(url, request, String.class);
            System.out.println("=== NPCI MOCK CLIENT RESPONSE FROM [" + apiType + "/" + txnId + "] ===");
            return response;
        } catch (Exception e) {
            System.err.println("NPCI MOCK SEND FAILED [" + apiType + "/" + txnId + "]: " + e.getMessage());
            return null;
        }
    }

    /**
     * Send ReqChkTxn XML to NPCI Mock Client
     */
    public String sendReqChkTxn(String xml) {
        return send("reqchktxn", xml);
    }

    public String sendReqChkTxn(String xml, String txnId) {
        return sendToImpsDynamic("reqchktxn", txnId, xml);
    }

    /**
     * Send RespChkTxn XML to NPCI Mock: POST /npci/respchktxn/{txnId}
     */
    public String sendRespChkTxn(String xml) {
        return send("respchktxn", xml);
    }

    public String sendRespChkTxn(String xml, String txnId) {
        return sendToNpciDynamic("respchktxn", txnId, xml);
    }

    /**
     * Send ReqHbt XML to NPCI Mock Client.
     * NPCI Mock (ImpsMockController) expects POST /imps/hbt/req/{txnId}.
     */
    public String sendReqHbt(String xml) {
        String txnId = com.hitachi.imps.service.iso.XmlUtil.read(xml, "//*[local-name()='Txn']/@id");
        if (txnId == null || txnId.isBlank()) txnId = com.hitachi.imps.service.iso.XmlUtil.read(xml, "//*[local-name()='Head']/@msgId");
        return sendReqHbt(xml, txnId != null ? txnId : "HBT" + System.currentTimeMillis());
    }

    /**
     * Send ReqHbt to NPCI Mock at /imps/hbt/req/{txnId}.
     */
    public String sendReqHbt(String xml, String txnId) {
        if (txnId == null || txnId.isBlank()) return sendReqHbt(xml);
        try {
            String url = routingConfig.getNpci().getBaseUrl() + "/imps/hbt/req/" + txnId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            HttpEntity<String> request = new HttpEntity<>(xml, headers);
            String response = restTemplate.postForObject(url, request, String.class);
            System.out.println("=== NPCI MOCK CLIENT REQHBT RESPONSE [" + txnId + "] ===");
            System.out.println(response != null ? response : "");
            return response;
        } catch (Exception e) {
            System.err.println("NPCI MOCK CLIENT SEND FAILED [reqhbt/" + txnId + "]: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Send RespHbt XML to NPCI Mock: POST /npci/resphbt/{txnId}
     */
    public String sendRespHbt(String xml) {
        return send("resphbt", xml);
    }

    public String sendRespHbt(String xml, String txnId) {
        return sendToNpciDynamic("resphbt", txnId, xml);
    }

    /**
     * Send ReqListAccPvd XML to NPCI Mock Client
     */
    public String sendReqListAccPvd(String xml) {
        return send("reqlistaccpvd", xml);
    }

    /**
     * Send RespListAccPvd XML to NPCI Mock: POST /npci/resplistaccpvd/{txnId}
     */
    public String sendRespListAccPvd(String xml) {
        return send("resplistaccpvd", xml);
    }

    public String sendRespListAccPvd(String xml, String txnId) {
        return sendToNpciDynamic("resplistaccpvd", txnId, xml);
    }

    /**
     * Send ReqValAdd XML to NPCI Mock Client
     */
    public String sendReqValAdd(String xml) {
        return send("reqvaladd", xml);
    }

    public String sendReqValAdd(String xml, String txnId) {
        return sendToImpsDynamic("reqvaladd", txnId, xml);
    }

    /**
     * Send RespValAdd XML to NPCI Mock: POST /npci/respvaladd/{txnId}
     */
    public String sendRespValAdd(String xml) {
        return send("respvaladd", xml);
    }

    public String sendRespValAdd(String xml, String txnId) {
        return sendToNpciDynamic("respvaladd", txnId, xml);
    }
}
