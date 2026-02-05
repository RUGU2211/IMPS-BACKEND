package com.hitachi.imps.converter;

/**
 * RespPay message field lengths per NPCI spec (table compliance).
 * Use for truncation/validation when building RespPay XML.
 *
 * Common Code Appendix: Rule 027 – errCode only when result=FAILURE; Rule 032 – Ref IFSC 11 chars when result=SUCCESS.
 *
 * Table mapping:
 * 1.1   API Name RespPay, 1.1.1 xmlns Min1 Max255
 * 2.1   Head: 2.1.1 ver Min1 Max6, 2.1.2 ts Max255, 2.1.3 orgId Max20, 2.1.4 msgId Length35, 2.1.5 prodType IMPS
 * 4.1   Txn: 4.1.1 id Length35, 4.1.2 note Max50, 4.1.3 refId Max35, 4.1.4 refUrl Max35, 4.1.5 ts Max255,
 *       4.1.6 type Max20, 4.1.7 subType Max20, 4.1.8 initiationMode Max3, 4.1.9 refCategory Max2
 * 5.1   Resp: 5.1.1 reqMsgId Length35, 5.1.2 result Max20, 5.1.3 errCode Max20 (Rule 027: only if result=FAILURE)
 * 5.2   Ref: 5.2.13 IFSC Length11 (Rule 032_RespPay_RefTag_IFSC)
 */
public final class RespPaySpec {

    private RespPaySpec() {}

    // 1. API: xmlns Min 1, Max 255
    public static final int XMLNS_MAX = 255;

    // 2. Head
    public static final int HEAD_VER_MIN = 1;
    public static final int HEAD_VER_MAX = 6;
    public static final int HEAD_TS_MAX = 255;
    public static final int HEAD_ORGID_MAX = 20;
    public static final int HEAD_MSGID_LEN = 35;
    public static final String PRODTYPE_FIXED = "IMPS";

    // 4. Txn
    public static final int TXN_ID_LEN = 35;
    public static final int TXN_NOTE_MAX = 50;
    public static final int TXN_REFID_MAX = 35;
    public static final int TXN_REFURL_MAX = 35;
    public static final int TXN_TS_MAX = 255;
    public static final int TXN_TYPE_MAX = 20;
    public static final int TXN_SUBTYPE_MAX = 20;
    public static final int TXN_INITIATIONMODE_MAX = 3;
    public static final int TXN_REFCATEGORY_MAX = 2;

    // 5.1 Resp
    public static final int RESP_REQMSGID_LEN = 35;
    public static final int RESP_RESULT_MAX = 20;
    public static final int RESP_ERRCODE_MAX = 20;

    // 5.2 Ref
    public static final int REF_SEQNUM_MAX = 3;
    public static final int REF_ADDR_MAX = 255;
    public static final int REF_SETTAMOUNT_TOTALDIGITS = 15;
    public static final int REF_SETTCURRENCY_LEN = 3;
    public static final int REF_APPROVALNUM_LEN = 6;
    public static final int REF_RESPCODE_MAX = 20;
    public static final int REF_REGNAME_MAX = 99;
    public static final int REF_ORGAMOUNT_TOTALDIGITS = 15;
    public static final int REF_REVERSALRESPCODE_MAX = 20;
    public static final int REF_ACNUM_MAX = 30;
    public static final int REF_CODE_LEN = 4;
    public static final int REF_IFSC_LEN = 11;

    /** Truncate to max length; null/blank returns empty string. */
    public static String truncate(String value, int maxLen) {
        if (value == null) return "";
        String s = value.trim();
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen);
    }

    /** Ensure exact length: pad with zeros (right) or truncate. */
    public static String exactLen(String value, int len, char padChar) {
        if (value == null) value = "";
        value = value.trim();
        if (value.length() >= len) return value.substring(0, len);
        return value + String.valueOf(padChar).repeat(len - value.length());
    }

    /** Amount string max 15 total digits (integer + fraction) per spec. */
    public static String amountWithinDigits(String amountStr, int maxTotalDigits) {
        if (amountStr == null || amountStr.isBlank()) return "0.00";
        String s = amountStr.trim();
        int digits = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) digits++;
        }
        if (digits <= maxTotalDigits) return s;
        int seen = 0;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length() && seen < maxTotalDigits; i++) {
            char c = s.charAt(i);
            if (c == '.' || c == '-' || c == '+') out.append(c);
            else if (Character.isDigit(c)) { out.append(c); seen++; }
        }
        return out.toString().isEmpty() ? "0.00" : out.toString();
    }
}
