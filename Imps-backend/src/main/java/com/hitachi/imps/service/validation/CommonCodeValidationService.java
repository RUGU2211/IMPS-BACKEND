package com.hitachi.imps.service.validation;

import org.springframework.stereotype.Service;

import com.hitachi.imps.exception.CommonCodeValidationException;
import com.hitachi.imps.service.iso.XmlUtil;
import com.hitachi.imps.spec.NpciReqPayRules;

/**
 * Applies IMPS Common Code Appendix rules that apply to <b>all</b> request types
 * (ReqPay, ReqChkTxn, ReqValAdd, ReqHbt, ReqListAccPvd) when processing txn.
 * Validates Head and Txn blocks: Rule 019, 020, 021, 022.
 *
 * Rule 021/022: msgId and Txn id must be 35 chars (3 BPC + 32 alphanumeric). Format only;
 * we do not require BPC to exist in institution_master so that requests from any bank
 * (including newly added) and test BPCs (e.g. BAN) are accepted. Our system generates
 * ids from institution_master.bank_code when we create messages (e.g. heartbeats).
 */
@Service
public class CommonCodeValidationService {

    /**
     * Validate common Head and Txn rules on any NPCI request XML.
     * Call this for every incoming request before ACK and processing.
     *
     * @param xml Request XML (ReqPay, ReqChkTxn, ReqValAdd, ReqHbt, ReqListAccPvd)
     * @throws CommonCodeValidationException if any rule fails
     */
    public void validateCommonHeadTxn(String xml) throws CommonCodeValidationException {
        CommonCodeValidationException errors = new CommonCodeValidationException();

        // Rule 019 – Head @ver must be 1.0 or 2.0
        String headVer = XmlUtil.read(xml, "//*[local-name()='Head']/@ver");
        if (headVer != null && !headVer.isBlank() && !NpciReqPayRules.isHeadVersionAllowed(headVer)) {
            errors.addError("019_Head_Version", "Head ver must be 1.0 or 2.0; got: " + headVer);
        }

        // Rule 020 – Head @ts ISO format (no AM/PM)
        String headTs = XmlUtil.read(xml, "//*[local-name()='Head']/@ts");
        if (headTs != null && !headTs.isBlank() && !NpciReqPayRules.isHeadTsValid(headTs)) {
            errors.addError("020_Head_ts", "Head ts must be ISO format YYYY-MM-DDTHH:mm:ss.sssZ or with ±hh:mm; got: " + headTs);
        }

        // Rule 021 – Head @msgId 35 chars (3 BPC + 32 UUID). Format only; any BPC accepted (existing or new bank).
        String msgId = XmlUtil.read(xml, "//*[local-name()='Head']/@msgId");
        if (msgId != null && !msgId.isBlank()) {
            if (msgId.length() != NpciReqPayRules.MSG_ID_TOTAL_LENGTH) {
                errors.addError("021_Head_MsgId", "Head msgId must be 35 characters (3 BPC + 32 UUID); got length: " + msgId.length());
            } else if (!NpciReqPayRules.isMsgIdFormatValid(msgId)) {
                errors.addError("021_Head_MsgId", "Head msgId format: first 3 alphanumeric (BPC), remaining 32 alphanumeric (UUID)");
            }
        }

        // Rule 022 – Txn @id 35 chars when present. Format only; any BPC accepted (existing or new bank).
        String txnIdBody = XmlUtil.read(xml, "//*[local-name()='Txn']/@id");
        if (txnIdBody != null && !txnIdBody.isBlank()) {
            if (txnIdBody.length() != NpciReqPayRules.TXN_ID_TOTAL_LENGTH) {
                errors.addError("022_Txn_UUID", "Txn id must be 35 characters (3 BPC + 32 UUID); got length: " + txnIdBody.length());
            } else if (!NpciReqPayRules.isTxnIdFormatValid(txnIdBody)) {
                errors.addError("022_Txn_UUID", "Txn id format: first 3 alphanumeric (BPC), remaining 32 alphanumeric (UUID)");
            }
        }

        if (errors.hasErrors()) {
            throw errors;
        }
    }
}
