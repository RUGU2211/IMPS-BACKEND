package com.hitachi.imps.service;

import java.util.Optional;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hitachi.imps.entity.AccountMaster;
import com.hitachi.imps.entity.InstitutionMaster;
import com.hitachi.imps.entity.TransactionEntity;
import com.hitachi.imps.repository.AccountMasterRepository;
import com.hitachi.imps.repository.InstitutionMasterRepository;
import com.hitachi.imps.repository.TransactionRepository;

/**
 * Transaction Validation Service
 * Validates transactions after receiving RespPay from Switch
 */
@Service
public class TransactionValidationService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountMasterRepository accountMasterRepository;

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
            System.out.println("=== TRANSACTION VALIDATION START ===");

            // 1. Validate Transaction exists
            if (txn == null) {
                result.setValid(false);
                result.addValidation("TRANSACTION", "NOT_FOUND", "Transaction not found in database");
                System.out.println("✗ Transaction not found");
                return result;
            }
            System.out.println("✓ Transaction found: " + txn.getTxnId());

            // 2. Validate Response Code
            String responseCode = null;
            try {
                if (iso.hasField(39)) {
                    responseCode = iso.getString(39);
                    if ("00".equals(responseCode)) {
                        System.out.println("✓ Response Code: SUCCESS (00)");
                        result.addValidation("RESPONSE_CODE", "VALID", "00");
                    } else {
                        System.out.println("✗ Response Code: FAILED (" + responseCode + ")");
                        result.addValidation("RESPONSE_CODE", "INVALID", responseCode);
                        // Don't fail validation, just log it
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠ Could not extract response code");
            }

            // 3. Validate RRN matches
            String rrn = null;
            try {
                if (iso.hasField(37)) {
                    rrn = iso.getString(37);
                    if (txn.getDe37() != null && rrn.equals(txn.getDe37())) {
                        System.out.println("✓ RRN matches: " + rrn);
                        result.addValidation("RRN", "VALID", rrn);
                    } else {
                        System.out.println("⚠ RRN mismatch: ISO=" + rrn + ", DB=" + txn.getDe37());
                        result.addValidation("RRN", "MISMATCH", "ISO:" + rrn + " vs DB:" + txn.getDe37());
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠ Could not extract RRN");
            }

            // 4. Validate STAN matches
            String stan = null;
            try {
                if (iso.hasField(11)) {
                    stan = iso.getString(11);
                    if (txn.getDe11() != null && stan.equals(txn.getDe11())) {
                        System.out.println("✓ STAN matches: " + stan);
                        result.addValidation("STAN", "VALID", stan);
                    } else {
                        System.out.println("⚠ STAN mismatch: ISO=" + stan + ", DB=" + txn.getDe11());
                        result.addValidation("STAN", "MISMATCH", "ISO:" + stan + " vs DB:" + txn.getDe11());
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠ Could not extract STAN");
            }

            // 5. Validate Payee Account (if available in ISO)
            String payeeAccount = null;
            String payeeIfsc = null;
            try {
                if (iso.hasField(103)) {
                    payeeAccount = iso.getString(103);
                }
                if (iso.hasField(33)) {
                    payeeIfsc = iso.getString(33);
                }
                
                if (payeeAccount != null && payeeIfsc != null) {
                    Optional<AccountMaster> accountOpt = accountMasterRepository
                        .findByAccountNumberAndIfscCodeAndStatus(payeeAccount, payeeIfsc, "ACTIVE");
                    
                    if (accountOpt.isPresent()) {
                        System.out.println("✓ Payee Account Valid: " + accountOpt.get().getAccountName());
                        result.addValidation("PAYEE_ACCOUNT", "VALID", accountOpt.get().getAccountName());
                    } else {
                        System.out.println("⚠ Payee Account Not Found: " + payeeAccount + "@" + payeeIfsc);
                        result.addValidation("PAYEE_ACCOUNT", "NOT_FOUND", payeeAccount + "@" + payeeIfsc);
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠ Could not validate payee account: " + e.getMessage());
            }

            // 6. Validate Institution (if available)
            if (payeeIfsc != null) {
                Optional<InstitutionMaster> institutionOpt = institutionMasterRepository
                    .findByIfscCode(payeeIfsc);
                
                if (institutionOpt.isPresent() && institutionOpt.get().getActive()) {
                    System.out.println("✓ Institution Valid: " + institutionOpt.get().getName());
                    result.addValidation("INSTITUTION", "VALID", institutionOpt.get().getName());
                } else {
                    System.out.println("⚠ Institution Not Found or Inactive: " + payeeIfsc);
                    result.addValidation("INSTITUTION", "NOT_FOUND", payeeIfsc);
                }
            }

            System.out.println("=== TRANSACTION VALIDATION COMPLETE ===");
            System.out.println("Overall Result: " + (result.isValid() ? "VALID" : "INVALID"));

        } catch (Exception e) {
            System.err.println("Validation Error: " + e.getMessage());
            e.printStackTrace();
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
