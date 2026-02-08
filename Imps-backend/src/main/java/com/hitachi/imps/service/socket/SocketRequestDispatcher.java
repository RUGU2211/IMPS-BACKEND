package com.hitachi.imps.service.socket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hitachi.imps.service.chktxn.ReqChkTxnService;
import com.hitachi.imps.service.chktxn.RespChkTxnService;
import com.hitachi.imps.service.heartbeat.ReqHbtService;
import com.hitachi.imps.service.listaccpvd.ReqListAccPvdService;
import com.hitachi.imps.service.listaccpvd.RespListAccPvdService;
import com.hitachi.imps.service.pay.ReqPayService;
import com.hitachi.imps.service.pay.RespPayService;
import com.hitachi.imps.service.valadd.ReqValAddService;
import com.hitachi.imps.service.valadd.RespValAddService;

/**
 * Dispatches socket-originated XML to the correct Req* or Resp* service (same as HTTP path).
 * Req*: response delivered via PendingSocketResponseStore when Switch/local handling completes.
 * Resp*: forwarded to Switch; socket returns ACK immediately.
 */
@Component
public class SocketRequestDispatcher {

    @Autowired private ReqPayService reqPayService;
    @Autowired private RespPayService respPayService;
    @Autowired private ReqChkTxnService reqChkTxnService;
    @Autowired private RespChkTxnService respChkTxnService;
    @Autowired private ReqHbtService reqHbtService;
    @Autowired private ReqListAccPvdService reqListAccPvdService;
    @Autowired private RespListAccPvdService respListAccPvdService;
    @Autowired private ReqValAddService reqValAddService;
    @Autowired private RespValAddService respValAddService;

    /** Run the request or response. Req*: response via pendingStore. Resp*: forward to Switch, caller returns ACK. */
    public void dispatch(String xml, String txnId, String msgType) {
        switch (msgType != null ? msgType : "") {
            case "ReqPay":
                reqPayService.processFromNpci(xml, txnId);
                break;
            case "RespPay":
                respPayService.processFromNpci(xml, txnId);
                break;
            case "ReqChkTxn":
                reqChkTxnService.processFromNpci(xml, txnId);
                break;
            case "RespChkTxn":
                respChkTxnService.processFromNpci(xml, txnId);
                break;
            case "ReqHbt":
                reqHbtService.processFromNpci(xml, txnId);
                break;
            case "ReqListAccPvd":
                reqListAccPvdService.processFromNpci(xml, txnId);
                break;
            case "RespListAccPvd":
                respListAccPvdService.processFromNpci(xml, txnId);
                break;
            case "ReqValAdd":
                reqValAddService.processFromNpci(xml, txnId);
                break;
            case "RespValAdd":
                respValAddService.processFromNpci(xml, txnId);
                break;
            default:
                throw new IllegalArgumentException("Unknown message type: " + msgType);
        }
    }
}
