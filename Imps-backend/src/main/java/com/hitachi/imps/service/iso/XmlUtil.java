package com.hitachi.imps.service.iso;

import javax.xml.xpath.*;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.parsers.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component
public class XmlUtil {

    /* ===============================
       GENERIC XPATH READER
       =============================== */
    public static String read(String xml, String xPathExp) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(
                new java.io.ByteArrayInputStream(xml.getBytes())
            );

            XPath xpath = XPathFactory.newInstance().newXPath();
            return xpath.evaluate(xPathExp, doc);

        } catch (Exception e) {
            return "";
        }
    }

    /* ===============================
       PARSE NPCI REQPAY → TAG MAP
       As per IMPS Specification
       =============================== */
    public static Map<String, String> parseReqPay(String xml) {

        Map<String, String> tags = new HashMap<>();

        // Header attributes
        tags.put("msgId", read(xml, "//*[local-name()='Head']/@msgId"));
        tags.put("orgId", read(xml, "//*[local-name()='Head']/@orgId"));
        tags.put("ts", read(xml, "//*[local-name()='Head']/@ts"));

        // Transaction attributes
        tags.put("txnId", read(xml, "//*[local-name()='Txn']/@id"));
        tags.put("txnType", read(xml, "//*[local-name()='Txn']/@type"));
        tags.put("custRef", read(xml, "//*[local-name()='Txn']/@custRef"));
        tags.put("note", read(xml, "//*[local-name()='Txn']/@note"));

        // Payer Amount
        tags.put("amount", read(xml, "//*[local-name()='Payer']//*[local-name()='Amount']/@value"));

        // Payer Account from Identity (pipe-separated ACNUM|IFSC)
        String identityId = read(xml, "//*[local-name()='Payer']//*[local-name()='Identity']/@id");
        if (identityId != null && identityId.contains("|")) {
            String[] parts = identityId.split("\\|");
            tags.put("payer_acnum", parts[0]);
            tags.put("payer_ifsc", parts.length > 1 ? parts[1] : "");
        } else {
            // Fallback to Detail tags
            tags.put("payer_acnum", read(xml, 
                "//*[local-name()='Payer']//*[local-name()='Detail'][@name='ACNUM']/@value"));
            tags.put("payer_ifsc", read(xml, 
                "//*[local-name()='Payer']//*[local-name()='Detail'][@name='IFSC']/@value"));
        }

        // Payer Account Type
        tags.put("payer_actype", read(xml, 
            "//*[local-name()='Payer']//*[local-name()='Detail'][@name='ACTYPE']/@value"));

        // Payee Account
        tags.put("payee_acnum", read(xml, 
            "//*[local-name()='Payee']//*[local-name()='Detail'][@name='ACNUM']/@value"));
        tags.put("payee_ifsc", read(xml, 
            "//*[local-name()='Payee']//*[local-name()='Detail'][@name='IFSC']/@value"));
        tags.put("payee_actype", read(xml, 
            "//*[local-name()='Payee']//*[local-name()='Detail'][@name='ACTYPE']/@value"));

        // Payer/Payee names
        tags.put("payer_name", read(xml, "//*[local-name()='Payer']/@name"));
        tags.put("payee_name", read(xml, "//*[local-name()='Payee']/@name"));

        return tags;
    }

    /* ===============================
       PARSE NPCI RESPPAY → TAG MAP
       =============================== */
    public static Map<String, String> parseRespPay(String xml) {
        Map<String, String> tags = new HashMap<>();

        tags.put("msgId", read(xml, "//*[local-name()='Head']/@msgId"));
        tags.put("reqMsgId", read(xml, "//*[local-name()='Resp']/@reqMsgId"));
        tags.put("result", read(xml, "//*[local-name()='Resp']/@result"));
        tags.put("respCode", read(xml, "//*[local-name()='Ref']/@respCode"));
        tags.put("approvalNum", read(xml, "//*[local-name()='Ref']/@approvalNum"));
        tags.put("settAmount", read(xml, "//*[local-name()='Ref']/@settAmount"));
        tags.put("acNum", read(xml, "//*[local-name()='Ref']/@acNum"));
        tags.put("IFSC", read(xml, "//*[local-name()='Ref']/@IFSC"));
        tags.put("regName", read(xml, "//*[local-name()='Ref']/@regName"));

        return tags;
    }

    /* ===============================
       PARSE NPCI REQCHKTXN → TAG MAP
       =============================== */
    public static Map<String, String> parseReqChkTxn(String xml) {
        Map<String, String> tags = new HashMap<>();

        tags.put("msgId", read(xml, "//*[local-name()='Head']/@msgId"));
        tags.put("orgId", read(xml, "//*[local-name()='Head']/@orgId"));
        tags.put("txnId", read(xml, "//*[local-name()='Txn']/@id"));
        tags.put("orgTxnId", read(xml, "//*[local-name()='Txn']/@orgTxnId"));
        tags.put("orgRrn", read(xml, "//*[local-name()='Txn']/@orgRrn"));
        tags.put("orgTxnDate", read(xml, "//*[local-name()='Txn']/@orgTxnDate"));
        tags.put("amount", read(xml, "//*[local-name()='Amount']/@value"));

        return tags;
    }

    /* ===============================
       PARSE NPCI REQVALADD → TAG MAP
       =============================== */
    public static Map<String, String> parseReqValAdd(String xml) {
        Map<String, String> tags = new HashMap<>();

        tags.put("msgId", read(xml, "//*[local-name()='Head']/@msgId"));
        tags.put("orgId", read(xml, "//*[local-name()='Head']/@orgId"));
        tags.put("txnId", read(xml, "//*[local-name()='Txn']/@id"));
        
        // Payee account details for validation
        tags.put("payee_acnum", read(xml, 
            "//*[local-name()='Payee']//*[local-name()='Detail'][@name='ACNUM']/@value"));
        tags.put("payee_ifsc", read(xml, 
            "//*[local-name()='Payee']//*[local-name()='Detail'][@name='IFSC']/@value"));
        tags.put("payee_actype", read(xml, 
            "//*[local-name()='Payee']//*[local-name()='Detail'][@name='ACTYPE']/@value"));

        return tags;
    }

    /* ===============================
       PARSE NPCI REQHBT → TAG MAP
       =============================== */
    public static Map<String, String> parseReqHbt(String xml) {
        Map<String, String> tags = new HashMap<>();

        tags.put("msgId", read(xml, "//*[local-name()='Head']/@msgId"));
        tags.put("orgId", read(xml, "//*[local-name()='Head']/@orgId"));
        tags.put("txnId", read(xml, "//*[local-name()='Txn']/@id"));
        tags.put("hbtType", read(xml, "//*[local-name()='HbtMsg']/@type"));
        tags.put("hbtValue", read(xml, "//*[local-name()='HbtMsg']/@value"));

        return tags;
    }

    /* ===============================
       PARSE NPCI REQLISTACCPVD → TAG MAP
       =============================== */
    public static Map<String, String> parseReqListAccPvd(String xml) {
        Map<String, String> tags = new HashMap<>();

        tags.put("msgId", read(xml, "//*[local-name()='Head']/@msgId"));
        tags.put("orgId", read(xml, "//*[local-name()='Head']/@orgId"));
        tags.put("txnId", read(xml, "//*[local-name()='Txn']/@id"));

        return tags;
    }

    /* ===============================
       AMOUNT → ISO DE4 (PAISE)
       =============================== */
    public static String amountToPaise(String amount) {
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

    /* ===============================
       STAN (DE11)
       =============================== */
    public static String randomStan() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    /* ===============================
       RRN (DE37)
       =============================== */
    public static String randomRrn() {
        return String.valueOf(System.currentTimeMillis())
                     .substring(1, 13);
    }
}
