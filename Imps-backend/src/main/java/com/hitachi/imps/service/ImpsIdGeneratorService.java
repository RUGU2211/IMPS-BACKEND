package com.hitachi.imps.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hitachi.imps.entity.InstitutionMaster;
import com.hitachi.imps.repository.InstitutionMasterRepository;
import com.hitachi.imps.spec.NpciReqPayRules;

/**
 * Generates msgId and txn_id per Rule 021 and 022:
 * - Total length 35 characters.
 * - First 3 = Bank Participation Code (BPC) from public.institution_master.
 * - Remaining 32 = unique id (UUID-based alphanumeric).
 *
 * reqMsgId / Txn id format: &lt;BPC 3 chars&gt; + &lt;32 alphanumeric&gt;.
 */
@Service
public class ImpsIdGeneratorService {

    private static final String DEFAULT_BPC = "BAN";

    @Autowired
    private InstitutionMasterRepository institutionMasterRepository;

    @Value("${imps.org-id:BANK01}")
    private String orgId;

    /**
     * Get Bank Participation Code (3 chars) from institution_master.
     * Uses first active institution for request_org_id = imps.org-id, else first active; first 3 of bank_code, padded to 3.
     */
    public String getBankParticipationCode() {
        Optional<InstitutionMaster> byOrg = institutionMasterRepository.findFirstByRequestOrgIdAndActiveTrueOrderByIdAsc(orgId);
        if (byOrg.isPresent() && byOrg.get().getBankCode() != null && !byOrg.get().getBankCode().isBlank()) {
            return normalizeBpc(byOrg.get().getBankCode());
        }
        Optional<InstitutionMaster> first = institutionMasterRepository.findByActiveTrue().stream().findFirst();
        if (first.isPresent() && first.get().getBankCode() != null && !first.get().getBankCode().isBlank()) {
            return normalizeBpc(first.get().getBankCode());
        }
        return DEFAULT_BPC;
    }

    /** First 3 chars of bankCode, padded with '0' to length 3 if needed. Public for use when generating ids per institution. */
    public static String normalizeBpc(String bankCode) {
        if (bankCode == null || bankCode.isBlank()) return DEFAULT_BPC;
        String s = bankCode.trim().toUpperCase();
        if (s.length() >= NpciReqPayRules.BPC_LENGTH) {
            return s.substring(0, NpciReqPayRules.BPC_LENGTH);
        }
        return String.format("%-3s", s).replace(' ', '0');
    }

    /**
     * Generate a 35-char msgId: BPC (3) + 32 alphanumeric (Rule 021). Uses default BPC (from org).
     */
    public String generateMsgId() {
        return getBankParticipationCode() + unique32();
    }

    /**
     * Generate a 35-char msgId for a specific bank: given BPC (3) + 32 alphanumeric.
     */
    public String generateMsgId(String bpc) {
        String prefix = (bpc != null && bpc.length() >= NpciReqPayRules.BPC_LENGTH)
            ? bpc.substring(0, NpciReqPayRules.BPC_LENGTH)
            : normalizeBpc(bpc);
        return prefix + unique32();
    }

    /**
     * Generate a 35-char txn_id: BPC (3) + 32 alphanumeric (Rule 022). Unique per transaction. Uses default BPC.
     */
    public String generateTxnId() {
        return getBankParticipationCode() + unique32();
    }

    /**
     * Generate a 35-char txn_id for a specific bank: given BPC (3) + 32 alphanumeric. Unique per call.
     */
    public String generateTxnId(String bpc) {
        String prefix = (bpc != null && bpc.length() >= NpciReqPayRules.BPC_LENGTH)
            ? bpc.substring(0, NpciReqPayRules.BPC_LENGTH)
            : normalizeBpc(bpc);
        return prefix + unique32();
    }

    /** 32 alphanumeric characters (UUID without dashes). */
    private static String unique32() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, NpciReqPayRules.UUID_PART_LENGTH);
    }

    /**
     * Return true if the given 3-char prefix is a valid BPC in institution_master (first 3 of any active bank_code).
     */
    public boolean isValidBpc(String bpc) {
        if (bpc == null || bpc.length() != NpciReqPayRules.BPC_LENGTH) return false;
        return institutionMasterRepository.existsByBpc(bpc);
    }
}
