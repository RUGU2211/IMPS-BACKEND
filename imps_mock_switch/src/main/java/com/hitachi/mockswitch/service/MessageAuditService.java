package com.hitachi.mockswitch.service;

import java.time.LocalDateTime;
import java.util.Base64;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hitachi.mockswitch.entity.MessageAuditLog;
import com.hitachi.mockswitch.repository.MessageAuditLogRepository;

/**
 * Message Audit Service
 * Uses message_audit_log table (shared with IMPS Backend in imps_db)
 */
@Service
public class MessageAuditService {

    @Autowired
    private MessageAuditLogRepository repo;

    /* ===============================
       ISO MESSAGE → SAFE STRING
       =============================== */
    private String isoToString(ISOMsg iso) throws ISOException {
        StringBuilder sb = new StringBuilder();
        sb.append("MTI=").append(iso.getMTI()).append("\n");

        for (int i = 1; i <= 128; i++) {
            if (iso.hasField(i)) {
                sb.append("DE").append(i)
                  .append("=")
                  .append(iso.getString(i))
                  .append("\n");
            }
        }
        return sb.toString();
    }

    /* ===============================
       SANITIZE STRING FOR DATABASE
       =============================== */
    private String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("\\x00", "").replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
    }

    /* ===============================
       RAW MESSAGE (ISO STRING)
       =============================== */
    public void saveRaw(String txnId, String stage, String message) {
        MessageAuditLog log = new MessageAuditLog();
        log.setTxnId(txnId);
        log.setStage(stage);
        log.setRawMessage(sanitize(message));
        log.setParsedMessage(null);
        log.setCreatedAt(LocalDateTime.now());

        repo.save(log);
    }

    /* ===============================
       RAW BINARY MESSAGE (ISO BYTES)
       =============================== */
    public void saveRawBytes(String txnId, String stage, byte[] data) {
        MessageAuditLog log = new MessageAuditLog();
        log.setTxnId(txnId);
        log.setStage(stage);
        log.setRawMessage("BASE64:" + Base64.getEncoder().encodeToString(data));
        log.setParsedMessage(null);
        log.setCreatedAt(LocalDateTime.now());

        repo.save(log);
    }

    /* ===============================
       PARSED MESSAGE (ISOMsg)
       =============================== */
    public void saveParsed(String txnId, String stage, ISOMsg iso) {
        try {
            String content = isoToString(iso);

            MessageAuditLog log = new MessageAuditLog();
            log.setTxnId(txnId);
            log.setStage(stage);
            log.setParsedMessage(content);
            log.setCreatedAt(LocalDateTime.now());

            repo.save(log);

        } catch (Exception e) {
            System.err.println("MessageAuditService save failed: " + e.getMessage());
        }
    }
}
