package com.hitachi.imps.util;

public enum NpciResponseCode {

    SUCCESS("00", "0000"),
    INVALID_ACCOUNT("12", "1212"),
    INSUFFICIENT_FUNDS("51", "2424"),
    BANK_NOT_AVAILABLE("91", "9191"),
    SYSTEM_ERROR("96", "9696");

    private final String isoCode;     // DE39
    private final String npciCode;    // NPCI app code

    NpciResponseCode(String isoCode, String npciCode) {
        this.isoCode = isoCode;
        this.npciCode = npciCode;
    }

    public String isoCode() {
        return isoCode;
    }

    public String npciCode() {
        return npciCode;
    }
}
