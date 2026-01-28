package com.hitachi.mockswitch.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

/**
 * Message Audit Log Entity
 * Uses the same table as IMPS Backend (message_audit_log in imps_db)
 */
@Entity
@Table(name = "message_audit_log")
public class MessageAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "txn_id")
    private String txnId;

    @Column(name = "stage")
    private String stage;

    @Column(name = "raw_message", columnDefinition = "TEXT")
    private String rawMessage;

    @Column(name = "parsed_message", columnDefinition = "TEXT")
    private String parsedMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ===== GETTERS & SETTERS =====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTxnId() { return txnId; }
    public void setTxnId(String txnId) { this.txnId = txnId; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getRawMessage() { return rawMessage; }
    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getParsedMessage() { return parsedMessage; }
    public void setParsedMessage(String parsedMessage) {
        this.parsedMessage = parsedMessage;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
