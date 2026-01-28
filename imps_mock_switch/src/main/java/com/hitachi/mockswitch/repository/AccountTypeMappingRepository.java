package com.hitachi.mockswitch.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hitachi.mockswitch.entity.AccountTypeMapping;

@Repository
public interface AccountTypeMappingRepository extends JpaRepository<AccountTypeMapping, Integer> {
    
    Optional<AccountTypeMapping> findByAccType(String accType);
    
    Optional<AccountTypeMapping> findByAccTypeIsoCode(String accTypeIsoCode);
}
