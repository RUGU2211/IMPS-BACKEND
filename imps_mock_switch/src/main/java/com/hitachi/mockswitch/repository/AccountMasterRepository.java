package com.hitachi.mockswitch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hitachi.mockswitch.entity.AccountMaster;

@Repository
public interface AccountMasterRepository extends JpaRepository<AccountMaster, Long> {

    Optional<AccountMaster> findByAccountNumberAndIfscCodeAndAccountStatusAndImpsEnabled(
        String accountNumber, String ifscCode, String accountStatus, String impsEnabled);

    /** For payer lookup: ReqPay has DE102 (account) and DE32 (payer IFSC first 4 chars). */
    List<AccountMaster> findByAccountNumberAndAccountStatusAndImpsEnabled(
        String accountNumber, String accountStatus, String impsEnabled);
}
