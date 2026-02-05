package com.hitachi.imps.spec;

/**
 * Account Provider (AccPvd) tag descriptions per NPCI spec 7.4.3.
 *
 * 1.1  AccPvdList  1..1  NA
 * 1.2  AccPvd      1..n  NA
 * 1.2.1  name           1..1  As per onboarding
 * 1.2.2  iin            1..n  4-digit NBIN
 * 1.2.3  bankCode       1..n  3-digit
 * 1.2.4  ifsc           1..n  IFSC Codes
 * 1.2.5  active         1..1  Y / N
 * 1.2.7  url            0..n  NA
 * 1.2.8  spocName       0..n  NA
 * 1.2.9  spocEmail      0..n  NA
 * 1.2.10 spocPhone      0..n  NA
 * 1.2.11 prods          0..n  IMPS
 * 1.2.12 lastModifiedTs 1..1  ISODateTime
 * 1.2.13 featureSupported 0..1  Reserved (use "01")
 */
public final class AccPvdSpec {

    private AccPvdSpec() {}

    public static final int NBIN_LENGTH = 4;       // 1.2.2 iin – 4-digit NBIN
    public static final int BANK_CODE_LENGTH = 3;  // 1.2.3 bankCode – 3-digit
    public static final String ACTIVE_Y = "Y";
    public static final String ACTIVE_N = "N";
    public static final String PRODS_IMPS = "IMPS";
    public static final String FEATURE_SUPPORTED_DEFAULT = "01";  // 1.2.13 Reserved

    /** Ensure 4-digit NBIN (pad or truncate). */
    public static String formatIin(String iin) {
        if (iin == null || iin.isBlank()) return "0000";
        String s = iin.trim().replaceAll("\\D", "");
        if (s.length() >= NBIN_LENGTH) return s.substring(0, NBIN_LENGTH);
        return String.format("%4s", s).replace(' ', '0');
    }

    /** Ensure 3-digit bank code. */
    public static String formatBankCode(String bankCode) {
        if (bankCode == null || bankCode.isBlank()) return "000";
        String s = bankCode.trim();
        if (s.length() >= BANK_CODE_LENGTH) return s.substring(0, BANK_CODE_LENGTH);
        return String.format("%3s", s).replace(' ', '0');
    }

    /** Active status Y/N. */
    public static String formatActive(Boolean active) {
        return Boolean.TRUE.equals(active) ? ACTIVE_Y : ACTIVE_N;
    }
}
