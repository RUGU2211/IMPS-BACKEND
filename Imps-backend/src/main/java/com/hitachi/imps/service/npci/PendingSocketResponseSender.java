package com.hitachi.imps.service.npci;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hitachi.imps.service.socket.PendingSocketResponseStore;

/**
 * Sends response on same socket (completes pending future).
 * Used when npci.compliant-flow is false (backward compatible).
 */
@Component
@ConditionalOnProperty(name = "npci.compliant-flow", havingValue = "false", matchIfMissing = true)
public class PendingSocketResponseSender implements INpciResponseSender {

    @Autowired
    private PendingSocketResponseStore pendingSocketStore;

    @Override
    public boolean send(String txnId, String respXml) {
        return pendingSocketStore.completePending(txnId, respXml);
    }
}
