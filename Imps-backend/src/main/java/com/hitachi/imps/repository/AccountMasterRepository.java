package com.hitachi.imps.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hitachi.imps.entity.AccountMaster;

/**
 * Account master: used for Switch/NPCI validation (account-level checks).
 * IMPS uses institution_master for validation.
 */
public interface AccountMasterRepository extends JpaRepository<AccountMaster, Long> {

    Optional<AccountMaster> findByAccountNumberAndIfscCodeAndAccountStatusAndImpsEnabled(
        String accountNumber,
        String ifscCode,
        String accountStatus,
        String impsEnabled
    );
}
