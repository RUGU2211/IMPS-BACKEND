package com.hitachi.imps.util;

public enum NpciErrorCodes {

    SUCCESS("00", "SUCCESS"),
    INVALID_IFSC("MJ", "INVALID IFSC"),
    DUPLICATE_TXN("94", "DUPLICATE TRANSACTION"),
    SYSTEM_ERROR("96", "SYSTEM ERROR"),
    SWITCH_DOWN("91", "SWITCH DOWN"),
    INSUFFICIENT_BAL("51", "INSUFFICIENT BALANCE");

    private final String code;
    private final String desc;

    NpciErrorCodes(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String code() { return code; }
    public String desc() { return desc; }
}

	