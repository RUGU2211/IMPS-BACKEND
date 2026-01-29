package com.hitachi.imps.service.heartbeat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.NpciMockClient;

/**
 * Sends outbound ReqHbt (ALIVE) to NPCI on a fixed schedule.
 *
 * Heartbeat Rule: System must generate heartbeat messages every 3 minutes.
 * If no response is received, NPCI may mark the bank BIN down and block outward traffic.
 */
@Service
@ConditionalOnProperty(prefix = "imps.heartbeat", name = "enabled", havingValue = "true")
public class HeartbeatSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatSchedulerService.class);

    @Autowired
    private NpciMockClient npciMockClient;

    @Value("${imps.org-id:BANK01}")
    private String orgId;

    /**
     * Send ALIVE heartbeat to NPCI every 3 minutes (configurable).
     * First run after initial-delay-ms; then every interval-ms.
     */
    @Scheduled(
        initialDelayString = "${imps.heartbeat.initial-delay-ms:15000}",
        fixedDelayString = "${imps.heartbeat.interval-ms:180000}"
    )
    public void sendScheduledHeartbeat() {
        String msgId = "HBT" + UUID.randomUUID().toString().replace("-", "").substring(0, 29);
        String txnId = msgId;
        String ts = OffsetDateTime.now().toString();

        String reqHbtXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <upi:ReqHbt xmlns:upi="http://npci.org/upi/schema/">
                <Head ver="1.0" ts="%s" orgId="%s" msgId="%s"/>
                <Txn id="%s" note="Heartbeat Check" refId="ALIVE" refUrl="https://www.npci.org.in/" ts="%s" type="Hbt"/>
                <HbtMsg type="ALIVE" value="NA"/>
            </upi:ReqHbt>
            """.formatted(ts, orgId, msgId, txnId, ts).trim();

        try {
            log.info("Sending scheduled ReqHbt (ALIVE) to NPCI, msgId={}", msgId);
            String response = npciMockClient.sendReqHbt(reqHbtXml);

            if (response == null || response.isBlank()) {
                log.error(
                    "No heartbeat response received from NPCI. NPCI may mark the bank BIN down and block outward traffic. msgId={}",
                    msgId
                );
            } else {
                log.info("Heartbeat response received from NPCI, msgId={}", msgId);
            }
        } catch (Exception e) {
            log.error(
                "Heartbeat send failed. No response from NPCI - BIN may be marked down and outward traffic blocked. msgId={}, error={}",
                msgId,
                e.getMessage(),
                e
            );
        }
    }
}
