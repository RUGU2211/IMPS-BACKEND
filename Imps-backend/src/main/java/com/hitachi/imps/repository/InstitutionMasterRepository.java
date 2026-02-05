package com.hitachi.imps.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hitachi.imps.entity.InstitutionMaster;

@Repository
public interface InstitutionMasterRepository extends JpaRepository<InstitutionMaster, Integer> {

	Optional<InstitutionMaster> findByBankCode(String bankCode);

	Optional<InstitutionMaster> findByIfscCode(String ifscCode);

	List<InstitutionMaster> findByActiveTrue();

	/** First active institution for given org (e.g. BANK01) for BPC / bank participation code. */
	Optional<InstitutionMaster> findFirstByRequestOrgIdAndActiveTrueOrderByIdAsc(String requestOrgId);

	/** Rule 021/022: true if the 3-char BPC exists as first 3 chars of bank_code in any active institution. */
	@Query(value = "SELECT EXISTS(SELECT 1 FROM institution_master WHERE active = true AND LENGTH(bank_code) >= 3 AND LEFT(bank_code, 3) = :bpc)", nativeQuery = true)
	boolean existsByBpc(@Param("bpc") String bpc);
}
