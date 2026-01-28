package com.hitachi.mockswitch.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Audit Log Entity
 * 
 * Stores all incoming and outgoing messages for audit trail.
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_txn_id", columnList = "transaction_id"),
    @Index(name = "idx_audit_rrn", columnList = "rrn"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_api_type", columnList = "api_type"),
    @Index(name = "idx_audit_direction", columnList = "direction")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "direction", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Direction direction;

    @Column(name = "api_type", nullable = false, length = 50)
    private String apiType;

    @Column(name = "mti", length = 10)
    private String mti;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "rrn", length = 50)
    private String rrn;

    @Column(name = "stan", length = 20)
    private String stan;

    @Column(name = "processing_code", length = 10)
    private String processingCode;

    @Column(name = "amount", length = 20)
    private String amount;

    @Column(name = "currency", length = 5)
    private String currency;

    @Column(name = "response_code", length = 10)
    private String responseCode;

    @Column(name = "approval_number", length = 20)
    private String approvalNumber;

    @Column(name = "payer_account", length = 50)
    private String payerAccount;

    @Column(name = "payee_account", length = 50)
    private String payeeAccount;

    @Column(name = "payer_ifsc", length = 20)
    private String payerIfsc;

    @Column(name = "payee_ifsc", length = 20)
    private String payeeIfsc;

    @Column(name = "source_endpoint", length = 255)
    private String sourceEndpoint;

    @Column(name = "destination_endpoint", length = 255)
    private String destinationEndpoint;

    @Column(name = "raw_iso_hex", columnDefinition = "TEXT")
    private String rawIsoHex;

    @Column(name = "parsed_iso_json", columnDefinition = "TEXT")
    private String parsedIsoJson;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ===============================
    // CONSTRUCTORS
    // ===============================
    
    public AuditLog() {
    }

    private AuditLog(Builder builder) {
        this.id = builder.id;
        this.direction = builder.direction;
        this.apiType = builder.apiType;
        this.mti = builder.mti;
        this.transactionId = builder.transactionId;
        this.rrn = builder.rrn;
        this.stan = builder.stan;
        this.processingCode = builder.processingCode;
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.responseCode = builder.responseCode;
        this.approvalNumber = builder.approvalNumber;
        this.payerAccount = builder.payerAccount;
        this.payeeAccount = builder.payeeAccount;
        this.payerIfsc = builder.payerIfsc;
        this.payeeIfsc = builder.payeeIfsc;
        this.sourceEndpoint = builder.sourceEndpoint;
        this.destinationEndpoint = builder.destinationEndpoint;
        this.rawIsoHex = builder.rawIsoHex;
        this.parsedIsoJson = builder.parsedIsoJson;
        this.httpStatus = builder.httpStatus;
        this.status = builder.status;
        this.errorMessage = builder.errorMessage;
        this.processingTimeMs = builder.processingTimeMs;
        this.timestamp = builder.timestamp;
        this.notes = builder.notes;
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // ===============================
    // BUILDER
    // ===============================
    
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Direction direction;
        private String apiType;
        private String mti;
        private String transactionId;
        private String rrn;
        private String stan;
        private String processingCode;
        private String amount;
        private String currency;
        private String responseCode;
        private String approvalNumber;
        private String payerAccount;
        private String payeeAccount;
        private String payerIfsc;
        private String payeeIfsc;
        private String sourceEndpoint;
        private String destinationEndpoint;
        private String rawIsoHex;
        private String parsedIsoJson;
        private Integer httpStatus;
        private ProcessingStatus status;
        private String errorMessage;
        private Long processingTimeMs;
        private LocalDateTime timestamp;
        private String notes;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder direction(Direction direction) { this.direction = direction; return this; }
        public Builder apiType(String apiType) { this.apiType = apiType; return this; }
        public Builder mti(String mti) { this.mti = mti; return this; }
        public Builder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public Builder rrn(String rrn) { this.rrn = rrn; return this; }
        public Builder stan(String stan) { this.stan = stan; return this; }
        public Builder processingCode(String processingCode) { this.processingCode = processingCode; return this; }
        public Builder amount(String amount) { this.amount = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder responseCode(String responseCode) { this.responseCode = responseCode; return this; }
        public Builder approvalNumber(String approvalNumber) { this.approvalNumber = approvalNumber; return this; }
        public Builder payerAccount(String payerAccount) { this.payerAccount = payerAccount; return this; }
        public Builder payeeAccount(String payeeAccount) { this.payeeAccount = payeeAccount; return this; }
        public Builder payerIfsc(String payerIfsc) { this.payerIfsc = payerIfsc; return this; }
        public Builder payeeIfsc(String payeeIfsc) { this.payeeIfsc = payeeIfsc; return this; }
        public Builder sourceEndpoint(String sourceEndpoint) { this.sourceEndpoint = sourceEndpoint; return this; }
        public Builder destinationEndpoint(String destinationEndpoint) { this.destinationEndpoint = destinationEndpoint; return this; }
        public Builder rawIsoHex(String rawIsoHex) { this.rawIsoHex = rawIsoHex; return this; }
        public Builder parsedIsoJson(String parsedIsoJson) { this.parsedIsoJson = parsedIsoJson; return this; }
        public Builder httpStatus(Integer httpStatus) { this.httpStatus = httpStatus; return this; }
        public Builder status(ProcessingStatus status) { this.status = status; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder processingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; return this; }
        public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }

        public AuditLog build() {
            return new AuditLog(this);
        }
    }

    // ===============================
    // GETTERS AND SETTERS
    // ===============================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }

    public String getApiType() { return apiType; }
    public void setApiType(String apiType) { this.apiType = apiType; }

    public String getMti() { return mti; }
    public void setMti(String mti) { this.mti = mti; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getRrn() { return rrn; }
    public void setRrn(String rrn) { this.rrn = rrn; }

    public String getStan() { return stan; }
    public void setStan(String stan) { this.stan = stan; }

    public String getProcessingCode() { return processingCode; }
    public void setProcessingCode(String processingCode) { this.processingCode = processingCode; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

    public String getApprovalNumber() { return approvalNumber; }
    public void setApprovalNumber(String approvalNumber) { this.approvalNumber = approvalNumber; }

    public String getPayerAccount() { return payerAccount; }
    public void setPayerAccount(String payerAccount) { this.payerAccount = payerAccount; }

    public String getPayeeAccount() { return payeeAccount; }
    public void setPayeeAccount(String payeeAccount) { this.payeeAccount = payeeAccount; }

    public String getPayerIfsc() { return payerIfsc; }
    public void setPayerIfsc(String payerIfsc) { this.payerIfsc = payerIfsc; }

    public String getPayeeIfsc() { return payeeIfsc; }
    public void setPayeeIfsc(String payeeIfsc) { this.payeeIfsc = payeeIfsc; }

    public String getSourceEndpoint() { return sourceEndpoint; }
    public void setSourceEndpoint(String sourceEndpoint) { this.sourceEndpoint = sourceEndpoint; }

    public String getDestinationEndpoint() { return destinationEndpoint; }
    public void setDestinationEndpoint(String destinationEndpoint) { this.destinationEndpoint = destinationEndpoint; }

    public String getRawIsoHex() { return rawIsoHex; }
    public void setRawIsoHex(String rawIsoHex) { this.rawIsoHex = rawIsoHex; }

    public String getParsedIsoJson() { return parsedIsoJson; }
    public void setParsedIsoJson(String parsedIsoJson) { this.parsedIsoJson = parsedIsoJson; }

    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }

    public ProcessingStatus getStatus() { return status; }
    public void setStatus(ProcessingStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // ===============================
    // ENUMS
    // ===============================

    public enum Direction {
        INBOUND,   // Request received
        OUTBOUND   // Response/Request sent
    }

    public enum ProcessingStatus {
        RECEIVED,      // Message received
        PROCESSING,    // Currently processing
        SUCCESS,       // Processed successfully
        FAILED,        // Processing failed
        SENT,          // Response sent
        SEND_FAILED    // Failed to send response
    }
}
