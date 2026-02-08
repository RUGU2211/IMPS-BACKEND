package com.hitachi.imps.service.validation;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hitachi.imps.entity.InstitutionMaster;
import com.hitachi.imps.repository.InstitutionMasterRepository;

/**
 * Institution (IFSC) validation via institution_master.
 * IMPS backend only – mock_switch has no access to institution_master.
 * Call for ReqPay, ReqChkTxn, ReqValAdd (from NPCI or Switch) before forwarding.
 */
@Service
public class InstitutionValidationService {

    @Autowired
    private InstitutionMasterRepository institutionMasterRepository;

    /**
     * Validate IFSC against institution_master.
     * @param ifsc IFSC code (11 chars)
     * @return true if IFSC exists and is active
     */
    public boolean isIfscValid(String ifsc) {
        if (ifsc == null || ifsc.isBlank()) return false;
        String trimmed = ifsc.trim().toUpperCase();
        if (trimmed.length() != 11) return false;
        Optional<InstitutionMaster> opt = institutionMasterRepository.findByIfscCode(trimmed);
        return opt.isPresent() && Boolean.TRUE.equals(opt.get().getActive());
    }

    /**
     * Validate payee IFSC. Returns error message if invalid, null if valid.
     */
    public String validatePayeeIfsc(String payeeIfsc) {
        if (payeeIfsc == null || payeeIfsc.isBlank()) return null; // no IFSC to validate
        if (!isIfscValid(payeeIfsc)) {
            return "IFSC not found or inactive in institution_master: " + payeeIfsc;
        }
        return null;
    }
}
