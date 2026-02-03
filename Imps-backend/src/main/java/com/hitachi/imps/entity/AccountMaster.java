package com.hitachi.imps.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Account master: shared for Switch and NPCI validation only.
 * IMPS uses institution_master for validation; this table is for account-level checks.
 */
@Entity
@Table(name = "account_master")
public class AccountMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "ifsc_code", nullable = false, length = 11)
    private String ifscCode;

    @Column(name = "account_holder_name", length = 100)
    private String accountHolderName;

    @Column(name = "account_type", length = 10)
    private String accountType;

    @Column(name = "available_balance", precision = 18, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "account_status", nullable = false, length = 10)
    private String accountStatus = "ACTIVE";

    @Column(name = "imps_enabled", nullable = false, length = 1)
    private String impsEnabled = "Y";

    @Column(name = "upi_enabled", nullable = false, length = 1)
    private String upiEnabled = "Y";

    @Column(name = "daily_txn_limit", precision = 18, scale = 2)
    private BigDecimal dailyTxnLimit;

    @Column(name = "last_txn_rrn", length = 20)
    private String lastTxnRrn;

    @Column(name = "last_updated_time")
    private LocalDateTime lastUpdatedTime;

    // ----- Getters & Setters -----
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public String getImpsEnabled() { return impsEnabled; }
    public void setImpsEnabled(String impsEnabled) { this.impsEnabled = impsEnabled; }

    public String getUpiEnabled() { return upiEnabled; }
    public void setUpiEnabled(String upiEnabled) { this.upiEnabled = upiEnabled; }

    public BigDecimal getDailyTxnLimit() { return dailyTxnLimit; }
    public void setDailyTxnLimit(BigDecimal dailyTxnLimit) { this.dailyTxnLimit = dailyTxnLimit; }

    public String getLastTxnRrn() { return lastTxnRrn; }
    public void setLastTxnRrn(String lastTxnRrn) { this.lastTxnRrn = lastTxnRrn; }

    public LocalDateTime getLastUpdatedTime() { return lastUpdatedTime; }
    public void setLastUpdatedTime(LocalDateTime lastUpdatedTime) { this.lastUpdatedTime = lastUpdatedTime; }
}
