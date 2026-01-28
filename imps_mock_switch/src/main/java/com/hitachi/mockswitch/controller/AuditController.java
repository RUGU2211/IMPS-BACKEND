package com.hitachi.mockswitch.controller;

import com.hitachi.mockswitch.entity.AuditLog;
import com.hitachi.mockswitch.entity.AuditLog.Direction;
import com.hitachi.mockswitch.entity.AuditLog.ProcessingStatus;
import com.hitachi.mockswitch.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Audit Controller
 * 
 * Provides REST API endpoints to query audit logs.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    @Autowired
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Get all audit logs
     */
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        return ResponseEntity.ok(auditService.getAllLogs());
    }

    /**
     * Get audit log by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> getById(@PathVariable Long id) {
        return auditService.getAllLogs().stream()
                .filter(log -> log.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get audit logs by Transaction ID
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<AuditLog>> getByTransactionId(@PathVariable String transactionId) {
        return ResponseEntity.ok(auditService.getLogsByTransactionId(transactionId));
    }

    /**
     * Get audit logs by RRN
     */
    @GetMapping("/rrn/{rrn}")
    public ResponseEntity<List<AuditLog>> getByRrn(@PathVariable String rrn) {
        return ResponseEntity.ok(auditService.getLogsByRrn(rrn));
    }

    /**
     * Get audit logs by API Type
     */
    @GetMapping("/api-type/{apiType}")
    public ResponseEntity<List<AuditLog>> getByApiType(@PathVariable String apiType) {
        return ResponseEntity.ok(auditService.getLogsByApiType(apiType.toUpperCase()));
    }

    /**
     * Get audit logs by Direction (INBOUND/OUTBOUND)
     */
    @GetMapping("/direction/{direction}")
    public ResponseEntity<List<AuditLog>> getByDirection(@PathVariable String direction) {
        try {
            Direction dir = Direction.valueOf(direction.toUpperCase());
            return ResponseEntity.ok(auditService.getAllLogs().stream()
                    .filter(log -> log.getDirection() == dir)
                    .toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get audit logs by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<AuditLog>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(auditService.getLogsByDateRange(startTime, endTime));
    }

    /**
     * Get failed transactions
     */
    @GetMapping("/failed")
    public ResponseEntity<List<AuditLog>> getFailedTransactions() {
        return ResponseEntity.ok(auditService.getFailedTransactions());
    }

    /**
     * Get audit statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(auditService.getStatistics());
    }

    /**
     * Get inbound requests only
     */
    @GetMapping("/inbound")
    public ResponseEntity<List<AuditLog>> getInboundLogs() {
        return ResponseEntity.ok(auditService.getAllLogs().stream()
                .filter(log -> log.getDirection() == Direction.INBOUND)
                .toList());
    }

    /**
     * Get outbound responses only
     */
    @GetMapping("/outbound")
    public ResponseEntity<List<AuditLog>> getOutboundLogs() {
        return ResponseEntity.ok(auditService.getAllLogs().stream()
                .filter(log -> log.getDirection() == Direction.OUTBOUND)
                .toList());
    }

    /**
     * Get successful transactions
     */
    @GetMapping("/success")
    public ResponseEntity<List<AuditLog>> getSuccessfulTransactions() {
        return ResponseEntity.ok(auditService.getAllLogs().stream()
                .filter(log -> log.getStatus() == ProcessingStatus.SUCCESS || 
                              log.getStatus() == ProcessingStatus.SENT)
                .toList());
    }

    /**
     * Get recent logs (last N entries)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<AuditLog>> getRecentLogs(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(auditService.getAllLogs().stream()
                .limit(limit)
                .toList());
    }

    /**
     * Get logs by response code (e.g., "00" for success)
     */
    @GetMapping("/response-code/{code}")
    public ResponseEntity<List<AuditLog>> getByResponseCode(@PathVariable String code) {
        return ResponseEntity.ok(auditService.getAllLogs().stream()
                .filter(log -> code.equals(log.getResponseCode()))
                .toList());
    }

    /**
     * Clean up old logs (requires days parameter)
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<String> cleanupOldLogs(@RequestParam int daysToKeep) {
        if (daysToKeep < 1) {
            return ResponseEntity.badRequest().body("daysToKeep must be at least 1");
        }
        auditService.cleanupOldLogs(daysToKeep);
        return ResponseEntity.ok("Cleanup initiated for logs older than " + daysToKeep + " days");
    }
}
