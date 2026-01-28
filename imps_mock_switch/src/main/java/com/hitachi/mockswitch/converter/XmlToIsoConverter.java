package com.hitachi.mockswitch.converter;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

import com.hitachi.mockswitch.iso.MockIsoPackager;
import com.hitachi.mockswitch.service.iso.XmlUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

/**
 * Converts NPCI XML messages to ISO 8583 for Mock Switch.
 * When client sends application/xml, Mock Switch converts to ISO and processes.
 */
@Component
public class XmlToIsoConverter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMdd");
    private static final Random RANDOM = new Random();
    private static final MockIsoPackager PACKAGER = new MockIsoPackager();

    public byte[] convertReqPay(String xml) throws ISOException {
        ISOMsg iso = buildReqPayIso(xml);
        return iso.pack();
    }

    public byte[] convertRespPay(String xml) throws ISOException {
        ISOMsg iso = buildRespPayIso(xml);
        return iso.pack();
    }

    public byte[] convertReqChkTxn(String xml) throws ISOException {
        ISOMsg iso = buildReqChkTxnIso(xml);
        return iso.pack();
    }

    public byte[] convertRespChkTxn(String xml) throws ISOException {
        ISOMsg iso = buildRespChkTxnIso(xml);
        return iso.pack();
    }

    public byte[] convertReqHbt(String xml) throws ISOException {
        ISOMsg iso = buildReqHbtIso(xml);
        return iso.pack();
    }

    public byte[] convertRespHbt(String xml) throws ISOException {
        ISOMsg iso = buildRespHbtIso(xml);
        return iso.pack();
    }

    public byte[] convertReqValAdd(String xml) throws ISOException {
        ISOMsg iso = buildReqValAddIso(xml);
        return iso.pack();
    }

    public byte[] convertRespValAdd(String xml) throws ISOException {
        ISOMsg iso = buildRespValAddIso(xml);
        return iso.pack();
    }

    public byte[] convertReqListAccPvd(String xml) throws ISOException {
        ISOMsg iso = buildReqListAccPvdIso(xml);
        return iso.pack();
    }

    public byte[] convertRespListAccPvd(String xml) throws ISOException {
        ISOMsg iso = buildRespListAccPvdIso(xml);
        return iso.pack();
    }

    private ISOMsg newIso() {
        ISOMsg iso = new ISOMsg();
        iso.setPackager(PACKAGER);
        return iso;
    }

    private static String genStan() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private static String genRrn() {
        return String.valueOf(System.currentTimeMillis()).substring(1, 13);
    }

    private ISOMsg buildReqPayIso(String xml) throws ISOException {
        Map<String, String> tags = XmlUtil.parseReqPay(xml);
        ISOMsg iso = newIso();
        iso.setMTI("0200");
        iso.set(3, "400000");
        iso.set(4, XmlUtil.amountToPaise(tags.get("amount")));
        iso.set(11, genStan());
        iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
        iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
        String payerIfsc = tags.get("payer_ifsc");
        if (payerIfsc != null && !payerIfsc.isEmpty())
            iso.set(32, payerIfsc.length() >= 4 ? payerIfsc.substring(0, 4) : payerIfsc);
        String payeeIfsc = tags.get("payee_ifsc");
        if (payeeIfsc != null && !payeeIfsc.isEmpty()) iso.set(33, payeeIfsc);
        iso.set(37, genRrn());
        iso.set(41, "IMPSTERM");
        iso.set(49, "356");
        if (tags.get("payer_acnum") != null && !tags.get("payer_acnum").isEmpty())
            iso.set(102, tags.get("payer_acnum"));
        if (tags.get("payee_acnum") != null && !tags.get("payee_acnum").isEmpty())
            iso.set(103, tags.get("payee_acnum"));
        if (tags.get("txnId") != null && !tags.get("txnId").isEmpty())
            iso.set(120, tags.get("txnId"));
        return iso;
    }

    private ISOMsg buildRespPayIso(String xml) throws ISOException {
        Map<String, String> tags = XmlUtil.parseRespPay(xml);
        ISOMsg iso = newIso();
        iso.setMTI("0210");
        iso.set(3, "400000");
        iso.set(4, XmlUtil.amountToPaise(tags.get("settAmount")));
        iso.set(11, genStan());
        iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
        iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
        if (tags.get("IFSC") != null && !tags.get("IFSC").isEmpty()) iso.set(33, tags.get("IFSC"));
        iso.set(37, genRrn());
        iso.set(38, tags.get("approvalNum") != null && !tags.get("approvalNum").isEmpty() ? tags.get("approvalNum") : "000000");
        iso.set(39, "SUCCESS".equalsIgnoreCase(tags.get("result")) ? "00" : (tags.get("respCode") != null && !tags.get("respCode").isEmpty() ? tags.get("respCode") : "96"));
        iso.set(41, "IMPSTERM");
        iso.set(49, "356");
        if (tags.get("acNum") != null && !tags.get("acNum").isEmpty()) iso.set(103, tags.get("acNum"));
        return iso;
    }

    private ISOMsg buildReqChkTxnIso(String xml) throws ISOException {
        ISOMsg iso = newIso();
        iso.setMTI("0200");
        iso.set(3, "380000");
        String amount = XmlUtil.read(xml, "//*[local-name()='Amount']/@value");
        iso.set(4, XmlUtil.amountToPaise(amount));
        iso.set(11, genStan());
        iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
        iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
        String orgRrn = XmlUtil.read(xml, "//*[local-name()='Txn']/@orgRrn");
        iso.set(37, orgRrn != null && !orgRrn.isEmpty() ? orgRrn : genRrn());
        iso.set(41, "IMPSTERM");
        iso.set(49, "356");
        String orgTxnId = XmlUtil.read(xml, "//*[local-name()='Txn']/@orgTxnId");
        if (orgTxnId != null) iso.set(48, orgTxnId);
        return iso;
    }

    private ISOMsg buildRespChkTxnIso(String xml) throws ISOException {
        ISOMsg iso = newIso();
        iso.setMTI("0210");
        iso.set(3, "380000");
        iso.set(11, genStan());
        iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
        iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
        iso.set(37, genRrn());
        iso.set(38, XmlUtil.read(xml, "//*[local-name()='Ref']/@approvalNum"));
        if (iso.getString(38) == null || iso.getString(38).isEmpty()) iso.set(38, "000000");
        String result = XmlUtil.read(xml, "//*[local-name()='Resp']/@result");
        String respCode = XmlUtil.read(xml, "//*[local-name()='Ref']/@respCode");
        iso.set(39, "SUCCESS".equalsIgnoreCase(result) ? "00" : (respCode != null ? respCode : "96"));
        iso.set(41, "IMPSTERM");
        iso.set(49, "356");
        return iso;
    }

    private ISOMsg buildReqHbtIso(String xml) throws ISOException {
        ISOMsg iso = newIso();
        iso.setMTI("0800");
        iso.set(3, "990000");
        iso.set(11, genStan());
        iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
        iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
        iso.set(37, genRrn());
        iso.set(41, "IMPSTERM");
        iso.set(24, "831");
        String hbtType = XmlUtil.read(xml, "//*[local-name()='HbtMsg']/@type");
        if (hbtType != null) iso.set(48, hbtType);
        return iso;
    }

    private ISOMsg buildRespHbtIso(String xml) throws ISOException {
        ISOMsg iso = newIso();
        iso.setMTI("0810");
        iso.set(3, "990000");
        iso.set(11, genStan());
        iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
        iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
        iso.set(37, genRrn());
        iso.set(39, "SUCCESS".equalsIgnoreCase(XmlUtil.read(xml, "//*[local-name()='Resp']/@result")) ? "00" : "96");
        iso.set(41, "IMPSTERM");
        return iso;
    }

    private ISOMsg buildReqValAddIso(String xml) throws ISOException {
        Map<String, String> tags = XmlUtil.parseReqValAdd(xml);
        ISOMsg iso = newIso();
        iso.setMTI("0200");
        iso.set(3, "310000");
        iso.set(11, genStan());
        iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
        iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
        if (tags.get("payee_ifsc") != null && !tags.get("payee_ifsc").isEmpty()) iso.set(33, tags.get("payee_ifsc"));
        iso.set(37, genRrn());
        iso.set(41, "IMPSTERM");
        iso.set(49, "356");
        if (tags.get("payee_acnum") != null && !tags.get("payee_acnum").isEmpty()) iso.set(102, tags.get("payee_acnum"));
        if (tags.get("txnId") != null && !tags.get("txnId").isEmpty()) iso.set(120, tags.get("txnId"));
        return iso;
    }

    private ISOMsg buildRespValAddIso(String xml) throws ISOException {
        ISOMsg iso = newIso();
        iso.setMTI("0210");
        iso.set(3, "310000");
        iso.set(11, genStan());
        iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
        iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
        iso.set(37, genRrn());
        iso.set(38, XmlUtil.read(xml, "//*[local-name()='Resp']/@approvalNum"));
        if (iso.getString(38) == null || iso.getString(38).isEmpty()) iso.set(38, "000000");
        iso.set(39, "SUCCESS".equalsIgnoreCase(XmlUtil.read(xml, "//*[local-name()='Resp']/@result")) ? "00" : "14");
        iso.set(41, "IMPSTERM");
        String acNum = XmlUtil.read(xml, "//*[local-name()='Resp']/@acNum");
        String ifsc = XmlUtil.read(xml, "//*[local-name()='Resp']/@IFSC");
        if (acNum != null) iso.set(102, acNum);
        if (ifsc != null) iso.set(33, ifsc);
        // DE120: Txn id so IMPS backend can match response to transaction
        String txnId = XmlUtil.read(xml, "//*[local-name()='Txn']/@id");
        if (txnId != null && !txnId.isEmpty()) iso.set(120, txnId);
        return iso;
    }

    private ISOMsg buildReqListAccPvdIso(String xml) throws ISOException {
        ISOMsg iso = newIso();
        iso.setMTI("0200");
        iso.set(3, "320000");
        iso.set(11, genStan());
        iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
        iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
        iso.set(37, genRrn());
        iso.set(41, "IMPSTERM");
        return iso;
    }

    private ISOMsg buildRespListAccPvdIso(String xml) throws ISOException {
        ISOMsg iso = newIso();
        iso.setMTI("0210");
        iso.set(3, "320000");
        iso.set(11, genStan());
        iso.set(12, LocalDateTime.now().format(TIME_FORMAT));
        iso.set(13, LocalDateTime.now().format(DATE_FORMAT));
        iso.set(37, genRrn());
        iso.set(39, "SUCCESS".equalsIgnoreCase(XmlUtil.read(xml, "//*[local-name()='Resp']/@result")) ? "00" : "96");
        iso.set(41, "IMPSTERM");
        return iso;
    }
}
