package com.hitachi.imps.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hitachi.imps.entity.InstitutionMaster;

@Repository
public interface InstitutionMasterRepository extends JpaRepository<InstitutionMaster, Integer> {
	
	Optional<InstitutionMaster> findByBankCode(String bankCode);
	
	Optional<InstitutionMaster> findByIfscCode(String ifscCode);

	List<InstitutionMaster> findByActiveTrue();

}
