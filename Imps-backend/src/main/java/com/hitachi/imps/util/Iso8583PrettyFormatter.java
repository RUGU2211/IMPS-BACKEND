package com.hitachi.imps.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/**
 * Formats ISO 8583 message for console with dynamic descriptions.
 */
public final class Iso8583PrettyFormatter {

    private static final Map<String, String> MTI_DESC = Map.of(
        "0200", "Financial Transaction Request",
        "0210", "Financial Transaction Response",
        "0800", "Network Management Request",
        "0810", "Network Management Response"
    );

    private static String descForDe3(String val) {
        if (val == null) return "";
        if (val.startsWith("40")) return "Processing Code – IMPS Pay";
        if (val.startsWith("38")) return "Processing Code – ReqChkTxn";
        if (val.startsWith("32")) return "Processing Code – ReqListAccPvd";
        if (val.startsWith("18") || val.startsWith("08")) return "Processing Code – ReqValAdd/Name Enquiry";
        if (val.startsWith("01")) return "Processing Code – Heartbeat";
        return "Processing Code";
    }

    private static String formatAmountPaise(String paise12) {
        if (paise12 == null || paise12.isBlank()) return "";
        try {
            BigDecimal paise = new BigDecimal(paise12.trim());
            BigDecimal rupees = paise.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return "Amount ₹" + rupees.stripTrailingZeros().toPlainString();
        } catch (Exception e) {
            return "Amount (paise)";
        }
    }

    private static final Map<String, String> DE39_DESC = Map.of(
        "00", "Response Code – Approved",
        "51", "Response Code – Insufficient funds",
        "14", "Response Code – Invalid account",
        "96", "Response Code – System error",
        "91", "Response Code – Issuer unavailable"
    );

    private static final Map<Integer, String> DE_LABELS = Map.ofEntries(
        Map.entry(11, "STAN"),
        Map.entry(37, "RRN"),
        Map.entry(38, "Approval number"),
        Map.entry(41, "Terminal ID"),
        Map.entry(49, "Currency (INR)"),
        Map.entry(32, "Payer IFSC prefix"),
        Map.entry(33, "Payee IFSC"),
        Map.entry(102, "Payer account"),
        Map.entry(103, "Payee account"),
        Map.entry(120, "Additional data")
    );

    public static String format(ISOMsg iso) {
        if (iso == null) return "(null)";
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("================ ISO 8583 MESSAGE ================\n");
            String mti = iso.getMTI();
            sb.append("MTI  : ").append(mti).append("  (").append(MTI_DESC.getOrDefault(mti, "Message Type")).append(")\n");
            for (int i = 1; i <= 128; i++) {
                if (!iso.hasField(i)) continue;
                String val = iso.getString(i);
                String desc = "";
                switch (i) {
                    case 3: desc = descForDe3(val); break;
                    case 4: desc = formatAmountPaise(val); break;
                    case 39: desc = DE39_DESC.getOrDefault(val, "Response Code"); break;
                    default: desc = DE_LABELS.getOrDefault(i, "");
                }
                if (desc.isEmpty()) desc = "DE" + i;
                sb.append(String.format("DE%-4d", i)).append(": ").append(val).append(" (").append(desc).append(")\n");
            }
            sb.append("===================================================");
            return sb.toString();
        } catch (ISOException e) {
            return "ISO format error: " + e.getMessage();
        }
    }
}
