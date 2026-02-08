package com.hitachi.mockswitch.service;

import java.util.Optional;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hitachi.mockswitch.entity.AccountMaster;
import com.hitachi.mockswitch.repository.AccountMasterRepository;

/**
 * Validation Service for Mock Switch.
 * Uses account_master only. institution_master and account_type_mapping are IMPS-only (no access from Switch).
 */
@Service
public class ValidationService {

    @Autowired
    private AccountMasterRepository accountMasterRepository;

    /**
     * Validate ReqPay transaction
     * @param iso ISO message
     * @return ValidationResult
     */
    public ValidationResult validateReqPay(ISOMsg iso) {
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        try {
            // Extract fields from ISO
            String payerAccount = iso.hasField(102) ? iso.getString(102) : null;
            String payeeAccount = iso.hasField(103) ? iso.getString(103) : null;
            String payeeIfsc = iso.hasField(33) ? iso.getString(33) : null;
            String amount = iso.hasField(4) ? iso.getString(4) : null;

            System.out.println("=== VALIDATION START ===");
            System.out.println("Payer Account: " + payerAccount);
            System.out.println("Payee Account: " + payeeAccount);
            System.out.println("Payee IFSC: " + payeeIfsc);
            System.out.println("Amount: " + amount);

            // 1. Validate Payee Account (Account Master)
            if (payeeAccount != null && payeeIfsc != null) {
                Optional<AccountMaster> payeeAccountOpt = accountMasterRepository
                    .findByAccountNumberAndIfscCodeAndAccountStatusAndImpsEnabled(payeeAccount, payeeIfsc, "ACTIVE", "Y");
                
                if (payeeAccountOpt.isPresent()) {
                    AccountMaster account = payeeAccountOpt.get();
                    System.out.println("✓ Payee Account Valid: " + account.getAccountHolderName());
                    result.addValidation("PAYEE_ACCOUNT", "VALID", account.getAccountHolderName());
                } else {
                    System.out.println("✗ Payee Account Not Found or Inactive");
                    result.setValid(false);
                    result.addValidation("PAYEE_ACCOUNT", "INVALID", "Account not found or inactive");
                }
            }

            // 2. Institution (IFSC) validation is done in Imps-backend only – not here.

            // 3. Validate Amount
            if (amount != null) {
                try {
                    long amountInPaise = Long.parseLong(amount);
                    if (amountInPaise > 0 && amountInPaise <= 20000000) { // Max 2 lakh
                        System.out.println("✓ Amount Valid: " + amountInPaise + " paise");
                        result.addValidation("AMOUNT", "VALID", String.valueOf(amountInPaise));
                    } else {
                        System.out.println("✗ Amount Invalid: Out of range");
                        result.setValid(false);
                        result.addValidation("AMOUNT", "INVALID", "Amount out of valid range");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("✗ Amount Invalid: Not a number");
                    result.setValid(false);
                    result.addValidation("AMOUNT", "INVALID", "Invalid amount format");
                }
            }

            System.out.println("=== VALIDATION COMPLETE ===");
            System.out.println("Overall Result: " + (result.isValid() ? "VALID" : "INVALID"));

        } catch (Exception e) {
            System.err.println("Validation Error: " + e.getMessage());
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
