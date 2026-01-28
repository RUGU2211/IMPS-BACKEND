package com.hitachi.imps.converter;

import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.service.iso.XmlUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

/**
 * Converter for transforming NPCI XML messages to ISO 8583 format.
 * Supports all IMPS API types: ReqPay, ReqChkTxn, ReqHbt, ReqValAdd, ReqListAccPvd
 */
@Component
public class XmlToIsoConverter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMdd");
    private static final Random RANDOM = new Random();

    /* ===============================
       REQPAY - XML to ISO 0200
       As per IMPS Specification
       =============================== */
    public ISOMsg convertReqPay(String xml) {
        try {
            Map<String, String> tags = XmlUtil.parseReqPay(xml);

            ISOMsg iso = new ISOMsg();
            iso.setPackager(new ImpsIsoPackager());
            iso.setMTI("0200");

            // DE3: Processing code for fund transfer (400000 = P2P transfer)
            iso.set(3, "400000");

            // DE4: Amount in paise (12 digits)
            iso.set(4, amountToPaise(tags.get("amount")));

            // DE11: STAN - System Trace Audit Number
            iso.set(11, generateStan());

            // DE12: Local time (HHmmss)
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));

            // DE13: Local date (MMdd)
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));

            // DE32: Acquiring Institution ID (Payer IFSC first 4 chars)
            String payerIfsc = tags.get("payer_ifsc");
            if (payerIfsc != null && !payerIfsc.isEmpty()) {
                iso.set(32, payerIfsc.substring(0, Math.min(4, payerIfsc.length())));
            }

            // DE33: Forwarding Institution ID (Payee IFSC)
            String payeeIfsc = tags.get("payee_ifsc");
            if (payeeIfsc != null && !payeeIfsc.isEmpty()) {
                iso.set(33, payeeIfsc);
            }

            // DE37: RRN - Retrieval Reference Number
            iso.set(37, generateRrn());

            // DE41: Terminal ID
            iso.set(41, "IMPSTERM");

            // DE49: Currency code (INR = 356)
            iso.set(49, "356");

            // DE102: Payer Account Number
            String payerAc = tags.get("payer_acnum");
            if (payerAc != null && !payerAc.isEmpty()) {
                iso.set(102, payerAc);
            }

            // DE103: Payee Account Number
            String payeeAc = tags.get("payee_acnum");
            if (payeeAc != null && !payeeAc.isEmpty()) {
                iso.set(103, payeeAc);
            }

            // DE120: Additional data (Transaction ID from XML)
            String txnId = tags.get("txnId");
            if (txnId != null && !txnId.isEmpty()) {
                iso.set(120, txnId);
            }

            return iso;

        } catch (Exception e) {
            throw new RuntimeException("ReqPay XML to ISO conversion failed", e);
        }
    }

    /* ===============================
       RESPPAY - XML to ISO 0210
       As per IMPS Specification
       =============================== */
    public ISOMsg convertRespPay(String xml) {
        try {
            Map<String, String> tags = XmlUtil.parseRespPay(xml);

            ISOMsg iso = new ISOMsg();
            iso.setPackager(new ImpsIsoPackager());
            iso.setMTI("0210");

            String result = tags.get("result");
            String respCode = tags.get("respCode");
            String approvalNum = tags.get("approvalNum");
            String settAmount = tags.get("settAmount");
            String acNum = tags.get("acNum");
            String ifsc = tags.get("IFSC");

            // DE3: Processing code
            iso.set(3, "400000");
            
            // DE4: Amount in paise
            iso.set(4, amountToPaise(settAmount));
            
            // DE11: STAN
            iso.set(11, generateStan());
            
            // DE12: Local time
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            
            // DE13: Local date
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            
            // DE33: Forwarding Institution (IFSC)
            if (ifsc != null && !ifsc.isEmpty()) {
                iso.set(33, ifsc);
            }
            
            // DE37: RRN
            iso.set(37, generateRrn());
            
            // DE38: Approval Number (max 6 chars per ImpsIsoPackager)
            iso.set(38, normalizeDe38(approvalNum));
            
            // DE39: Response Code (max 2 chars per ImpsIsoPackager)
            iso.set(39, normalizeDe39(result, respCode));
            
            // DE41: Terminal ID
            iso.set(41, "IMPSTERM");
            
            // DE49: Currency code
            iso.set(49, "356");

            // DE103: Payee Account
            if (acNum != null && !acNum.isEmpty()) {
                iso.set(103, acNum);
            }

            return iso;

        } catch (Exception e) {
            throw new RuntimeException("RespPay XML to ISO conversion failed", e);
        }
    }

    /* ===============================
       REQCHKTXN - XML to ISO 0200 (Inquiry)
       =============================== */
    public ISOMsg convertReqChkTxn(String xml) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(new ImpsIsoPackager());
            iso.setMTI("0200");

            // Processing code for status inquiry
            iso.set(3, "380000");

            String txnId = XmlUtil.read(xml, "//*[local-name()='Txn']/@id");
            String orgTxnId = XmlUtil.read(xml, "//*[local-name()='Txn']/@orgTxnId");
            String orgRrn = XmlUtil.read(xml, "//*[local-name()='Txn']/@orgRrn");
            String amount = XmlUtil.read(xml, "//*[local-name()='Amount']/@value");

            iso.set(4, amountToPaise(amount));
            iso.set(11, generateStan());
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            iso.set(37, orgRrn != null && !orgRrn.isEmpty() ? orgRrn : generateRrn());
            iso.set(41, "IMPSTERM");
            iso.set(49, "356");

            // Original transaction reference in DE48
            if (orgTxnId != null) {
                iso.set(48, orgTxnId);
            }
            // ChkTxn transaction ID in DE120 (so Switch can echo it back for DB lookup)
            if (txnId != null && !txnId.isEmpty()) {
                iso.set(120, txnId);
            }

            return iso;

        } catch (Exception e) {
            throw new RuntimeException("ReqChkTxn XML to ISO conversion failed", e);
        }
    }

    /* ===============================
       RESPCHKTXN - XML to ISO 0210
       =============================== */
    public ISOMsg convertRespChkTxn(String xml) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(new ImpsIsoPackager());
            iso.setMTI("0210");

            String result = XmlUtil.read(xml, "//*[local-name()='Resp']/@result");
            String respCode = XmlUtil.read(xml, "//*[local-name()='Ref']/@respCode");
            String approvalNum = XmlUtil.read(xml, "//*[local-name()='Ref']/@approvalNum");

            iso.set(3, "380000");
            iso.set(11, generateStan());
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            iso.set(37, generateRrn());
            iso.set(38, normalizeDe38(approvalNum));
            iso.set(39, normalizeDe39(result, respCode));
            iso.set(41, "IMPSTERM");
            iso.set(49, "356");

            return iso;

        } catch (Exception e) {
            throw new RuntimeException("RespChkTxn XML to ISO conversion failed", e);
        }
    }

    /* ===============================
       REQHBT - XML to ISO 0800 (Network Management)
       =============================== */
    public ISOMsg convertReqHbt(String xml) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(new ImpsIsoPackager());
            iso.setMTI("0800");

            String txnId = XmlUtil.read(xml, "//*[local-name()='Txn']/@id");
            String hbtType = XmlUtil.read(xml, "//*[local-name()='HbtMsg']/@type");

            // Processing code for heartbeat
            iso.set(3, "990000");
            iso.set(11, generateStan());
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            iso.set(37, generateRrn());
            iso.set(41, "IMPSTERM");

            // Network management info
            iso.set(24, "831"); // Function code for heartbeat

            // Heartbeat type in DE48
            if (hbtType != null) {
                iso.set(48, hbtType);
            }
            // HBT transaction ID in DE120 (so Switch can echo it back for DB lookup)
            if (txnId != null && !txnId.isEmpty()) {
                iso.set(120, txnId);
            }

            return iso;

        } catch (Exception e) {
            throw new RuntimeException("ReqHbt XML to ISO conversion failed", e);
        }
    }

    /* ===============================
       RESPHBT - XML to ISO 0810
       =============================== */
    public ISOMsg convertRespHbt(String xml) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(new ImpsIsoPackager());
            iso.setMTI("0810");

            String result = XmlUtil.read(xml, "//*[local-name()='Resp']/@result");

            iso.set(3, "990000");
            iso.set(11, generateStan());
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            iso.set(37, generateRrn());
            iso.set(39, "SUCCESS".equalsIgnoreCase(result) ? "00" : "96");
            iso.set(41, "IMPSTERM");

            return iso;

        } catch (Exception e) {
            throw new RuntimeException("RespHbt XML to ISO conversion failed", e);
        }
    }

    /* ===============================
       REQVALADD - XML to ISO 0200 (Account Validation / Name Enquiry)
       As per IMPS Specification
       =============================== */
    public ISOMsg convertReqValAdd(String xml) {
        try {
            Map<String, String> tags = XmlUtil.parseReqValAdd(xml);

            ISOMsg iso = new ISOMsg();
            iso.setPackager(new ImpsIsoPackager());
            iso.setMTI("0200");

            // DE3: Processing code for account validation
            iso.set(3, "310000");

            // DE11: STAN
            iso.set(11, generateStan());
            
            // DE12: Local time
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            
            // DE13: Local date
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));

            // DE33: Forwarding Institution (Payee IFSC)
            String ifsc = tags.get("payee_ifsc");
            if (ifsc != null && !ifsc.isEmpty()) {
                iso.set(33, ifsc);
            }

            // DE37: RRN
            iso.set(37, generateRrn());
            
            // DE41: Terminal ID
            iso.set(41, "IMPSTERM");
            
            // DE49: Currency code
            iso.set(49, "356");

            // DE102: Account to validate (Payee account for name enquiry)
            String acNum = tags.get("payee_acnum");
            if (acNum != null && !acNum.isEmpty()) {
                iso.set(102, acNum);
            }

            // DE120: Transaction ID
            String txnId = tags.get("txnId");
            if (txnId != null && !txnId.isEmpty()) {
                iso.set(120, txnId);
            }

            return iso;

        } catch (Exception e) {
            throw new RuntimeException("ReqValAdd XML to ISO conversion failed", e);
        }
    }

    /* ===============================
       RESPVALADD - XML to ISO 0210
       =============================== */
    public ISOMsg convertRespValAdd(String xml) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(new ImpsIsoPackager());
            iso.setMTI("0210");

            String result = XmlUtil.read(xml, "//*[local-name()='Resp']/@result");
            String acNum = XmlUtil.read(xml, "//*[local-name()='Resp']/@acNum");
            String ifsc = XmlUtil.read(xml, "//*[local-name()='Resp']/@IFSC");
            String approvalNum = XmlUtil.read(xml, "//*[local-name()='Resp']/@approvalNum");

            iso.set(3, "310000");
            iso.set(11, generateStan());
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            iso.set(37, generateRrn());
            iso.set(38, normalizeDe38(approvalNum));
            iso.set(39, "SUCCESS".equalsIgnoreCase(result) ? "00" : "14");
            iso.set(41, "IMPSTERM");

            if (acNum != null) {
                iso.set(102, acNum);
            }
            if (ifsc != null) {
                iso.set(33, ifsc);
            }

            return iso;

        } catch (Exception e) {
            throw new RuntimeException("RespValAdd XML to ISO conversion failed", e);
        }
    }

    /* ===============================
       REQLISTACCPVD - XML to ISO 0200 (List Request)
       =============================== */
    public ISOMsg convertReqListAccPvd(String xml) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(new ImpsIsoPackager());
            iso.setMTI("0200");

            // Processing code for list request
            iso.set(3, "320000");
            iso.set(11, generateStan());
            iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
            iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
            iso.set(37, generateRrn());
            iso.set(41, "IMPSTERM");

            return iso;

        } catch (Exception e) {
            throw new RuntimeException("ReqListAccPvd XML to ISO conversion failed", e);
        }
    }

    /* ===============================
       HELPER METHODS
       =============================== */
    private String amountToPaise(String amount) {
        if (amount == null || amount.isBlank()) {
            return "000000000000";
        }
        try {
            double rupees = Double.parseDouble(amount.trim());
            long paise = (long) (rupees * 100);
            return String.format("%012d", paise);
        } catch (NumberFormatException e) {
            return "000000000000";
        }
    }

    private String generateStan() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String generateRrn() {
        return String.valueOf(System.currentTimeMillis()).substring(1, 13);
    }

    /** ISO DE38: Authorization/Approval ID — max 6 chars. Truncate or pad to 6. */
    private String normalizeDe38(String approvalNum) {
        if (approvalNum == null || approvalNum.isBlank()) {
            return "000000";
        }
        String s = approvalNum.trim();
        if (s.length() > 6) {
            return s.substring(s.length() - 6); // last 6 (keeps numeric suffix when e.g. "APPROVED")
        }
        return String.format("%-6s", s).replace(' ', '0').substring(0, 6); // pad right with 0 to 6
    }

    /** ISO DE39: Response Code — max 2 chars. Map NPCI codes (e.g. U30) to 2 digits. */
    private String normalizeDe39(String result, String respCode) {
        if ("SUCCESS".equalsIgnoreCase(result)) {
            return "00";
        }
        if (respCode == null || respCode.isBlank()) {
            return "96";
        }
        String s = respCode.trim();
        if (s.length() > 2) {
            return s.substring(s.length() - 2); // "U30" -> "30", "51" -> "51"
        }
        return s.length() == 1 ? "0" + s : s;
    }
}
