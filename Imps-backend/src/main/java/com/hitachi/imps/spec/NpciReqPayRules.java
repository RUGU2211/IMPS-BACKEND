package com.hitachi.imps.spec;

/**
 * REQPAY Rules & Validations per NPCI IMPS Common Code Technical Specifications – Appendix (Rules).
 *
 * Head/Txn (Rule 019–022, 024): Head_Version, Head_ts, Head_MsgId, Txn_UUID, Txn_code.
 * Payer/Payee (026, 029): InfoRating TRUE/FALSE, Type PERSON/ENTITY.
 * Device (034, 035): Device values, TYPE 1–20 chars from fixed enum (MOB, INET, BRC, ATM, MAT, SMS, WAP, IVR), ID 1–35.
 * Institution (042–044): initiationMode 12, type MTO/BANK, route MTSS/RDA.
 * Account (048, 049): ACCOUNT → IFSC 11, ACTYPE enum, ACNUM max 30; MOBILE → MOBNUM 12, MMID 7.
 * Amount/RefCategory (051, 052): 2 decimals; refCategory 00–09.
 * 8.2 Fixed Enumerations: prodType UPI|IMPS|AEPS; Device type; Institution IFSC for FIR.
 */
public final class NpciReqPayRules {

    private NpciReqPayRules() {}

    // ---------- Rule 019 – Head_Version (2.1.1) ----------
    public static final String HEAD_VER_1 = "1.0";
    public static final String HEAD_VER_2 = "2.0";
    public static final String[] HEAD_VERSIONS = { HEAD_VER_1, HEAD_VER_2 };

    // ---------- Rule 020 – Head_ts (2.1.2): YYYY-MM-DDTHH:mm:ss.sssZ or ±hh:mm ----------
    /** ISO timestamp: date T time with optional millis and Z or ±hh:mm. No AM/PM. */
    public static final java.util.regex.Pattern HEAD_TS_PATTERN = java.util.regex.Pattern.compile(
        "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,3})?(Z|[+-]\\d{2}:\\d{2})?$"
    );

    // ---------- Rule 021 – Head_MsgId (2.1.4): 35 chars = 3 BPC + 32 UUID ----------
    public static final int MSG_ID_TOTAL_LENGTH = 35;
    public static final int BPC_LENGTH = 3;
    public static final int UUID_PART_LENGTH = 32;

    // ---------- Rule 022 – Txn_UUID (4.1.1): same as MsgId ----------
    public static final int TXN_ID_TOTAL_LENGTH = 35;

    // ---------- Rule 024 – Txn_code (5.1.5, 6.2.5): PERSON=0000, ENTITY=XXXX (MCC) ----------
    public static final String CODE_PERSON = "0000";
    public static final int ENTITY_MCC_LENGTH = 4;

    // ---------- Rule 026 – Payer/Payee_InfoRating (5.6.1, 6.5.1) ----------
    public static final String INFO_RATING_TRUE = "TRUE";
    public static final String INFO_RATING_FALSE = "FALSE";

    // ---------- Rule 029 – Payer/Payee_Type (5.1.4, 6.2.4) ----------
    public static final String PAYER_PAYEE_TYPE_PERSON = "PERSON";
    public static final String PAYER_PAYEE_TYPE_ENTITY = "ENTITY";
    public static final String[] PAYER_PAYEE_TYPES = { PAYER_PAYEE_TYPE_PERSON, PAYER_PAYEE_TYPE_ENTITY };

    // ---------- Rule 034, 035 – Device TYPE (5.8.2): min 1, max 20; allowed enum ----------
    public static final int DEVICE_TYPE_MIN_LEN = 1;
    public static final int DEVICE_TYPE_MAX_LEN = 20;
    public static final int DEVICE_ID_MIN_LEN = 1;
    public static final int DEVICE_ID_MAX_LEN = 35;

    // ---------- 8.2 Fixed Enumerations ----------
    public static final String PROD_TYPE_UPI = "UPI";
    public static final String PROD_TYPE_IMPS = "IMPS";
    public static final String PROD_TYPE_AEPS = "AEPS";

    public static final String[] PROD_TYPES = { PROD_TYPE_UPI, PROD_TYPE_IMPS, PROD_TYPE_AEPS };

    /** Device Type (Rule 035): MOB, INET, BRC, ATM, MAT, SMS, WAP, IVR */
    public static final String DEVICE_MOB = "MOB";
    public static final String DEVICE_INET = "INET";
    public static final String DEVICE_WAP = "WAP";
    public static final String DEVICE_IVR = "IVR";
    public static final String DEVICE_ATM = "ATM";
    public static final String DEVICE_BRC = "BRC";
    public static final String DEVICE_MAT = "MAT";
    public static final String DEVICE_SMS = "SMS";

    public static final String[] DEVICE_TYPES = { DEVICE_MOB, DEVICE_INET, DEVICE_WAP, DEVICE_IVR, DEVICE_ATM, DEVICE_BRC, DEVICE_MAT, DEVICE_SMS };

    /** Rule 048 – ACTYPE allowed values */
    public static final String[] ACTYPE_VALUES = {
        "SAVINGS", "DEFAULT", "CURRENT", "NRE", "NRO", "PPIWALLET", "BANKWALLET",
        "CREDIT", "SOD", "UOD", "SEMICLOSEDPPIWALLET", "SEMICLOSEDBANKWALLET", "SNRR"
    };

    /** 8.1 Rule 043 – Payer Institution Type */
    public static final String INSTITUTION_TYPE_MTO = "MTO";
    public static final String INSTITUTION_TYPE_BANK = "BANK";
    public static final String[] INSTITUTION_TYPES = { INSTITUTION_TYPE_MTO, INSTITUTION_TYPE_BANK };

    /** 8.1 Rule 044 – Payer Institution Route */
    public static final String ROUTE_MTSS = "MTSS";
    public static final String ROUTE_RDA = "RDA";
    public static final String[] ROUTES = { ROUTE_MTSS, ROUTE_RDA };

    /** 8.1 Rule 052 – refCategory allowed 00–09 */
    public static final String REF_CATEGORY_NULL = "00";
    public static final String REF_CATEGORY_ADVERTISEMENT = "01";
    public static final String REF_CATEGORY_INVOICE = "02";
    public static final int REF_CATEGORY_MAX = 9;  // 00 to 09

    /** 8.1 Rule 048 – ACCOUNT: IFSC 11-char, ACNUM max 30 */
    public static final int IFSC_LENGTH = 11;
    public static final int ACNUM_MAX_DIGITS = 30;

    /** 8.1 Rule 049 – MOBILE: MOBNUM 12 digits (+91 + 10), MMID 7-digit */
    public static final int MOBNUM_LENGTH = 12;  // +91 + 10 digits
    public static final int MMID_LENGTH = 7;

    /** 8.1 Rule 051 – Amount: 2 decimal places */
    public static final int AMOUNT_DECIMAL_PLACES = 2;

    /** initiationMode "12" → Institution block mandatory (Rule 042) */
    public static final String INITIATION_MODE_FIR = "12";

    public static boolean isRefCategoryAllowed(String refCategory) {
        if (refCategory == null || refCategory.length() != 2) return false;
        try {
            int n = Integer.parseInt(refCategory);
            return n >= 0 && n <= REF_CATEGORY_MAX;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isProdTypeAllowed(String prodType) {
        if (prodType == null) return false;
        for (String p : PROD_TYPES) {
            if (p.equals(prodType)) return true;
        }
        return false;
    }

    public static boolean isInstitutionTypeAllowed(String type) {
        if (type == null) return false;
        return INSTITUTION_TYPE_MTO.equals(type) || INSTITUTION_TYPE_BANK.equals(type);
    }

    public static boolean isRouteAllowed(String route) {
        if (route == null) return false;
        return ROUTE_MTSS.equals(route) || ROUTE_RDA.equals(route);
    }

    /** Rule 019 – Head version 1.0 or 2.0 */
    public static boolean isHeadVersionAllowed(String ver) {
        if (ver == null || ver.isBlank()) return false;
        return HEAD_VER_1.equals(ver.trim()) || HEAD_VER_2.equals(ver.trim());
    }

    /** Rule 020 – Head ts ISO format (no AM/PM) */
    public static boolean isHeadTsValid(String ts) {
        if (ts == null || ts.isBlank()) return false;
        return HEAD_TS_PATTERN.matcher(ts.trim()).matches();
    }

    /** Rule 021 – MsgId 35 chars: first 3 alphanumeric (BPC), remaining 32 (UUID-like alphanumeric) */
    public static boolean isMsgIdFormatValid(String msgId) {
        if (msgId == null) return false;
        String s = msgId.trim();
        if (s.length() != MSG_ID_TOTAL_LENGTH) return false;
        return s.substring(0, BPC_LENGTH).matches("^[A-Za-z0-9]{3}$")
            && s.substring(BPC_LENGTH).matches("^[A-Za-z0-9]{32}$");
    }

    /** Rule 022 – Txn id same format as MsgId (35 chars) */
    public static boolean isTxnIdFormatValid(String txnId) {
        if (txnId == null) return false;
        String s = txnId.trim();
        if (s.length() != TXN_ID_TOTAL_LENGTH) return false;
        return s.substring(0, BPC_LENGTH).matches("^[A-Za-z0-9]{3}$")
            && s.substring(BPC_LENGTH).matches("^[A-Za-z0-9]{32}$");
    }

    /** Rule 024 – PERSON code must be 0000; ENTITY = 4-digit MCC */
    public static boolean isPayerPayeeCodeValid(String type, String code) {
        if (code == null || code.isBlank()) return true; // optional in some contexts
        if (PAYER_PAYEE_TYPE_PERSON.equals(type)) return CODE_PERSON.equals(code);
        if (PAYER_PAYEE_TYPE_ENTITY.equals(type)) return code.matches("^\\d{4}$");
        return true;
    }

    /** Rule 026 – InfoRating TRUE | FALSE */
    public static boolean isInfoRatingAllowed(String rating) {
        if (rating == null || rating.isBlank()) return true;
        return INFO_RATING_TRUE.equalsIgnoreCase(rating.trim()) || INFO_RATING_FALSE.equalsIgnoreCase(rating.trim());
    }

    /** Rule 029 – Payer/Payee type PERSON | ENTITY */
    public static boolean isPayerPayeeTypeAllowed(String type) {
        if (type == null || type.isBlank()) return false;
        String t = type.trim();
        return PAYER_PAYEE_TYPE_PERSON.equals(t) || PAYER_PAYEE_TYPE_ENTITY.equals(t);
    }

    /** Rule 035 – Device TYPE from allowed enum; length 1–20 */
    public static boolean isDeviceTypeAllowed(String type) {
        if (type == null || type.isBlank()) return false;
        if (type.length() < DEVICE_TYPE_MIN_LEN || type.length() > DEVICE_TYPE_MAX_LEN) return false;
        for (String d : DEVICE_TYPES) {
            if (d.equals(type)) return true;
        }
        return false;
    }

    /** Rule 048 – ACTYPE from fixed enumeration */
    public static boolean isActypeAllowed(String actype) {
        if (actype == null || actype.isBlank()) return true;
        for (String a : ACTYPE_VALUES) {
            if (a.equalsIgnoreCase(actype.trim())) return true;
        }
        return false;
    }
}
