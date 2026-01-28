package com.hitachi.imps.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hitachi.imps.entity.AccountMaster;


public interface AccountMasterRepository extends JpaRepository<AccountMaster, Integer> {
	
	Optional<AccountMaster> findByAccountNumberAndIfscCodeAndStatus(
	    String accountNumber,
	    String ifscCode,
	    String status
);
}

