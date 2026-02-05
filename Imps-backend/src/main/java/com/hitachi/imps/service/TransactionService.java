package com.hitachi.imps.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.exception.DuplicateTxnIdException;
import com.hitachi.imps.repository.TransactionRepository;

@Service
public class TransactionService {

    /** switch_status values: INIT → ISO_SENT → SUCCESS | FAILED */
    public static final String STATUS_INIT = "INIT";
    public static final String STATUS_ISO_SENT = "ISO_SENT";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    @Autowired
    private TransactionRepository repo;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private String now() {
        return LocalDateTime.now().format(FMT);
    }

    /**
     * Validate that txn_id does not already exist in public.transaction.
     * @throws DuplicateTxnIdException if txn_id already exists
     */
    public void validateNewTxnId(String txnId) {
        if (txnId != null && !txnId.isBlank() && repo.existsByTxnId(txnId)) {
            throw new DuplicateTxnIdException(txnId);
        }
    }

    public boolean existsByTxnId(String txnId) {
        return txnId != null && !txnId.isBlank() && repo.existsByTxnId(txnId);
    }

    /* ===============================
       1️⃣ Create initial transaction
       =============================== */
    public TransactionEntity createRequest(String txnId, String reqXml) {
        return createRequest(txnId, reqXml, "PAY");
    }

    public TransactionEntity createRequest(String txnId, String reqXml, String txnType) {
        TransactionEntity txn = new TransactionEntity();
        txn.setTxnId(txnId);
        txn.setTxnType(txnType != null ? txnType : "PAY");
        txn.setReqXml(reqXml);
        txn.setReqInDateTime(now());
        txn.setSwitchStatus(STATUS_INIT);
        return repo.save(txn);
    }

    /* ===============================
       2️⃣ ISO sent to switch
       =============================== */
    public void markIsoSent(TransactionEntity txn) {
        txn.setReqOutDateTime(now());
        txn.setSwitchStatus(STATUS_ISO_SENT);
        repo.save(txn);
    }

    /* ===============================
       3️⃣ SUCCESS response
       =============================== */
    public void markSuccess(
            TransactionEntity txn,
            String respXml,
            String approvalNumber,
            String settlementDate) {

        txn.setSwitchStatus(STATUS_SUCCESS);
        txn.setApprovalNumber(approvalNumber);   // DE38
        txn.setRespXml(respXml);
        txn.setRespInDateTime(now());
        txn.setRespOutDateTime(LocalDateTime.now());  // when response is finalized/sent out

        repo.save(txn);   // ✅ FIXED
    }

    /* ===============================
       4️⃣ FAILURE response
       =============================== */
    public void markFailure(
            TransactionEntity txn,
            String respXml) {

        txn.setSwitchStatus(STATUS_FAILED);

        // 🔴 NPCI HARD RULE
        if (txn.getApprovalNumber() == null || txn.getApprovalNumber().isBlank()) {
            txn.setApprovalNumber(generateApproval());
        }


        txn.setRespXml(respXml);
        txn.setRespInDateTime(now());
        txn.setRespOutDateTime(LocalDateTime.now());  // when response is finalized/sent out

        repo.save(txn);
    }


    /* ===============================
       5️⃣ Find transaction by txn_id
       (Returns the latest one if duplicates exist)
       =============================== */
    public TransactionEntity findByTxnId(String txnId) {
        // Use findTopByTxnIdOrderByIdDesc to get the LATEST transaction
        // This handles cases where same txnId exists multiple times (e.g., during testing)
        return repo.findTopByTxnIdOrderByIdDesc(txnId)
                .orElseThrow(() ->
                        new RuntimeException(
                            "Transaction not found: " + txnId));
    }

    /* ===============================
       6️⃣ Save ReqValAdd response
       =============================== */
    public void saveValAddResponse(
            String txnId,
            String respXml,
            String status) {

        TransactionEntity txn =
                repo.findByTxnId(txnId)
                    .orElseThrow(() ->
                        new RuntimeException(
                            "Transaction not found: " + txnId));

        txn.setRespXml(respXml);
        txn.setSwitchStatus(status);
        txn.setRespOutDateTime(LocalDateTime.now());

        repo.save(txn);
    }
    
    /* ===============================
    6️⃣ findLatestByTxnId
    =============================== */
    
    public TransactionEntity findLatestByTxnId(String txnId) {
        return repo.findTopByTxnIdOrderByIdDesc(txnId)
            .orElseThrow(() ->
                new RuntimeException("Transaction not found: " + txnId));
    }

    /** Find transaction by txn_id if exists (for updating response status). */
    public Optional<TransactionEntity> findOptionalByTxnId(String txnId) {
        return repo.findTopByTxnIdOrderByIdDesc(txnId);
    }

    /**
     * Fallback for VALADD: find ISO_SENT row whose req_xml contains Txn id (for old rows stored with msgId as txn_id).
     * DE120 in response is Txn @id; older rows have txn_id = msgId, so lookup by DE120 fails.
     * Uses oldest-first (FIFO) so when multiple rows share the same Txn id, the first response updates the first request.
     * Matches id="VALUE", id='VALUE', or id = "VALUE" (optional spaces) so stored XML variations still match.
     */
    public Optional<TransactionEntity> findOptionalValAddIsoSentByTxnIdInReqXml(String txnIdFromResponse) {
        if (txnIdFromResponse == null || txnIdFromResponse.isBlank()) return Optional.empty();
        List<TransactionEntity> list = repo.findByTxnTypeAndSwitchStatusOrderByIdAsc("VALADD", STATUS_ISO_SENT);
        // Exact patterns (fast path)
        String patternDbl = "id=\"" + txnIdFromResponse + "\"";
        String patternSgl = "id='" + txnIdFromResponse + "'";
        // Regex: id, optional spaces, =, optional spaces, quote, value, quote (handles id = "VALUE" etc.)
        Pattern regex = Pattern.compile("id\\s*=\\s*[\"']" + Pattern.quote(txnIdFromResponse) + "[\"']");
        return list.stream()
            .filter(t -> {
                String req = t.getReqXml();
                if (req == null) return false;
                if (req.contains(patternDbl) || req.contains(patternSgl)) return true;
                return regex.matcher(req).find();
            })
            .findFirst();
    }

    /* ===============================
    Approval Number Generator
    =============================== */
    private String generateApproval() {
     return String.valueOf(System.currentTimeMillis()).substring(7);
    }
    

}
