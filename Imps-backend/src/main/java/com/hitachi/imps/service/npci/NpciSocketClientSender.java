package com.hitachi.imps.service.npci;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hitachi.imps.client.npci.NpciSocketClient;

/**
 * Sends response to NPCI via outbound socket (Phase 3).
 * Used when npci.compliant-flow is true.
 */
@Component
@ConditionalOnProperty(name = "npci.compliant-flow", havingValue = "true")
public class NpciSocketClientSender implements INpciResponseSender {

    @Autowired
    private NpciSocketClient npciSocketClient;

    @Override
    public boolean send(String txnId, String respXml) {
        return npciSocketClient.sendResponse(respXml);
    }
}
