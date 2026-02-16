package com.hitachi.imps.client.switchclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.hitachi.imps.service.routing.SwitchAddressResolver;
import com.hitachi.imps.service.routing.SwitchAddressResolver.SwitchAddress;
import com.hitachi.imps.util.IsoUtil;

/**
 * REST client for Switch.
 * Uses institution_master (lookup by request_org_id) for switch base URL; falls back to application.yml.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "routing.switch.rest.enabled", havingValue = "true")
public class RestSwitchClient implements ISwitchClient {

    private static final Logger log = LoggerFactory.getLogger(RestSwitchClient.class);

    @Autowired
    private SwitchAddressResolver switchAddressResolver;
    @Autowired
    private RestTemplate restTemplate;

    private String buildDynamicUrl(String apiType, String txnId, String requestOrgId) {
        SwitchAddress addr = switchAddressResolver.resolve(requestOrgId);
        String base = addr.getRestBaseUrl();
        return base + "/imps/" + apiType + "/" + (txnId != null ? txnId : "");
    }

    private byte[] sendDynamic(String apiType, String txnId, byte[] isoBytes, String requestOrgId) {
        if (txnId == null || txnId.isBlank()) return null;
        try {
            String url = buildDynamicUrl(apiType, txnId, requestOrgId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            HttpEntity<byte[]> request = new HttpEntity<>(isoBytes, headers);
            return restTemplate.postForObject(url, request, byte[].class);
        } catch (Exception e) {
            log.warn("SWITCH SEND FAILED [{}/{}]: {}", apiType, txnId, e.getMessage());
            return null;
        }
    }

    @Override
    public byte[] sendReqPay(ISOMsg iso, String txnId) {
        return sendReqPay(iso, txnId, null);
    }

    @Override
    public byte[] sendReqPay(ISOMsg iso, String txnId, String requestOrgId) {
        return sendDynamic("reqpay", txnId, IsoUtil.pack(iso), requestOrgId);
    }

    @Override
    public byte[] sendReqPay(byte[] isoBytes, String txnId) {
        return sendReqPay(isoBytes, txnId, null);
    }

    @Override
    public byte[] sendReqPay(byte[] isoBytes, String txnId, String requestOrgId) {
        return sendDynamic("reqpay", txnId, isoBytes, requestOrgId);
    }

    @Override
    public byte[] sendReqChkTxn(ISOMsg iso, String txnId) {
        return sendReqChkTxn(iso, txnId, null);
    }

    @Override
    public byte[] sendReqChkTxn(ISOMsg iso, String txnId, String requestOrgId) {
        return sendDynamic("reqchktxn", txnId, IsoUtil.pack(iso), requestOrgId);
    }

    @Override
    public byte[] sendReqChkTxn(byte[] isoBytes, String txnId) {
        return sendReqChkTxn(isoBytes, txnId, null);
    }

    @Override
    public byte[] sendReqChkTxn(byte[] isoBytes, String txnId, String requestOrgId) {
        return sendDynamic("reqchktxn", txnId, isoBytes, requestOrgId);
    }

    @Override
    public byte[] sendReqHbt(ISOMsg iso, String txnId) {
        return sendReqHbt(iso, txnId, null);
    }

    @Override
    public byte[] sendReqHbt(ISOMsg iso, String txnId, String requestOrgId) {
        return sendDynamic("reqhbt", txnId, IsoUtil.pack(iso), requestOrgId);
    }

    @Override
    public byte[] sendReqHbt(byte[] isoBytes, String txnId) {
        return sendReqHbt(isoBytes, txnId, null);
    }

    @Override
    public byte[] sendReqHbt(byte[] isoBytes, String txnId, String requestOrgId) {
        return sendDynamic("reqhbt", txnId, isoBytes, requestOrgId);
    }

    @Override
    public byte[] sendReqListAccPvd(ISOMsg iso, String txnId) {
        return sendReqListAccPvd(iso, txnId, null);
    }

    @Override
    public byte[] sendReqListAccPvd(ISOMsg iso, String txnId, String requestOrgId) {
        return sendDynamic("reqlistaccpvd", txnId, IsoUtil.pack(iso), requestOrgId);
    }

    @Override
    public byte[] sendReqListAccPvd(byte[] isoBytes, String txnId) {
        return sendReqListAccPvd(isoBytes, txnId, null);
    }

    @Override
    public byte[] sendReqListAccPvd(byte[] isoBytes, String txnId, String requestOrgId) {
        return sendDynamic("reqlistaccpvd", txnId, isoBytes, requestOrgId);
    }

    @Override
    public byte[] sendReqValAdd(ISOMsg iso, String txnId) {
        return sendReqValAdd(iso, txnId, null);
    }

    @Override
    public byte[] sendReqValAdd(ISOMsg iso, String txnId, String requestOrgId) {
        return sendDynamic("reqvaladd", txnId, IsoUtil.pack(iso), requestOrgId);
    }

    @Override
    public byte[] sendReqValAdd(byte[] isoBytes, String txnId) {
        return sendReqValAdd(isoBytes, txnId, null);
    }

    @Override
    public byte[] sendReqValAdd(byte[] isoBytes, String txnId, String requestOrgId) {
        return sendDynamic("reqvaladd", txnId, isoBytes, requestOrgId);
    }

    @Override
    public byte[] sendRespPay(ISOMsg iso, String txnId) {
        return txnId != null && !txnId.isBlank() ? sendDynamic("resppay", txnId, IsoUtil.pack(iso), null) : null;
    }

    @Override
    public byte[] sendRespChkTxn(ISOMsg iso, String txnId) {
        return txnId != null && !txnId.isBlank() ? sendDynamic("respchktxn", txnId, IsoUtil.pack(iso), null) : null;
    }

    @Override
    public byte[] sendRespHbt(ISOMsg iso, String txnId) {
        return txnId != null && !txnId.isBlank() ? sendDynamic("resphbt", txnId, IsoUtil.pack(iso), null) : null;
    }

    @Override
    public byte[] sendRespValAdd(ISOMsg iso, String txnId) {
        return txnId != null && !txnId.isBlank() ? sendDynamic("respvaladd", txnId, IsoUtil.pack(iso), null) : null;
    }
}
