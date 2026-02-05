package com.hitachi.imps.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hitachi.imps.entity.TransactionEntity;

@Repository
public interface TransactionRepository
extends JpaRepository<TransactionEntity, Integer> {

	Optional<TransactionEntity>findFirstByDe11AndDe37AndDe13(
	String de11,
	String de37,
	String de13
	);
	Optional<TransactionEntity> findByTxnId(String txnId);
	Optional<TransactionEntity> findTopByTxnIdOrderByIdDesc(String txnId);
	boolean existsByTxnId(String txnId);
	 Optional<TransactionEntity>findTopByTxnIdOrderByReqInDateTimeDesc(String txnId);

	/** Find VALADD transactions in ISO_SENT (for fallback lookup by Txn id in req_xml). */
	List<TransactionEntity> findByTxnTypeAndSwitchStatusOrderByIdDesc(String txnType, String switchStatus);

	/** Same, oldest first (FIFO) so one response updates the oldest pending request. */
	List<TransactionEntity> findByTxnTypeAndSwitchStatusOrderByIdAsc(String txnType, String switchStatus);
}
