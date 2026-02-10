package com.hitachi.imps.client.switchclient;

import org.jpos.iso.ISOMsg;

/**
 * Sends ISO messages to Switch (socket or REST).
 */
public interface ISwitchClient {

    byte[] sendReqPay(ISOMsg iso, String txnId);
    byte[] sendReqPay(ISOMsg iso, String txnId, String requestOrgId);
    byte[] sendReqPay(byte[] isoBytes, String txnId);
    byte[] sendReqPay(byte[] isoBytes, String txnId, String requestOrgId);

    byte[] sendReqChkTxn(ISOMsg iso, String txnId);
    byte[] sendReqChkTxn(ISOMsg iso, String txnId, String requestOrgId);
    byte[] sendReqChkTxn(byte[] isoBytes, String txnId);
    byte[] sendReqChkTxn(byte[] isoBytes, String txnId, String requestOrgId);

    byte[] sendReqHbt(ISOMsg iso, String txnId);
    byte[] sendReqHbt(ISOMsg iso, String txnId, String requestOrgId);
    byte[] sendReqHbt(byte[] isoBytes, String txnId);
    byte[] sendReqHbt(byte[] isoBytes, String txnId, String requestOrgId);

    byte[] sendReqListAccPvd(ISOMsg iso, String txnId);
    byte[] sendReqListAccPvd(ISOMsg iso, String txnId, String requestOrgId);
    byte[] sendReqListAccPvd(byte[] isoBytes, String txnId);
    byte[] sendReqListAccPvd(byte[] isoBytes, String txnId, String requestOrgId);

    byte[] sendReqValAdd(ISOMsg iso, String txnId);
    byte[] sendReqValAdd(ISOMsg iso, String txnId, String requestOrgId);
    byte[] sendReqValAdd(byte[] isoBytes, String txnId);
    byte[] sendReqValAdd(byte[] isoBytes, String txnId, String requestOrgId);

    byte[] sendRespPay(ISOMsg iso, String txnId);
    byte[] sendRespChkTxn(ISOMsg iso, String txnId);
    byte[] sendRespHbt(ISOMsg iso, String txnId);
    byte[] sendRespValAdd(ISOMsg iso, String txnId);
}
