package com.hitachi.mockswitch.service;

import com.hitachi.mockswitch.entity.AuditLog;
import com.hitachi.mockswitch.entity.AuditLog.Direction;
import com.hitachi.mockswitch.entity.AuditLog.ProcessingStatus;
import com.hitachi.mockswitch.repository.AuditLogRepository;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Audit Service
 * 
 * Handles all audit logging operations for the Mock Switch.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log an inbound ISO request
     */
    @Transactional
    public AuditLog logInboundRequest(byte[] isoBytes, ISOMsg isoMsg, String apiType, String sourceEndpoint) {
        long startTime = System.currentTimeMillis();
        
        AuditLog auditLog = AuditLog.builder()
                .direction(Direction.INBOUND)
                .apiType(apiType)
                .sourceEndpoint(sourceEndpoint != null ? sourceEndpoint : "unknown")
                .destinationEndpoint("Mock Switch")
                .rawIsoHex(bytesToHex(isoBytes))
                .timestamp(LocalDateTime.now())
                .status(ProcessingStatus.RECEIVED)
                .httpStatus(200)
                .notes("Request received")
                .build();

        // Extract ISO fields
        if (isoMsg != null) {
            try {
                auditLog.setMti(isoMsg.getMTI());
                auditLog.setStan(isoMsg.hasField(11) ? isoMsg.getString(11) : null);
                auditLog.setProcessingCode(isoMsg.hasField(3) ? isoMsg.getString(3) : null);
                auditLog.setAmount(isoMsg.hasField(4) ? isoMsg.getString(4) : null);
                auditLog.setRrn(isoMsg.hasField(37) ? isoMsg.getString(37) : null);
                auditLog.setCurrency(isoMsg.hasField(49) ? isoMsg.getString(49) : null);
                auditLog.setPayerAccount(isoMsg.hasField(102) ? isoMsg.getString(102) : null);
                auditLog.setPayeeAccount(isoMsg.hasField(103) ? isoMsg.getString(103) : null);
                auditLog.setPayerIfsc(isoMsg.hasField(32) ? isoMsg.getString(32) : null);
                auditLog.setPayeeIfsc(isoMsg.hasField(33) ? isoMsg.getString(33) : null);
                auditLog.setTransactionId(isoMsg.hasField(120) ? isoMsg.getString(120) : null);
                auditLog.setParsedIsoJson(isoMsgToJson(isoMsg));
            } catch (ISOException e) {
                log.error("Error extracting ISO fields: {}", e.getMessage());
                auditLog.setErrorMessage("ISO parsing error: " + e.getMessage());
            }
        }

        auditLog.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        AuditLog saved = auditLogRepository.save(auditLog);
        // message_audit_log is written only by Imps-backend (4 stages per flow)
        return saved;
    }

    /**
     * Log an outbound ISO response
     */
    @Transactional
    public AuditLog logOutboundResponse(byte[] isoBytes, ISOMsg isoMsg, String apiType, 
                                         String destinationEndpoint, Integer httpStatus, 
                                         ProcessingStatus status, String errorMessage) {
        long startTime = System.currentTimeMillis();
        
        AuditLog auditLog = AuditLog.builder()
                .direction(Direction.OUTBOUND)
                .apiType(apiType)
                .sourceEndpoint("Mock Switch")
                .destinationEndpoint(destinationEndpoint != null ? destinationEndpoint : "unknown")
                .rawIsoHex(isoBytes != null ? bytesToHex(isoBytes) : null)
                .httpStatus(httpStatus)
                .timestamp(LocalDateTime.now())
                .status(status)
                .errorMessage(errorMessage)
                .notes(status == ProcessingStatus.SENT ? "Response sent to IMPS Backend" : (errorMessage != null ? "Send failed" : null))
                .build();

        // Extract ISO fields (including payer_ifsc DE32, payee_ifsc DE33 for outbound)
        if (isoMsg != null) {
            try {
                auditLog.setMti(isoMsg.getMTI());
                auditLog.setStan(isoMsg.hasField(11) ? isoMsg.getString(11) : null);
                auditLog.setProcessingCode(isoMsg.hasField(3) ? isoMsg.getString(3) : null);
                auditLog.setAmount(isoMsg.hasField(4) ? isoMsg.getString(4) : null);
                auditLog.setRrn(isoMsg.hasField(37) ? isoMsg.getString(37) : null);
                auditLog.setResponseCode(isoMsg.hasField(39) ? isoMsg.getString(39) : null);
                auditLog.setApprovalNumber(isoMsg.hasField(38) ? isoMsg.getString(38) : null);
                auditLog.setCurrency(isoMsg.hasField(49) ? isoMsg.getString(49) : null);
                auditLog.setPayerAccount(isoMsg.hasField(102) ? isoMsg.getString(102) : null);
                auditLog.setPayeeAccount(isoMsg.hasField(103) ? isoMsg.getString(103) : null);
                auditLog.setPayerIfsc(isoMsg.hasField(32) ? isoMsg.getString(32) : null);
                auditLog.setPayeeIfsc(isoMsg.hasField(33) ? isoMsg.getString(33) : null);
                auditLog.setTransactionId(isoMsg.hasField(120) ? isoMsg.getString(120) : null);
                auditLog.setParsedIsoJson(isoMsgToJson(isoMsg));
            } catch (ISOException e) {
                log.error("Error extracting ISO fields: {}", e.getMessage());
            }
        }

        auditLog.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        return auditLogRepository.save(auditLog);
    }

    /**
     * Update audit log status
     */
    @Transactional
    public void updateStatus(Long auditLogId, ProcessingStatus status, String errorMessage) {
        auditLogRepository.findById(auditLogId).ifPresent(auditLog -> {
            auditLog.setStatus(status);
            if (errorMessage != null) {
                auditLog.setErrorMessage(errorMessage);
            }
            auditLogRepository.save(auditLog);
        });
    }

    /**
     * Update audit log with processing time
     */
    @Transactional
    public void updateProcessingTime(Long auditLogId, long processingTimeMs) {
        auditLogRepository.findById(auditLogId).ifPresent(auditLog -> {
            auditLog.setProcessingTimeMs(processingTimeMs);
            auditLogRepository.save(auditLog);
        });
    }

    /**
     * Get all audit logs (recent first)
     */
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }

    /**
     * Get audit logs by transaction ID
     */
    public List<AuditLog> getLogsByTransactionId(String transactionId) {
        return auditLogRepository.findByTransactionIdOrderByTimestampDesc(transactionId);
    }

    /**
     * Get audit logs by RRN
     */
    public List<AuditLog> getLogsByRrn(String rrn) {
        return auditLogRepository.findByRrnOrderByTimestampDesc(rrn);
    }

    /**
     * Get audit logs by API type
     */
    public List<AuditLog> getLogsByApiType(String apiType) {
        return auditLogRepository.findByApiTypeOrderByTimestampDesc(apiType);
    }

    /**
     * Get audit logs by date range
     */
    public List<AuditLog> getLogsByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(startTime, endTime);
    }

    /**
     * Get failed transactions
     */
    public List<AuditLog> getFailedTransactions() {
        return auditLogRepository.findByStatusInOrderByTimestampDesc(
                List.of(ProcessingStatus.FAILED, ProcessingStatus.SEND_FAILED));
    }

    /**
     * Get statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalLogs", auditLogRepository.count());
        stats.put("inboundCount", auditLogRepository.countByDirection(Direction.INBOUND));
        stats.put("outboundCount", auditLogRepository.countByDirection(Direction.OUTBOUND));
        stats.put("successCount", auditLogRepository.countByStatus(ProcessingStatus.SUCCESS));
        stats.put("failedCount", auditLogRepository.countByStatus(ProcessingStatus.FAILED));
        stats.put("countByApiType", auditLogRepository.getCountByApiType());
        stats.put("countByResponseCode", auditLogRepository.getCountByResponseCode());
        stats.put("avgProcessingTime", auditLogRepository.getAvgProcessingTimeByApiType());
        
        return stats;
    }

    /**
     * Clean up old logs (older than specified days)
     */
    @Transactional
    @Async
    public void cleanupOldLogs(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        auditLogRepository.deleteByTimestampBefore(cutoffDate);
        log.info("Cleaned up audit logs older than {} days", daysToKeep);
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private String isoMsgToJson(ISOMsg iso) {
        if (iso == null) return null;
        
        StringBuilder json = new StringBuilder("{");
        try {
            json.append("\"mti\":\"").append(iso.getMTI()).append("\"");
            
            for (int i = 2; i <= 128; i++) {
                if (iso.hasField(i)) {
                    json.append(",\"DE").append(i).append("\":\"")
                        .append(escapeJson(iso.getString(i)))
                        .append("\"");
                }
            }
        } catch (ISOException e) {
            log.error("Error converting ISO to JSON: {}", e.getMessage());
        }
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
