package com.hitachi.imps.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hitachi.imps.entity.AccountTypeMapping;

public interface AccountTypeMappingRepository
        extends JpaRepository<AccountTypeMapping, Integer> {

    Optional<AccountTypeMapping> findByAccType(String accType);
}
