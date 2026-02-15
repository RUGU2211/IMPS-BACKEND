package com.hitachi.npci.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Receives XML from IMPS Backend (when it forwards Switch→IMPS requests to NPCI mock) and returns ACK.
 * Paths used by Imps-backend: /imps/reqpay/{txnId}, /imps/reqchktxn/{txnId}, /imps/reqvaladd/{txnId}, /imps/hbt/req/{txnId}.
 * Also supports /imps/pay/req/, /imps/chktxn/req/, etc.
 */
@RestController
@RequestMapping("/imps")
public class ImpsMockController {

    @PostMapping(value = { "/reqpay/{txnId}", "/pay/req/{txnId}" }, consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String payReq(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        System.out.println("========== [NPCI] IMPS → NPCI | REQ received | ReqPay ==========");
        System.out.println("  REQ received from: IMPS | TxnId: " + txnId);
        System.out.println("  ---------- XML (first 300) ----------");
        System.out.println("  " + (xml != null && xml.length() > 300 ? xml.substring(0, 300) + "..." : xml));
        String respPay = buildRespPaySuccess(xml, txnId);
        System.out.println("========== [NPCI] NPCI → IMPS | RESP sent | RespPay (SUCCESS) ==========");
        System.out.println("  RESP sent to: IMPS | connection will close");
        System.out.println("==========================================");
        return respPay;
    }

    @PostMapping(value = "/pay/resp/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String payResp(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("RespPay", txnId, xml, request);
    }

    @PostMapping(value = { "/reqchktxn/{txnId}", "/chktxn/req/{txnId}" }, consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String chktxnReq(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("ReqChkTxn", txnId, xml, request);
    }

    @PostMapping(value = "/chktxn/resp/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String chktxnResp(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("RespChkTxn", txnId, xml, request);
    }

    @PostMapping(value = "/hbt/req/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String hbtReq(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("ReqHbt", txnId, xml, request);
    }

    @PostMapping(value = "/hbt/resp/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String hbtResp(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("RespHbt", txnId, xml, request);
    }

    @PostMapping(value = "/listaccpvd/req/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String listaccpvdReq(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("ReqListAccPvd", txnId, xml, request);
    }

    @PostMapping(value = "/listaccpvd/resp/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String listaccpvdResp(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("RespListAccPvd", txnId, xml, request);
    }

    @PostMapping(value = { "/reqvaladd/{txnId}", "/valadd/req/{txnId}" }, consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String valaddReq(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("ReqValAdd", txnId, xml, request);
    }

    @PostMapping(value = "/valadd/resp/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String valaddResp(@PathVariable String txnId, @RequestBody String xml, HttpServletRequest request) {
        return logAndAck("RespValAdd", txnId, xml, request);
    }

    private String logAndAck(String api, String txnId, String xml, HttpServletRequest request) {
        System.out.println("========== [NPCI] IMPS → NPCI | REQ received | " + api + " ==========");
        System.out.println("  REQ received from: IMPS | TxnId: " + txnId);
        System.out.println("========== [NPCI] NPCI → IMPS | RESP (ACK) sent ==========");
        System.out.println("  RESP sent to: IMPS | connection will close");
        System.out.println("  XML (first 500): " + (xml != null && xml.length() > 500 ? xml.substring(0, 500) + "..." : xml));
        System.out.println("==========================================");
        String msgId = extractMsgId(xml);
        String ack = buildAck(api, msgId != null ? msgId : txnId);
        return ack;
    }

    private String extractMsgId(String xml) {
        try {
            int i = xml.indexOf("msgId=\"");
            if (i != -1) {
                i += 7;
                int j = xml.indexOf("\"", i);
                if (j != -1) return xml.substring(i, j);
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    private String buildAck(String api, String reqMsgId) {
        String ts = java.time.OffsetDateTime.now().toString();
        return String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<ns2:Ack xmlns:ns2=\"http://npci.org/upi/schema/\" xmlns:ns3=\"http://npci.org/cm/schema/\" api=\"%s\" reqMsgId=\"%s\" ts=\"%s\">\n</ns2:Ack>",
            api, reqMsgId != null ? reqMsgId : "", ts
        );
    }

    /** Build minimal RespPay SUCCESS so IMPS can convert to ISO and return to Switch. */
    private String buildRespPaySuccess(String reqPayXml, String txnId) {
        String msgId = extractMsgId(reqPayXml);
        String reqMsgId = msgId != null ? msgId : (txnId != null ? txnId : "");
        String ts = java.time.OffsetDateTime.now().toString();
        String approvalNum = String.format("%06d", (int)(Math.random() * 1_000_000));
        String amount = extractAmount(reqPayXml);
        String payeeAcNum = extractPayeeAcNum(reqPayXml);
        String payeeIfsc = extractPayeeIfsc(reqPayXml);
        String respMsgId = "R" + (reqMsgId.length() >= 34 ? reqMsgId.substring(1, 34) : reqMsgId);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<ns2:RespPay xmlns:ns2=\"http://npci.org/upi/schema/\">\n" +
            "<Head ver=\"2.0\" ts=\"" + escapeXml(ts) + "\" orgId=\"NPCI\" msgId=\"" + escapeXml(respMsgId) + "\" prodType=\"IMPS\"/>\n" +
            "<Txn id=\"" + escapeXml(txnId != null ? txnId : "") + "\" note=\"Mock\" refId=\"\" custRef=\"\" refUrl=\"\" ts=\"" + escapeXml(ts) + "\" purpose=\"\" type=\"PAY\" subType=\"PAY\" initiationMode=\"\" refCategory=\"\"/>\n" +
            "<Resp reqMsgId=\"" + escapeXml(reqMsgId) + "\" result=\"SUCCESS\">\n" +
            "<Ref type=\"PAYEE\" seqNum=\"1\" addr=\"" + escapeXml(payeeAcNum != null ? payeeAcNum : "") + "@bank\" regName=\"BENEFICIARY\" acNum=\"" + escapeXml(payeeAcNum != null ? payeeAcNum : "") + "\" IFSC=\"" + escapeXml(payeeIfsc != null ? payeeIfsc : "") + "\" code=\"0000\" accType=\"SAVINGS\" settAmount=\"" + escapeXml(amount != null ? amount : "1000.00") + "\" orgAmount=\"" + escapeXml(amount != null ? amount : "1000.00") + "\" settCurrency=\"INR\" approvalNum=\"" + approvalNum + "\" respCode=\"00\"/>\n" +
            "</Resp>\n" +
            "</ns2:RespPay>";
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String extractAmount(String xml) {
        try {
            int i = xml.indexOf("Amount value=\"");
            if (i != -1) {
                i += 14;
                int j = xml.indexOf("\"", i);
                if (j != -1) return xml.substring(i, j);
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    private String extractPayeeAcNum(String xml) {
        try {
            int payeeStart = xml.indexOf("<Payee");
            if (payeeStart == -1) return null;
            int acnum = xml.indexOf("name=\"ACNUM\" value=\"", payeeStart);
            if (acnum != -1) {
                acnum += 19;
                int end = xml.indexOf("\"", acnum);
                if (end != -1) return xml.substring(acnum, end);
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    private String extractPayeeIfsc(String xml) {
        try {
            int payeeStart = xml.indexOf("<Payee");
            if (payeeStart == -1) return null;
            int ifsc = xml.indexOf("name=\"IFSC\" value=\"", payeeStart);
            if (ifsc != -1) {
                ifsc += 18;
                int end = xml.indexOf("\"", ifsc);
                if (end != -1) return xml.substring(ifsc, end);
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }
}
