package com.hitachi.mockswitch.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hitachi.mockswitch.entity.AccountMaster;
import com.hitachi.mockswitch.repository.AccountMasterRepository;

/**
 * Debit/Credit logic for Mock Switch.
 * Updates account_master.available_balance and last_txn_rrn/last_updated_time
 * when processing successful ReqPay (fund transfer).
 */
@Service
public class AccountLedgerService {

    private static final String ACTIVE = "ACTIVE";
    private static final String IMPS_Y = "Y";

    /** Response code: success */
    public static final String RC_SUCCESS = "00";
    /** Response code: insufficient funds */
    public static final String RC_INSUFFICIENT_FUNDS = "51";
    /** Response code: invalid account / not found */
    public static final String RC_INVALID_ACCOUNT = "14";
    /** Response code: system/ledger error */
    public static final String RC_SYSTEM_ERROR = "96";

    @Autowired
    private AccountMasterRepository accountMasterRepository;

    /**
     * Perform debit (payer) and credit (payee) and update account_master.
     * ReqPay ISO: DE4=amount (paise), DE32=payer IFSC first 4, DE33=payee IFSC, DE102=payer ac, DE103=payee ac, DE37=RRN.
     *
     * @param reqIso ReqPay ISO 0200 message
     * @return response code "00" on success, "51" insufficient funds, "14" account invalid, "96" error
     */
    @Transactional(rollbackFor = Exception.class)
    public String debitAndCredit(ISOMsg reqIso) {
        try {
            String amountPaise = reqIso.hasField(4) ? reqIso.getString(4) : null;
            String payerIfscPrefix = reqIso.hasField(32) ? reqIso.getString(32) : null;
            String payeeIfsc = reqIso.hasField(33) ? reqIso.getString(33) : null;
            String payerAccount = reqIso.hasField(102) ? reqIso.getString(102) : null;
            String payeeAccount = reqIso.hasField(103) ? reqIso.getString(103) : null;
            String rrn = reqIso.hasField(37) ? reqIso.getString(37) : null;

            if (payerAccount == null || payeeAccount == null || amountPaise == null) {
                System.err.println("AccountLedger: missing DE4/DE32/DE33/DE102/DE103");
                return RC_INVALID_ACCOUNT;
            }

            BigDecimal amountRupees = paiseToRupees(amountPaise);
            if (amountRupees.compareTo(BigDecimal.ZERO) <= 0) {
                System.err.println("AccountLedger: invalid amount " + amountPaise);
                return RC_INVALID_ACCOUNT;
            }

            // Resolve payer: DE102 + DE32 (first 4 of IFSC)
            Optional<AccountMaster> payerOpt = findPayerAccount(payerAccount, payerIfscPrefix);
            if (payerOpt.isEmpty()) {
                System.out.println("AccountLedger: payer not found " + payerAccount + " IFSC*" + payerIfscPrefix);
                return RC_INVALID_ACCOUNT;
            }

            // Resolve payee: DE103 + DE33
            Optional<AccountMaster> payeeOpt = accountMasterRepository
                .findByAccountNumberAndIfscCodeAndAccountStatusAndImpsEnabled(
                    payeeAccount, payeeIfsc, ACTIVE, IMPS_Y);
            if (payeeOpt.isEmpty()) {
                System.out.println("AccountLedger: payee not found " + payeeAccount + "@" + payeeIfsc);
                return RC_INVALID_ACCOUNT;
            }

            AccountMaster payer = payerOpt.get();
            AccountMaster payee = payeeOpt.get();

            BigDecimal balance = payer.getAvailableBalance() != null ? payer.getAvailableBalance() : BigDecimal.ZERO;
            if (balance.compareTo(amountRupees) < 0) {
                System.out.println("AccountLedger: insufficient funds payer " + payer.getAccountNumber() + " balance=" + balance + " required=" + amountRupees);
                return RC_INSUFFICIENT_FUNDS;
            }

            LocalDateTime now = LocalDateTime.now();

            // Debit payer
            payer.setAvailableBalance(balance.subtract(amountRupees));
            payer.setLastTxnRrn(rrn);
            payer.setLastUpdatedTime(now);
            accountMasterRepository.save(payer);

            // Credit payee
            BigDecimal payeeBalance = payee.getAvailableBalance() != null ? payee.getAvailableBalance() : BigDecimal.ZERO;
            payee.setAvailableBalance(payeeBalance.add(amountRupees));
            payee.setLastTxnRrn(rrn);
            payee.setLastUpdatedTime(now);
            accountMasterRepository.save(payee);

            System.out.println("AccountLedger: DEBIT " + amountRupees + " from " + payer.getAccountNumber() + "@" + payer.getIfscCode() + " -> CREDIT to " + payee.getAccountNumber() + "@" + payee.getIfscCode() + " RRN=" + rrn);
            return RC_SUCCESS;

        } catch (Exception e) {
            System.err.println("AccountLedger: error " + e.getMessage());
            e.printStackTrace();
            return RC_SYSTEM_ERROR;
        }
    }

    private Optional<AccountMaster> findPayerAccount(String accountNumber, String ifscPrefix) {
        if (accountNumber == null || accountNumber.isBlank()) return Optional.empty();
        List<AccountMaster> list = accountMasterRepository.findByAccountNumberAndAccountStatusAndImpsEnabled(
            accountNumber, ACTIVE, IMPS_Y);
        if (list.isEmpty()) return Optional.empty();
        if (ifscPrefix != null && !ifscPrefix.isBlank()) {
            return list.stream()
                .filter(a -> a.getIfscCode() != null && a.getIfscCode().startsWith(ifscPrefix))
                .findFirst();
        }
        return Optional.of(list.get(0));
    }

    private static BigDecimal paiseToRupees(String amountPaise) {
        if (amountPaise == null || amountPaise.isBlank()) return BigDecimal.ZERO;
        try {
            BigDecimal paise = new BigDecimal(amountPaise.trim());
            return paise.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
