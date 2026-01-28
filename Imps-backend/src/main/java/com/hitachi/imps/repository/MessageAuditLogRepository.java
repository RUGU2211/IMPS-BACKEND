package com.hitachi.imps.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hitachi.imps.entity.MessageAuditLog;

public interface MessageAuditLogRepository
        extends JpaRepository<MessageAuditLog, Integer> {

    List<MessageAuditLog> findByTxnId(String txnId);

    List<MessageAuditLog> findByTxnIdAndStage(String txnId, String stage);
}





