package com.hitachi.imps.converter;

import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.util.IsoUtil;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Converter for transforming ISO 8583 messages to NPCI XML format.
 * Supports all IMPS API types: RespPay, RespChkTxn, RespHbt, RespValAdd, RespListAccPvd
 */
@Component
public class IsoToXmlConverter {

    private static final String NAMESPACE = "http://npci.org/upi/schema/";
    private static final String PROD_TYPE = "IMPS";
    private static final String API_VERSION = "2.0";

    /* ===============================
       REQPAY - ISO 0200 to XML
       (When Switch initiates a request)
       =============================== */
    public String convertReqPayToXml(byte[] isoBytes) {
        ISOMsg iso = unpackIso(isoBytes);
        return convertReqPayToXml(iso);
    }

    public String convertReqPayToXml(ISOMsg iso) {
        try {
            String txnId = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            String msgId = "MSG" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            String amount = paiseToRupees(iso.getString(4));
            String payerAc = iso.getString(102);
            String payeeAc = iso.getString(103);
            String payeeIfsc = iso.getString(33);

            return """
                <ns2:ReqPay xmlns:ns2="%s">
                    <Head ver="%s" ts="%s" orgId="SWITCH" msgId="%s" prodType="%s"/>
                    <Txn id="%s" note="Switch Request" custRef="%s" type="PAY" ts="%s"/>
                    <Payer addr="switch@bank" name="SWITCH_PAYER" seqNum="1" type="ENTITY" code="0000">
                        <Ac addrType="ACCOUNT">
                            <Detail name="ACNUM" value="%s"/>
                            <Detail name="ACTYPE" value="SAVINGS"/>
                        </Ac>
                        <Amount value="%s" curr="INR"/>
                    </Payer>
                    <Payees>
                        <Payee seqNum="1" type="PERSON" code="0000">
                            <Ac addrType="ACCOUNT">
                                <Detail name="IFSC" value="%s"/>
                                <Detail name="ACNUM" value="%s"/>
                                <Detail name="ACTYPE" value="SAVINGS"/>
                            </Ac>
                            <Amount value="%s" curr="INR"/>
                        </Payee>
                    </Payees>
                </ns2:ReqPay>
                """.formatted(
                    NAMESPACE, API_VERSION, OffsetDateTime.now(), msgId, PROD_TYPE,
                    txnId, iso.getString(37), OffsetDateTime.now(),
                    payerAc != null ? payerAc : "",
                    amount,
                    payeeIfsc != null ? payeeIfsc : "",
                    payeeAc != null ? payeeAc : "",
                    amount
                );
        } catch (Exception e) {
            throw new RuntimeException("ISO to ReqPay XML conversion failed", e);
        }
    }

    /* ===============================
       RESPPAY - ISO 0210 to XML
       =============================== */
    public String convertRespPayToXml(byte[] isoBytes) {
        ISOMsg iso = unpackIso(isoBytes);
        return convertRespPayToXml(iso);
    }

    public String convertRespPayToXml(ISOMsg iso) {
        try {
            String msgId = "MSG" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            String responseCode = iso.getString(39);
            String result = "00".equals(responseCode) ? "SUCCESS" : "FAILURE";
            String approvalNum = iso.getString(38);
            String amount = paiseToRupees(iso.getString(4));
            String acNum = iso.getString(103);
            String ifsc = iso.getString(33);

            return """
                <ns2:RespPay xmlns:ns2="%s">
                    <Head ver="%s" ts="%s" orgId="SWITCH" msgId="%s" prodType="%s"/>
                    <Txn id="%s" note="Response" type="CREDIT" ts="%s"/>
                    <Resp reqMsgId="%s" result="%s">
                        <Ref type="PAYEE" seqNum="1"
                             addr="%s"
                             settAmount="%s"
                             orgAmount="%s"
                             settCurrency="INR"
                             acNum="%s"
                             IFSC="%s"
                             code="0000"
                             accType="SAVINGS"
                             approvalNum="%s"
                             respCode="%s"
                             regName="BENEFICIARY"/>
                    </Resp>
                </ns2:RespPay>
                """.formatted(
                    NAMESPACE, API_VERSION, OffsetDateTime.now(), msgId, PROD_TYPE,
                    iso.getString(37), OffsetDateTime.now(),
                    iso.getString(11),
                    result,
                    acNum != null ? acNum + "@bank.ifsc.npci" : "",
                    amount, amount,
                    acNum != null ? acNum : "",
                    ifsc != null ? ifsc : "",
                    approvalNum != null ? approvalNum : "000000",
                    responseCode != null ? responseCode : "00"
                );
        } catch (Exception e) {
            throw new RuntimeException("ISO to RespPay XML conversion failed", e);
        }
    }

    /* ===============================
       REQCHKTXN - ISO 0200 to XML
       =============================== */
    public String convertReqChkTxnToXml(byte[] isoBytes) {
        ISOMsg iso = unpackIso(isoBytes);
        return convertReqChkTxnToXml(iso);
    }

    public String convertReqChkTxnToXml(ISOMsg iso) {
        try {
            String msgId = "MSG" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            String txnId = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            String orgTxnId = iso.getString(48);
            String orgRrn = iso.getString(37);
            String amount = paiseToRupees(iso.getString(4));

            return """
                <ns2:ReqChkTxn xmlns:ns2="%s">
                    <Head ver="%s" ts="%s" orgId="SWITCH" msgId="%s" prodType="%s"/>
                    <Txn id="%s" note="Status Check" custRef="%s" orgTxnId="%s" orgRrn="%s" type="VR" ts="%s"/>
                    <Payer addr="switch@bank" name="SWITCH" seqNum="1" type="ENTITY" code="0000">
                        <Amount value="%s" curr="INR"/>
                    </Payer>
                </ns2:ReqChkTxn>
                """.formatted(
                    NAMESPACE, API_VERSION, OffsetDateTime.now(), msgId, PROD_TYPE,
                    txnId, orgRrn != null ? orgRrn : "", orgTxnId != null ? orgTxnId : "", orgRrn != null ? orgRrn : "",
                    OffsetDateTime.now(), amount
                );
        } catch (Exception e) {
            throw new RuntimeException("ISO to ReqChkTxn XML conversion failed", e);
        }
    }

    /* ===============================
       RESPCHKTXN - ISO 0210 to XML
       =============================== */
    public String convertRespChkTxnToXml(byte[] isoBytes) {
        ISOMsg iso = unpackIso(isoBytes);
        return convertRespChkTxnToXml(iso);
    }

    public String convertRespChkTxnToXml(ISOMsg iso) {
        try {
            String msgId = "MSG" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            String responseCode = iso.getString(39);
            String result = "00".equals(responseCode) ? "SUCCESS" : "FAILURE";
            String approvalNum = iso.getString(38);
            String amount = paiseToRupees(iso.getString(4));

            return """
                <ns2:RespChkTxn xmlns:ns2="%s">
                    <Head ver="%s" ts="%s" orgId="SWITCH" msgId="%s" prodType="%s"/>
                    <Txn id="%s" note="Status Response" type="ChkBankStatus" ts="%s"/>
                    <Resp reqMsgId="%s" result="%s">
                        <Ref type="PAYEE" seqNum="1"
                             settAmount="%s"
                             orgAmount="%s"
                             settCurrency="INR"
                             approvalNum="%s"
                             respCode="%s"/>
                    </Resp>
                </ns2:RespChkTxn>
                """.formatted(
                    NAMESPACE, API_VERSION, OffsetDateTime.now(), msgId, PROD_TYPE,
                    iso.getString(37), OffsetDateTime.now(),
                    iso.getString(11), result,
                    amount, amount,
                    approvalNum != null ? approvalNum : "000000",
                    responseCode != null ? responseCode : "00"
                );
        } catch (Exception e) {
            throw new RuntimeException("ISO to RespChkTxn XML conversion failed", e);
        }
    }

    /* ===============================
       REQHBT - ISO 0800 to XML
       =============================== */
    public String convertReqHbtToXml(byte[] isoBytes) {
        ISOMsg iso = unpackIso(isoBytes);
        return convertReqHbtToXml(iso);
    }

    public String convertReqHbtToXml(ISOMsg iso) {
        try {
            String msgId = "MSG" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            String txnId = "HBT" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            String hbtType = iso.getString(48);

            return """
                <upi:ReqHbt xmlns:upi="%s">
                    <Head ver="1.0" ts="%s" orgId="SWITCH" msgId="%s"/>
                    <Txn id="%s" note="Heartbeat" refId="" refUrl="" ts="%s" type="Hbt"/>
                    <HbtMsg type="%s" value="NA"/>
                </upi:ReqHbt>
                """.formatted(
                    NAMESPACE, OffsetDateTime.now(), msgId,
                    txnId, OffsetDateTime.now(),
                    hbtType != null ? hbtType : "ALIVE"
                );
        } catch (Exception e) {
            throw new RuntimeException("ISO to ReqHbt XML conversion failed", e);
        }
    }

    /* ===============================
       RESPHBT - ISO 0810 to XML
       =============================== */
    public String convertRespHbtToXml(byte[] isoBytes) {
        ISOMsg iso = unpackIso(isoBytes);
        return convertRespHbtToXml(iso);
    }

    public String convertRespHbtToXml(ISOMsg iso) {
        try {
            String msgId = "MSG" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            String responseCode = iso.getString(39);
            String result = "00".equals(responseCode) ? "SUCCESS" : "FAILURE";

            return """
                <upi:RespHbt xmlns:upi="%s">
                    <Head ver="1.0" ts="%s" orgId="SWITCH" msgId="%s"/>
                    <Txn id="%s" note="Heartbeat Response" refId="" refUrl="" ts="%s" type="Hbt"/>
                    <Resp reqMsgId="%s" result="%s"/>
                </upi:RespHbt>
                """.formatted(
                    NAMESPACE, OffsetDateTime.now(), msgId,
                    iso.getString(37), OffsetDateTime.now(),
                    iso.getString(11), result
                );
        } catch (Exception e) {
            throw new RuntimeException("ISO to RespHbt XML conversion failed", e);
        }
    }

    /* ===============================
       REQVALADD - ISO 0200 to XML
       =============================== */
    public String convertReqValAddToXml(byte[] isoBytes) {
        ISOMsg iso = unpackIso(isoBytes);
        return convertReqValAddToXml(iso);
    }

    public String convertReqValAddToXml(ISOMsg iso) {
        try {
            String msgId = "MSG" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            String txnId = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            String acNum = iso.getString(102);
            String ifsc = iso.getString(33);

            return """
                <ns2:ReqValAdd xmlns:ns2="%s">
                    <Head ver="%s" ts="%s" orgId="SWITCH" msgId="%s" prodType="%s"/>
                    <Txn id="%s" note="Name Enquiry" type="NameEnq" ts="%s"/>
                    <Payer addr="switch@bank" name="SWITCH" seqNum="1" type="ENTITY" code="0000"/>
                    <Payee seqNum="1" type="PERSON" code="0000">
                        <Ac addrType="ACCOUNT">
                            <Detail name="IFSC" value="%s"/>
                            <Detail name="ACNUM" value="%s"/>
                            <Detail name="ACTYPE" value="DEFAULT"/>
                        </Ac>
                    </Payee>
                </ns2:ReqValAdd>
                """.formatted(
                    NAMESPACE, API_VERSION, OffsetDateTime.now(), msgId, PROD_TYPE,
                    txnId, OffsetDateTime.now(),
                    ifsc != null ? ifsc : "",
                    acNum != null ? acNum : ""
                );
        } catch (Exception e) {
            throw new RuntimeException("ISO to ReqValAdd XML conversion failed", e);
        }
    }

    /* ===============================
       RESPVALADD - ISO 0210 to XML
       =============================== */
    public String convertRespValAddToXml(byte[] isoBytes) {
        ISOMsg iso = unpackIso(isoBytes);
        return convertRespValAddToXml(iso);
    }

    public String convertRespValAddToXml(ISOMsg iso) {
        try {
            String msgId = "MSG" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            String responseCode = iso.getString(39);
            String result = "00".equals(responseCode) ? "SUCCESS" : "FAILURE";
            String acNum = iso.getString(102);
            String ifsc = iso.getString(33);
            String approvalNum = iso.getString(38);

            return """
                <ns2:RespValAdd xmlns:ns2="%s">
                    <Head ver="%s" ts="%s" orgId="SWITCH" msgId="%s" prodType="%s"/>
                    <Txn id="%s" note="Name Enquiry Response" type="NameEnq" ts="%s"/>
                    <Resp reqMsgId="%s" result="%s"
                          IFSC="%s" acNum="%s" accType="DEFAULT"
                          approvalNum="%s" code="0000" type="PERSON"/>
                </ns2:RespValAdd>
                """.formatted(
                    NAMESPACE, API_VERSION, OffsetDateTime.now(), msgId, PROD_TYPE,
                    iso.getString(37), OffsetDateTime.now(),
                    iso.getString(11), result,
                    ifsc != null ? ifsc : "",
                    acNum != null ? acNum : "",
                    approvalNum != null ? approvalNum : "000000"
                );
        } catch (Exception e) {
            throw new RuntimeException("ISO to RespValAdd XML conversion failed", e);
        }
    }

    /* ===============================
       RESPLISTACCPVD - ISO 0210 to XML
       (List Account Provider doesn't typically use ISO)
       =============================== */
    public String convertRespListAccPvdToXml(byte[] isoBytes) {
        ISOMsg iso = unpackIso(isoBytes);
        return convertRespListAccPvdToXml(iso);
    }

    public String convertRespListAccPvdToXml(ISOMsg iso) {
        String msgId = "MSG" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        return """
            <ns2:RespListAccPvd xmlns:ns2="%s">
                <Head ver="%s" ts="%s" orgId="SWITCH" msgId="%s" prodType="%s"/>
                <Txn type="ListAccPvd"/>
                <Resp reqMsgId="%s" result="SUCCESS"/>
                <AccPvdList/>
            </ns2:RespListAccPvd>
            """.formatted(
                NAMESPACE, API_VERSION, OffsetDateTime.now(), msgId, PROD_TYPE,
                iso.getString(11)
            );
    }

    /* ===============================
       HELPER METHODS
       =============================== */
    private ISOMsg unpackIso(byte[] isoBytes) {
        return IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
    }

    private String paiseToRupees(String paise) {
        if (paise == null || paise.isBlank()) {
            return "0.00";
        }
        try {
            long paiseValue = Long.parseLong(paise.trim());
            double rupees = paiseValue / 100.0;
            return String.format("%.2f", rupees);
        } catch (NumberFormatException e) {
            return "0.00";
        }
    }
}
