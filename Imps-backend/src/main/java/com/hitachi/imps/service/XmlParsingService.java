package com.hitachi.imps.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hitachi.imps.entity.XmlPathReqPay;
import com.hitachi.imps.repository.XmlPathReqPayRepository;
import com.hitachi.imps.service.iso.XmlUtil;

/**
 * Service for parsing IMPS XML messages.
 * Extracts key fields from NPCI XML format as per IMPS specification.
 */
@Service
public class XmlParsingService {

    @Autowired
    private XmlPathReqPayRepository repo;

    @Autowired
    private XmlUtil xmlUtil;

    /* =========================================================
       NPCI FINANCIAL API - ReqPay
       Uses DB-driven XPath (NPCI compliant)
       ========================================================= */
    public Map<String, String> parseNpcIReqPay(String xml) {
        Map<String, String> data = new HashMap<>();

        // Load ACTIVE XPath rules from DB
        List<XmlPathReqPay> rules = repo.findByStatus("ACTIVE");
        for (XmlPathReqPay rule : rules) {
            String value = xmlUtil.read(xml, rule.getxPath());
            data.put(rule.getName(), value);
        }

        // Mandatory header attributes
        data.put("msg_id", xmlUtil.read(xml, "//*[local-name()='Head']/@msgId"));
        data.put("org_id", xmlUtil.read(xml, "//*[local-name()='Head']/@orgId"));
        data.put("prod_type", xmlUtil.read(xml, "//*[local-name()='Head']/@prodType"));

        // Transaction attributes
        data.put("txn_id", xmlUtil.read(xml, "//*[local-name()='Txn']/@id"));
        data.put("txn_type", xmlUtil.read(xml, "//*[local-name()='Txn']/@type"));

        return data;
    }

    /* =========================================================
       CHECK STATUS API - ReqChkTxn
       ========================================================= */
    public Map<String, String> parseReqChkTxn(String xml) {
        Map<String, String> map = new HashMap<>();

        map.put("msg_id", xmlUtil.read(xml, "//*[local-name()='Head']/@msgId"));
        map.put("orgTxnId", xmlUtil.read(xml, "//*[local-name()='Txn']/@orgTxnId"));
        map.put("orgRrn", xmlUtil.read(xml, "//*[local-name()='Txn']/@orgRrn"));
        map.put("orgTxnDate", xmlUtil.read(xml, "//*[local-name()='Txn']/@orgTxnDate"));

        return map;
    }

    /* =========================================================
       NAME ENQUIRY API - ReqValAdd
       ========================================================= */
    public Map<String, String> parseReqValAdd(String xml) {
        Map<String, String> map = new HashMap<>();

        map.put("msg_id", xmlUtil.read(xml, "//*[local-name()='Head']/@msgId"));
        
        // Payee account details for name enquiry
        map.put("ACNUM", xmlUtil.read(xml, 
            "//*[local-name()='Payee']//*[local-name()='Detail'][@name='ACNUM']/@value"));
        map.put("IFSC", xmlUtil.read(xml, 
            "//*[local-name()='Payee']//*[local-name()='Detail'][@name='IFSC']/@value"));

        return map;
    }

    /* =========================================================
       GENERIC HELPER - Extract msgId (ACK usage)
       ========================================================= */
    public String extractMsgId(String xml) {
        return xmlUtil.read(xml, "//*[local-name()='Head']/@msgId");
    }

    /* =========================================================
       GENERIC HELPER - Extract txnId (Transaction ID)
       ========================================================= */
    public String extractTxnId(String xml) {
        return xmlUtil.read(xml, "//*[local-name()='Txn']/@id");
    }

    /* =========================================================
       HEARTBEAT API - ReqHbt (NPCI SYSTEM MONITORING)
       ========================================================= */
    public Map<String, String> parseReqHbt(String xml) {
        Map<String, String> map = new HashMap<>();

        map.put("msg_id", xmlUtil.read(xml, "//*[local-name()='Head']/@msgId"));
        map.put("org_id", xmlUtil.read(xml, "//*[local-name()='Head']/@orgId"));
        map.put("txn_id", xmlUtil.read(xml, "//*[local-name()='Txn']/@id"));
        map.put("note", xmlUtil.read(xml, "//*[local-name()='Txn']/@note"));
        map.put("ref_id", xmlUtil.read(xml, "//*[local-name()='Txn']/@refId"));
        map.put("txn_ts", xmlUtil.read(xml, "//*[local-name()='Txn']/@ts"));
        map.put("hbt_type", xmlUtil.read(xml, "//*[local-name()='HbtMsg']/@type"));
        map.put("hbt_value", xmlUtil.read(xml, "//*[local-name()='HbtMsg']/@value"));

        return map;
    }

    /* =========================================================
       LIST ACCOUNT PROVIDER API - ReqListAccPvd
       ========================================================= */
    public Map<String, String> parseReqListAccPvd(String xml) {
        Map<String, String> map = new HashMap<>();

        map.put("msg_id", xmlUtil.read(xml, "//*[local-name()='Head']/@msgId"));
        map.put("org_id", xmlUtil.read(xml, "//*[local-name()='Head']/@orgId"));
        map.put("txn_id", xmlUtil.read(xml, "//*[local-name()='Txn']/@id"));

        return map;
    }
}
