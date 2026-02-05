package com.hitachi.imps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.hitachi.imps.client.NpciMockClient;
import com.hitachi.imps.exception.CommonCodeValidationException;
import com.hitachi.imps.exception.InvalidReqMsgIdException;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.XmlParsingService;
import com.hitachi.imps.service.chktxn.reqchktxn.NpciReqChkTxnService;
import com.hitachi.imps.service.heartbeat.reqhbt.NpciReqHbtService;
import com.hitachi.imps.service.listaccpvd.reqlistaccpvd.NpciReqListAccPvdService;
import com.hitachi.imps.exception.ReqPayValidationException;
import com.hitachi.imps.service.pay.reqpay.NpciReqPayService;
import com.hitachi.imps.service.pay.reqpay.ReqPayValidationService;
import com.hitachi.imps.service.valadd.reqvaladd.NpciReqValAddService;
import com.hitachi.imps.service.validation.CommonCodeValidationService;

/**
 * Dynamic IMPS API – NPCI to IMPS (requests only).
 * URL pattern: http://localhost:8081/imps/{reqpay|reqchktxn|reqvaladd|reqhbt|reqlistaccpvd}/{txn_id}
 * txnId: unique per transaction (validated against public.transaction; duplicate rejected with 409).
 *
 * Flow: NPCI → IMPS (ACK to npci_mock) → process → IMPS → Switch; Switch → IMPS (/switch/resp*) → IMPS → NPCI (/npci/resp*).
 */
@RestController
@RequestMapping("/imps")
public class ImpsController {

    @Autowired private AckService ackService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private NpciMockClient npciMockClient;
    @Autowired private TransactionService transactionService;

    @Autowired private CommonCodeValidationService commonCodeValidationService;
    @Autowired private ReqPayValidationService reqPayValidationService;
    @Autowired private NpciReqPayService reqPayService;
    @Autowired private NpciReqChkTxnService reqChkTxnService;
    @Autowired private NpciReqHbtService reqHbtService;
    @Autowired private NpciReqListAccPvdService reqListAccPvdService;
    @Autowired private NpciReqValAddService reqValAddService;

    private void validateNewTxnId(String txnId) {
        if (txnId == null || txnId.isBlank())
            throw new IllegalArgumentException("txnId must not be blank");
        transactionService.validateNewTxnId(txnId);
    }

    private String ackAndProcessReq(String xml, String txnId, String apiName, String reqType, Runnable process) {
        String reqMsgId = xmlParsingService.extractMsgId(xml);
        if (reqMsgId == null || reqMsgId.isBlank())
            throw new InvalidReqMsgIdException("reqMsgId (Head @msgId) is required for ACK and must not be blank");
        System.out.println("ack of " + reqType + "/" + txnId + " send to npci");
        String ack = ackService.buildAck(apiName, reqMsgId);
        npciMockClient.sendAckToNpciMock(ack);
        process.run();
        return ack;
    }

    /* ========== NPCI → IMPS (dynamic paths) ========== */
    @PostMapping(value = "/reqpay/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqpay(@PathVariable String txnId, @RequestBody String xml) throws ReqPayValidationException {
        System.out.println("imps reqpay/" + txnId + " receive");
        validateNewTxnId(txnId);
        reqPayValidationService.validate(xml);
        return ackAndProcessReq(xml, txnId, "ReqPay", "reqpay", () -> reqPayService.processAsync(xml, txnId));
    }

    @PostMapping(value = "/reqchktxn/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqchktxn(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("imps reqchktxn/" + txnId + " receive");
        validateNewTxnId(txnId);
        commonCodeValidationService.validateCommonHeadTxn(xml);
        return ackAndProcessReq(xml, txnId, "ReqChkTxn", "reqchktxn", () -> reqChkTxnService.processAsync(xml, txnId));
    }

    @PostMapping(value = "/reqhbt/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqhbt(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("imps reqhbt/" + txnId + " receive");
        validateNewTxnId(txnId);
        commonCodeValidationService.validateCommonHeadTxn(xml);
        return ackAndProcessReq(xml, txnId, "ReqHbt", "reqhbt", () -> reqHbtService.processAsync(xml, txnId));
    }

    @PostMapping(value = "/reqlistaccpvd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqlistaccpvd(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("imps reqlistaccpvd/" + txnId + " receive");
        validateNewTxnId(txnId);
        commonCodeValidationService.validateCommonHeadTxn(xml);
        return ackAndProcessReq(xml, txnId, "ReqListAccPvd", "reqlistaccpvd", () -> reqListAccPvdService.processAsync(xml, txnId));
    }

    @PostMapping(value = "/reqvaladd/{txnId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String reqvaladd(@PathVariable String txnId, @RequestBody String xml) throws CommonCodeValidationException {
        System.out.println("imps reqvaladd/" + txnId + " receive");
        validateNewTxnId(txnId);
        commonCodeValidationService.validateCommonHeadTxn(xml);
        return ackAndProcessReq(xml, txnId, "ReqValAdd", "reqvaladd", () -> reqValAddService.processAsync(xml, txnId));
    }
}
