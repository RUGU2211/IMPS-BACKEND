package com.hitachi.imps.client.npci;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.hitachi.imps.config.RoutingConfig;

/**
 * REST client for sending XML to NPCI.
 * Used when NPCI communicates with IMPS over HTTP/REST (e.g. ACK, request/response).
 * Configure routing.npci.base-url to point to actual NPCI or mock; IMPS has no dependency on mock apps.
 */
@Component
public class NpciRestClient {

    @Autowired
    private RoutingConfig routingConfig;
    @Autowired
    private RestTemplate restTemplate;

    public String send(String endpointKey, String xml) {
        try {
            String url = routingConfig.getNpci().getFullUrl(endpointKey);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            String response = restTemplate.postForObject(url, new HttpEntity<>(xml, headers), String.class);
            return response;
        } catch (Exception e) {
            System.err.println("NPCI REST SEND FAILED [" + endpointKey + "]: " + e.getMessage());
            return null;
        }
    }

    /** Send ACK to NPCI (REST). Used when NPCI sends request via HTTP and expects ACK at routing.npci.base-url. */
    public void sendAck(String ackXml) {
        try {
            String url = routingConfig.getNpci().getFullUrl("ack");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            restTemplate.postForObject(url, new HttpEntity<>(ackXml, headers), String.class);
        } catch (Exception e) {
            System.err.println("NPCI REST: Send ACK failed: " + e.getMessage());
        }
    }

    public String sendReqPay(String xml) { return send("reqpay", xml); }
    public String sendReqPay(String xml, String txnId) { return sendToImpsDynamic("reqpay", txnId, xml); }
    public String sendRespPay(String xml) { return send("resppay", xml); }
    public String sendRespPay(String xml, String txnId) { return sendToNpciDynamic("resppay", txnId, xml); }

    private String sendToImpsDynamic(String apiType, String txnId, String xml) {
        if (txnId == null || txnId.isBlank()) return send(apiType, xml);
        try {
            String url = routingConfig.getNpci().getBaseUrl() + "/imps/" + apiType + "/" + txnId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            return restTemplate.postForObject(url, new HttpEntity<>(xml, headers), String.class);
        } catch (Exception e) {
            System.err.println("NPCI REST SEND FAILED [" + apiType + "/" + txnId + "]: " + e.getMessage());
            return null;
        }
    }

    private String sendToNpciDynamic(String apiType, String txnId, String xml) {
        if (txnId == null || txnId.isBlank()) return null;
        try {
            String url = routingConfig.getNpci().getBaseUrl() + "/npci/" + apiType + "/" + txnId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            return restTemplate.postForObject(url, new HttpEntity<>(xml, headers), String.class);
        } catch (Exception e) {
            System.err.println("NPCI REST SEND FAILED [" + apiType + "/" + txnId + "]: " + e.getMessage());
            return null;
        }
    }

    public String sendReqChkTxn(String xml) { return send("reqchktxn", xml); }
    public String sendReqChkTxn(String xml, String txnId) { return sendToImpsDynamic("reqchktxn", txnId, xml); }
    public String sendRespChkTxn(String xml) { return send("respchktxn", xml); }
    public String sendRespChkTxn(String xml, String txnId) { return sendToNpciDynamic("respchktxn", txnId, xml); }

    public String sendReqHbt(String xml) {
        String txnId = com.hitachi.imps.service.iso.XmlUtil.read(xml, "//*[local-name()='Txn']/@id");
        if (txnId == null || txnId.isBlank()) txnId = com.hitachi.imps.service.iso.XmlUtil.read(xml, "//*[local-name()='Head']/@msgId");
        return sendReqHbt(xml, txnId != null ? txnId : "HBT" + System.currentTimeMillis());
    }
    public String sendReqHbt(String xml, String txnId) {
        if (txnId == null || txnId.isBlank()) return sendReqHbt(xml);
        try {
            String url = routingConfig.getNpci().getBaseUrl() + "/imps/hbt/req/" + txnId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            return restTemplate.postForObject(url, new HttpEntity<>(xml, headers), String.class);
        } catch (Exception e) {
            System.err.println("NPCI REST SEND FAILED [reqhbt]: " + e.getMessage());
            return null;
        }
    }
    public String sendRespHbt(String xml) { return send("resphbt", xml); }
    public String sendRespHbt(String xml, String txnId) { return sendToNpciDynamic("resphbt", txnId, xml); }

    public String sendReqListAccPvd(String xml) { return send("reqlistaccpvd", xml); }
    public String sendRespListAccPvd(String xml) { return send("resplistaccpvd", xml); }
    public String sendRespListAccPvd(String xml, String txnId) { return sendToNpciDynamic("resplistaccpvd", txnId, xml); }

    public String sendReqValAdd(String xml) { return send("reqvaladd", xml); }
    public String sendReqValAdd(String xml, String txnId) { return sendToImpsDynamic("reqvaladd", txnId, xml); }
    public String sendRespValAdd(String xml) { return send("respvaladd", xml); }
    public String sendRespValAdd(String xml, String txnId) { return sendToNpciDynamic("respvaladd", txnId, xml); }
}
