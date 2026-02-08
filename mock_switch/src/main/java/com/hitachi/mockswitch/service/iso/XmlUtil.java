package com.hitachi.mockswitch.service.iso;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * XML parsing utility for NPCI IMPS XML messages.
 * Used when Mock Switch accepts application/xml and converts to ISO.
 */
public final class XmlUtil {

    private XmlUtil() {}

    public static String read(String xml, String xPathExp) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            XPath xpath = XPathFactory.newInstance().newXPath();
            return xpath.evaluate(xPathExp, doc);
        } catch (Exception e) {
            return "";
        }
    }

    public static Map<String, String> parseReqPay(String xml) {
        Map<String, String> tags = new HashMap<>();
        tags.put("msgId", read(xml, "//*[local-name()='Head']/@msgId"));
        tags.put("orgId", read(xml, "//*[local-name()='Head']/@orgId"));
        tags.put("ts", read(xml, "//*[local-name()='Head']/@ts"));
        tags.put("txnId", read(xml, "//*[local-name()='Txn']/@id"));
        tags.put("txnType", read(xml, "//*[local-name()='Txn']/@type"));
        tags.put("custRef", read(xml, "//*[local-name()='Txn']/@custRef"));
        tags.put("note", read(xml, "//*[local-name()='Txn']/@note"));
        tags.put("amount", read(xml, "//*[local-name()='Payer']//*[local-name()='Amount']/@value"));
        String identityId = read(xml, "//*[local-name()='Payer']//*[local-name()='Identity']/@id");
        if (identityId != null && identityId.contains("|")) {
            String[] parts = identityId.split("\\|");
            tags.put("payer_acnum", parts[0]);
            tags.put("payer_ifsc", parts.length > 1 ? parts[1] : "");
        } else {
            tags.put("payer_acnum", read(xml, "//*[local-name()='Payer']//*[local-name()='Detail'][@name='ACNUM']/@value"));
            tags.put("payer_ifsc", read(xml, "//*[local-name()='Payer']//*[local-name()='Detail'][@name='IFSC']/@value"));
        }
        tags.put("payer_actype", read(xml, "//*[local-name()='Payer']//*[local-name()='Detail'][@name='ACTYPE']/@value"));
        tags.put("payee_acnum", read(xml, "//*[local-name()='Payee']//*[local-name()='Detail'][@name='ACNUM']/@value"));
        tags.put("payee_ifsc", read(xml, "//*[local-name()='Payee']//*[local-name()='Detail'][@name='IFSC']/@value"));
        tags.put("payee_actype", read(xml, "//*[local-name()='Payee']//*[local-name()='Detail'][@name='ACTYPE']/@value"));
        tags.put("payer_name", read(xml, "//*[local-name()='Payer']/@name"));
        tags.put("payee_name", read(xml, "//*[local-name()='Payee']/@name"));
        return tags;
    }

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

    public static Map<String, String> parseReqValAdd(String xml) {
        Map<String, String> tags = new HashMap<>();
        tags.put("msgId", read(xml, "//*[local-name()='Head']/@msgId"));
        tags.put("orgId", read(xml, "//*[local-name()='Head']/@orgId"));
        tags.put("txnId", read(xml, "//*[local-name()='Txn']/@id"));
        tags.put("payee_acnum", read(xml, "//*[local-name()='Payee']//*[local-name()='Detail'][@name='ACNUM']/@value"));
        tags.put("payee_ifsc", read(xml, "//*[local-name()='Payee']//*[local-name()='Detail'][@name='IFSC']/@value"));
        tags.put("payee_actype", read(xml, "//*[local-name()='Payee']//*[local-name()='Detail'][@name='ACTYPE']/@value"));
        return tags;
    }

    public static String amountToPaise(String amount) {
        if (amount == null || amount.isBlank()) return "000000000000";
        try {
            long paise = (long) (Double.parseDouble(amount.trim()) * 100);
            return String.format("%012d", paise);
        } catch (NumberFormatException e) {
            return "000000000000";
        }
    }

    public static String randomStan() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    public static String randomRrn() {
        return String.valueOf(System.currentTimeMillis()).substring(1, 13);
    }
}
