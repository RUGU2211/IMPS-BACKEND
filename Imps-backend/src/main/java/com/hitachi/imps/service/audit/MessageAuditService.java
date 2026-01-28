package com.hitachi.imps.service.audit;

import java.time.LocalDateTime;
import java.util.Base64;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitachi.imps.entity.MessageAuditLog;
import com.hitachi.imps.repository.MessageAuditLogRepository;

@Service
public class MessageAuditService {

    @Autowired
    private MessageAuditLogRepository repo;

    private final ObjectMapper mapper = new ObjectMapper();

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
       (Remove null bytes and invalid UTF-8)
       =============================== */
    private String sanitize(String input) {
        if (input == null) return null;
        // Remove null bytes and other control characters
        return input.replaceAll("\\x00", "").replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
    }

    /* ===============================
       RAW MESSAGE (XML / ISO STRING)
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
       Convert to Base64 for safe storage
       =============================== */
    public void saveRawBytes(String txnId, String stage, byte[] data) {
        MessageAuditLog log = new MessageAuditLog();
        log.setTxnId(txnId);
        log.setStage(stage);
        // Store binary as Base64 encoded string
        log.setRawMessage("BASE64:" + Base64.getEncoder().encodeToString(data));
        log.setParsedMessage(null);
        log.setCreatedAt(LocalDateTime.now());

        repo.save(log);
    }

    /* ===============================
       PARSED MESSAGE (Map / ISOMsg)
       =============================== */
    public void saveParsed(String txnId, String stage, Object data) {
        try {
            String content;

            if (data instanceof ISOMsg iso) {
                // ✅ Convert ISO safely (NO Jackson)
                content = isoToString(iso);
            } else {
                // ✅ JSON only for Map / simple objects
                content = mapper.writeValueAsString(data);
            }

            MessageAuditLog log = new MessageAuditLog();
            log.setTxnId(txnId);
            log.setStage(stage);
            log.setParsedMessage(content);
            log.setCreatedAt(LocalDateTime.now());

            repo.save(log);

        } catch (Exception e) {
            throw new RuntimeException("Audit save failed", e);
        }
    }
}
