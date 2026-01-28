package com.hitachi.mockswitch.repository;

import com.hitachi.mockswitch.entity.AuditLog;
import com.hitachi.mockswitch.entity.AuditLog.Direction;
import com.hitachi.mockswitch.entity.AuditLog.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Audit Log Repository
 * 
 * Provides database operations for audit logs.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Find by Transaction ID
    List<AuditLog> findByTransactionIdOrderByTimestampDesc(String transactionId);

    // Find by RRN
    List<AuditLog> findByRrnOrderByTimestampDesc(String rrn);

    // Find by API Type
    List<AuditLog> findByApiTypeOrderByTimestampDesc(String apiType);

    // Find by Direction
    List<AuditLog> findByDirectionOrderByTimestampDesc(Direction direction);

    // Find by Status
    List<AuditLog> findByStatusOrderByTimestampDesc(ProcessingStatus status);

    // Find by timestamp range
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    // Find by API Type and Direction
    List<AuditLog> findByApiTypeAndDirectionOrderByTimestampDesc(
            String apiType, Direction direction);

    // Find failed transactions
    List<AuditLog> findByStatusInOrderByTimestampDesc(List<ProcessingStatus> statuses);

    // Count by API Type
    long countByApiType(String apiType);

    // Count by Direction
    long countByDirection(Direction direction);

    // Count by Status
    long countByStatus(ProcessingStatus status);

    // Find recent logs with pagination
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    // Custom query: Find by multiple criteria
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:apiType IS NULL OR a.apiType = :apiType) AND " +
           "(:direction IS NULL OR a.direction = :direction) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:startTime IS NULL OR a.timestamp >= :startTime) AND " +
           "(:endTime IS NULL OR a.timestamp <= :endTime) " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findByFilters(
            @Param("apiType") String apiType,
            @Param("direction") Direction direction,
            @Param("status") ProcessingStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // Stats query: Count by API Type
    @Query("SELECT a.apiType, COUNT(a) FROM AuditLog a GROUP BY a.apiType ORDER BY COUNT(a) DESC")
    List<Object[]> getCountByApiType();

    // Stats query: Count by Response Code
    @Query("SELECT a.responseCode, COUNT(a) FROM AuditLog a WHERE a.responseCode IS NOT NULL GROUP BY a.responseCode ORDER BY COUNT(a) DESC")
    List<Object[]> getCountByResponseCode();

    // Stats query: Average processing time by API Type
    @Query("SELECT a.apiType, AVG(a.processingTimeMs) FROM AuditLog a WHERE a.processingTimeMs IS NOT NULL GROUP BY a.apiType")
    List<Object[]> getAvgProcessingTimeByApiType();

    // Delete old logs (for cleanup)
    void deleteByTimestampBefore(LocalDateTime timestamp);
}
