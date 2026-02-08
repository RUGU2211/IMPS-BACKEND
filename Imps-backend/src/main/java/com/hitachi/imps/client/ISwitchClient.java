package com.hitachi.imps.client;

import org.jpos.iso.ISOMsg;

/**
 * Sends ISO messages to Switch (socket or REST).
 * Uses institution_master (lookup by request_org_id) for switch_ip/switch_port when requestOrgId provided.
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
