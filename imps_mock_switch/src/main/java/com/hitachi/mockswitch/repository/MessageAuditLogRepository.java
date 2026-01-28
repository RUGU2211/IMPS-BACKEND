package com.hitachi.mockswitch.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hitachi.mockswitch.entity.MessageAuditLog;

@Repository
public interface MessageAuditLogRepository extends JpaRepository<MessageAuditLog, Integer> {

    List<MessageAuditLog> findByTxnId(String txnId);

    List<MessageAuditLog> findByTxnIdAndStage(String txnId, String stage);
}
