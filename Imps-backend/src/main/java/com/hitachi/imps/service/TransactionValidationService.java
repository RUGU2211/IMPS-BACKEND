package com.hitachi.imps.service;

import java.util.Optional;

import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hitachi.imps.entity.InstitutionMaster;
import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.repository.InstitutionMasterRepository;
import com.hitachi.imps.repository.TransactionRepository;

/**
 * Transaction Validation Service
 * Validates transactions after receiving RespPay from Switch.
 * account_master validation done in Switch only – not here.
 */
@Service
public class TransactionValidationService {

    private static final Logger log = LoggerFactory.getLogger(TransactionValidationService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private InstitutionMasterRepository institutionMasterRepository;

    /**
     * Validate transaction after receiving RespPay from Switch
     * @param iso ISO response message
     * @param txn Transaction entity
     * @return ValidationResult
     */
    public ValidationResult validateTransaction(ISOMsg iso, TransactionEntity txn) {
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        try {
            log.debug("Transaction validation start for txnId={}", txn != null ? txn.getTxnId() : "null");

            // 1. Validate Transaction exists
            if (txn == null) {
                result.setValid(false);
                result.addValidation("TRANSACTION", "NOT_FOUND", "Transaction not found in database");
                log.warn("Transaction validation failed: transaction not found");
                return result;
            }
            log.debug("Transaction found: {}", txn.getTxnId());

            // 2. Validate Response Code
            String responseCode = null;
            try {
                if (iso.hasField(39)) {
                    responseCode = iso.getString(39);
                    if ("00".equals(responseCode)) {
                        log.debug("Response code: SUCCESS (00)");
                        result.addValidation("RESPONSE_CODE", "VALID", "00");
                    } else {
                        log.debug("Response code: FAILED ({})", responseCode);
                        result.addValidation("RESPONSE_CODE", "INVALID", responseCode);
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract response code: {}", e.getMessage());
            }

            // 3. Validate RRN matches
            String rrn = null;
            try {
                if (iso.hasField(37)) {
                    rrn = iso.getString(37);
                    if (txn.getDe37() != null && rrn.equals(txn.getDe37())) {
                        log.debug("RRN matches: {}", rrn);
                        result.addValidation("RRN", "VALID", rrn);
                    } else {
                        log.debug("RRN mismatch: ISO={}, DB={}", rrn, txn.getDe37());
                        result.addValidation("RRN", "MISMATCH", "ISO:" + rrn + " vs DB:" + txn.getDe37());
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract RRN");
            }

            // 4. Validate STAN matches
            String stan = null;
            try {
                if (iso.hasField(11)) {
                    stan = iso.getString(11);
                    if (txn.getDe11() != null && stan.equals(txn.getDe11())) {
                        log.debug("STAN matches: {}", stan);
                        result.addValidation("STAN", "VALID", stan);
                    } else {
                        log.debug("STAN mismatch: ISO={}, DB={}", stan, txn.getDe11());
                        result.addValidation("STAN", "MISMATCH", "ISO:" + stan + " vs DB:" + txn.getDe11());
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract STAN");
            }

            // 5. Payee Account validation done in Switch (account_master) – not here.

            // 6. Validate Institution via institution_master (IMPS validation table)
            String payeeIfsc = null;
            try {
                if (iso.hasField(33)) payeeIfsc = iso.getString(33);
            } catch (Exception e) { /* ignore */ }
            if (payeeIfsc != null) {
                Optional<InstitutionMaster> institutionOpt = institutionMasterRepository
                    .findByIfscCode(payeeIfsc);

                if (institutionOpt.isPresent() && Boolean.TRUE.equals(institutionOpt.get().getActive())) {
                    log.debug("Institution valid: {}", institutionOpt.get().getName());
                    result.addValidation("INSTITUTION", "VALID", institutionOpt.get().getName());
                } else {
                    log.debug("Institution not found or inactive: {}", payeeIfsc);
                    result.addValidation("INSTITUTION", "NOT_FOUND", payeeIfsc);
                }
            }

            log.debug("Transaction validation complete: result={}", result.isValid() ? "VALID" : "INVALID");

        } catch (Exception e) {
            log.error("Validation Error", e);
            result.setValid(false);
            result.addValidation("SYSTEM_ERROR", "ERROR", e.getMessage());
        }

        return result;
    }

    /**
     * Validation Result class
     */
    public static class ValidationResult {
        private boolean valid = true;
        private java.util.Map<String, String> validations = new java.util.HashMap<>();

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public void addValidation(String field, String status, String message) {
            validations.put(field, status + ":" + message);
        }

        public java.util.Map<String, String> getValidations() {
            return validations;
        }
    }
}
