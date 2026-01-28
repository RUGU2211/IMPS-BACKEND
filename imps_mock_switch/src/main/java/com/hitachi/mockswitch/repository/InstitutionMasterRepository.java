package com.hitachi.mockswitch.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hitachi.mockswitch.entity.InstitutionMaster;

@Repository
public interface InstitutionMasterRepository extends JpaRepository<InstitutionMaster, Integer> {
    
    Optional<InstitutionMaster> findByIfscCodeAndActive(String ifscCode, Boolean active);
    
    Optional<InstitutionMaster> findByBankCodeAndActive(String bankCode, Boolean active);
}
