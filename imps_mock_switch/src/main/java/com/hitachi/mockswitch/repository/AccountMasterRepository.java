package com.hitachi.mockswitch.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hitachi.mockswitch.entity.AccountMaster;

@Repository
public interface AccountMasterRepository extends JpaRepository<AccountMaster, Integer> {
    
    Optional<AccountMaster> findByAccountNumberAndIfscCodeAndStatus(
        String accountNumber, String ifscCode, String status
    );
    
    Optional<AccountMaster> findByAccountNumber(String accountNumber);
}
