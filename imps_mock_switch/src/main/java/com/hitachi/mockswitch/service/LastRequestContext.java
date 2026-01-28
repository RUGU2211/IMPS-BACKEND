package com.hitachi.mockswitch.service;

import org.springframework.stereotype.Component;

/**
 * Holds DE120 (and key fields) from the last received request per type.
 * Used when manual trigger is enabled: if caller does not supply txnId,
 * we use this so manual mode behaves like auto (same DE120 echoed back).
 * Auto mode is the default; this is ready for future use when you switch to manual.
 */
@Component
public class LastRequestContext {

    private volatile String lastReqPayDe120;
    private volatile String lastReqChkTxnDe120;
    private volatile String lastReqHbtDe120;
    private volatile String lastReqValAddDe120;
    private volatile String lastReqListAccPvdDe120;

    public String getLastReqPayDe120() { return lastReqPayDe120; }
    public void setLastReqPayDe120(String v) { this.lastReqPayDe120 = v; }

    public String getLastReqChkTxnDe120() { return lastReqChkTxnDe120; }
    public void setLastReqChkTxnDe120(String v) { this.lastReqChkTxnDe120 = v; }

    public String getLastReqHbtDe120() { return lastReqHbtDe120; }
    public void setLastReqHbtDe120(String v) { this.lastReqHbtDe120 = v; }

    public String getLastReqValAddDe120() { return lastReqValAddDe120; }
    public void setLastReqValAddDe120(String v) { this.lastReqValAddDe120 = v; }

    public String getLastReqListAccPvdDe120() { return lastReqListAccPvdDe120; }
    public void setLastReqListAccPvdDe120(String v) { this.lastReqListAccPvdDe120 = v; }
}
