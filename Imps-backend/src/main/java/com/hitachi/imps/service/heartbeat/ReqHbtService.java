package com.hitachi.imps.service.heartbeat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.NpciMockClient;
import com.hitachi.imps.entity.InstitutionMaster;
import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.repository.InstitutionMasterRepository;
import com.hitachi.imps.service.TransactionService;
import com.hitachi.imps.service.audit.MessageAuditService;
import com.hitachi.imps.service.npci.INpciResponseSender;
import com.hitachi.imps.service.socket.PendingSocketResponseStore;
import com.hitachi.imps.service.util.ResponseIdHelper;
import com.hitachi.imps.service.XmlParsingService;
import com.hitachi.imps.util.IsoUtil;
import com.hitachi.imps.converter.IsoToXmlConverter;

/**
 * ReqHbt: NPCI → IMPS (XML) and Switch → IMPS (ISO).
 * Same bank-status logic for both: read institution_master, result=SUCCESS if all active, else FAILURE.
 * Note: "Complete: X UP, Y DOWN. UP: bank1, bank2. DOWN: bank3. (ReqPay/ReqChkTxn/ReqValAdd require switch on port 9084)"
 */
@Service
public class ReqHbtService {

    private static final String STATUS_SUMMARY = "(ReqPay/ReqChkTxn/ReqValAdd require switch on port 9084)";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMdd");

    @Autowired private NpciMockClient npciMockClient;
    @Autowired private MessageAuditService auditService;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private TransactionService transactionService;
    @Autowired private PendingSocketResponseStore pendingSocketStore;
    @Autowired(required = false) private INpciResponseSender npciResponseSender;
    @Autowired private InstitutionMasterRepository institutionRepo;
    @Autowired private IsoToXmlConverter isoToXmlConverter;

    @Async
    public void processAsync(String xml, String pathTxnId) {
        try { processFromNpci(xml, pathTxnId); } catch (Exception e) { System.err.println("ReqHbtService (NPCI) ERROR: " + e.getMessage()); }
    }

    public void processFromNpci(String xml, String pathTxnId) {
        String msgId = xmlParsingService.extractMsgId(xml);
        String txnId = (pathTxnId != null && !pathTxnId.isBlank()) ? pathTxnId : xmlParsingService.extractTxnId(xml);
        if (txnId == null || txnId.isBlank()) txnId = msgId;
        auditService.saveRaw(txnId, "NPCI_REQHBT_XML_IN", xml);
        TransactionEntity txn = transactionService.createRequest(txnId, xml, "HBT");

        BankStatus status = computeBankStatus();
        String result = status.result;
        String note = status.note;

        if ("FAILURE".equals(result)) {
            logDownBanksToConsole(status);
        }

        Map<String, String> hbt = xmlParsingService.parseReqHbt(xml);
        String respMsgId = ResponseIdHelper.responseMsgIdFromRequest(msgId);
        String respXml = String.format(
            "<upi:RespHbt xmlns:upi=\"http://npci.org/upi/schema/\"><Head ver=\"1.0\" ts=\"%s\" orgId=\"BANK01\" msgId=\"%s\"/><Txn id=\"%s\" note=\"%s\" refId=\"%s\" refUrl=\"\" ts=\"%s\" type=\"Hbt\"/><Resp reqMsgId=\"%s\" result=\"%s\"/></upi:RespHbt>",
            OffsetDateTime.now(), respMsgId, txnId != null ? txnId : "", escapeXml(note),
            hbt.get("ref_id") != null ? hbt.get("ref_id") : "", hbt.get("txn_ts") != null ? hbt.get("txn_ts") : OffsetDateTime.now().toString(), msgId, result);
        auditService.saveRaw(txnId, "NPCI_RESPHBT_XML_OUT", respXml);
        if ("FAILURE".equals(result)) {
            transactionService.markFailure(txn, respXml);
        } else {
            transactionService.markSuccess(txn, respXml, null, null);
        }
        if (sendToNpci(txnId, respXml)) return;
        try { npciMockClient.sendRespHbt(respXml); } catch (Exception e) { System.out.println("NPCI Mock not available: " + e.getMessage()); }
    }

    /** Switch → IMPS: receive ReqHbt ISO, return RespHbt ISO with same bank status logic. Logs to transaction and message_audit_log. */
    public byte[] processFromSwitch(byte[] isoBytes, String pathTxnId) {
        String txnId = pathTxnId != null && !pathTxnId.isBlank() ? pathTxnId : "HBT00000000000000000000000000000001";
        ISOMsg reqIso = null;
        try {
            reqIso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
            if (reqIso.hasField(120)) {
                String de120 = reqIso.getString(120);
                if (de120 != null && !de120.isBlank()) txnId = de120;
            }
        } catch (Exception e) { /* use pathTxnId or default */ }

        String reqXmlStored = isoToXml(isoBytes, true);
        auditService.saveRawBytesWithParsed(txnId, "SWITCH_REQHBT_ISO_IN", isoBytes);
        TransactionEntity txn = transactionService.createRequest(txnId, reqXmlStored, "HBT");
        setDeFieldsFromReqIso(txn, reqIso);

        BankStatus status = computeBankStatus();
        String responseCode = "SUCCESS".equals(status.result) ? "00" : "96";
        byte[] respBytes = buildRespHbtIso(isoBytes, responseCode, status.note);

        String respXmlStored = isoToXml(respBytes, false);
        auditService.saveRawBytesWithParsed(txnId, "SWITCH_RESPHBT_ISO_OUT", respBytes);
        if ("FAILURE".equals(status.result)) {
            logDownBanksToConsole(status);
            transactionService.markFailure(txn, respXmlStored);
        } else {
            transactionService.markSuccess(txn, respXmlStored, null, null);
        }
        return respBytes;
    }

    private String isoToXml(byte[] isoBytes, boolean isReq) {
        try {
            return isReq ? isoToXmlConverter.convertReqHbtToXml(isoBytes) : isoToXmlConverter.convertRespHbtToXml(isoBytes);
        } catch (Exception e) {
            return "BASE64:" + java.util.Base64.getEncoder().encodeToString(isoBytes);
        }
    }

    private void setDeFieldsFromReqIso(TransactionEntity txn, ISOMsg reqIso) {
        if (reqIso == null) return;
        try {
            if (reqIso.hasField(11)) txn.setDe11(reqIso.getString(11));
            if (reqIso.hasField(12)) txn.setDe12(reqIso.getString(12));
            if (reqIso.hasField(13)) txn.setDe13(reqIso.getString(13));
            if (reqIso.hasField(37)) txn.setDe37(reqIso.getString(37));
        } catch (Exception e) { /* ignore */ }
    }

    private BankStatus computeBankStatus() {
        List<InstitutionMaster> institutions = institutionRepo.findAll();
        int upCount = 0;
        int downCount = 0;
        List<String> upBanks = new ArrayList<>();
        List<String> downBanks = new ArrayList<>();
        List<InstitutionMaster> downInstitutions = new ArrayList<>();
        if (institutions != null) {
            for (InstitutionMaster inst : institutions) {
                boolean active = Boolean.TRUE.equals(inst.getActive());
                String label = formatBankLabel(inst);
                if (active) {
                    upCount++;
                    upBanks.add(label);
                } else {
                    downCount++;
                    downBanks.add(label);
                    downInstitutions.add(inst);
                }
            }
        }
        String result = (downCount == 0) ? "SUCCESS" : "FAILURE";
        String note = buildBankStatusNote(upCount, downCount, upBanks, downBanks);
        return new BankStatus(result, note, downInstitutions != null ? downInstitutions : List.of());
    }

    /** Log failed switch connection details from institution_master to console. */
    private void logDownBanksToConsole(BankStatus status) {
        if (status.downInstitutions == null || status.downInstitutions.isEmpty()) return;
        for (InstitutionMaster inst : status.downInstitutions) {
            String host = inst.getSwitchIp() != null && !inst.getSwitchIp().isBlank() ? inst.getSwitchIp() : "localhost";
            String port = inst.getSwitchPort() != null && !inst.getSwitchPort().isBlank() ? inst.getSwitchPort() : "9084";
            System.out.println("[IMPS] Switch connection FAILED (institution_master): id=" + inst.getId()
                + " name=\"" + (inst.getName() != null ? inst.getName() : "") + "\""
                + " request_org_id=" + (inst.getRequestOrgId() != null ? inst.getRequestOrgId() : "")
                + " bank_code=" + (inst.getBankCode() != null ? inst.getBankCode() : "")
                + " switch_ip=" + host + " switch_port=" + port
                + " | DB switch_status=FAILED (all switches must be UP for SUCCESS)");
        }
    }

    private byte[] buildRespHbtIso(byte[] reqIsoBytes, String responseCode, String note) {
        try {
            ISOMsg reqIso = IsoUtil.unpack(reqIsoBytes, new ImpsIsoPackager());
            ISOMsg respIso = new ISOMsg();
            respIso.setPackager(new ImpsIsoPackager());
            respIso.setMTI("0810");
            copyField(reqIso, respIso, 3);
            copyField(reqIso, respIso, 24);
            copyField(reqIso, respIso, 37);
            copyField(reqIso, respIso, 41);
            copyField(reqIso, respIso, 120);
            respIso.set(11, String.format("%06d", System.currentTimeMillis() % 1000000));
            respIso.set(12, LocalDateTime.now().format(TIME_FMT));
            respIso.set(13, LocalDateTime.now().format(DATE_FMT));
            respIso.set(39, responseCode);
            if (note != null && !note.isBlank()) respIso.set(48, note);
            return IsoUtil.pack(respIso);
        } catch (Exception e) {
            throw new RuntimeException("buildRespHbtIso failed", e);
        }
    }

    private static void copyField(ISOMsg src, ISOMsg dst, int field) throws ISOException {
        if (src.hasField(field)) dst.set(field, src.getString(field));
    }

    private static record BankStatus(String result, String note, List<InstitutionMaster> downInstitutions) {}

    private static String formatBankLabel(InstitutionMaster inst) {
        String name = inst.getName() != null ? inst.getName() : "";
        String orgId = inst.getRequestOrgId() != null ? inst.getRequestOrgId() : "";
        if (name.isEmpty() && orgId.isEmpty()) return "id=" + inst.getId();
        if (name.isEmpty()) return orgId;
        if (orgId.isEmpty()) return name;
        return name + " (" + orgId + ")";
    }

    private static String buildBankStatusNote(int upCount, int downCount, List<String> upBanks, List<String> downBanks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Complete: ").append(upCount).append(" UP, ").append(downCount).append(" DOWN. ");
        if (!upBanks.isEmpty()) sb.append("UP: ").append(String.join(", ", upBanks)).append(". ");
        if (!downBanks.isEmpty()) sb.append("DOWN: ").append(String.join(", ", downBanks)).append(". ");
        sb.append(STATUS_SUMMARY);
        return sb.toString();
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private boolean sendToNpci(String txnId, String respXml) {
        if (npciResponseSender != null) return npciResponseSender.send(txnId, respXml);
        return pendingSocketStore.completePending(txnId, respXml);
    }
}
