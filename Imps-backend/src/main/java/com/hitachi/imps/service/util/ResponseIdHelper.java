package com.hitachi.imps.service.util;

import com.hitachi.imps.spec.NpciReqPayRules;

/**
 * Helper for response message IDs. No random generation – use request-derived values only.
 * Rule 021: Head msgId in response = 35 chars (3 BPC + 32). We use BPC from request msgId + "R" + timestamp.
 */
public final class ResponseIdHelper {

    private ResponseIdHelper() {}

    /**
     * Build 35-char response Head msgId from request msgId (no random).
     * Uses first 3 chars (BPC) of reqMsgId + "R" + 31 chars from timestamp = 35 total.
     */
    public static String responseMsgIdFromRequest(String reqMsgId) {
        String bpc = (reqMsgId != null && reqMsgId.length() >= NpciReqPayRules.BPC_LENGTH)
            ? reqMsgId.substring(0, NpciReqPayRules.BPC_LENGTH)
            : "RSP";
        String t = String.valueOf(System.currentTimeMillis());
        String rest = (t.length() >= 31) ? t.substring(t.length() - 31) : String.format("%31s", t).replace(' ', '0');
        return bpc + "R" + rest;
    }
}
