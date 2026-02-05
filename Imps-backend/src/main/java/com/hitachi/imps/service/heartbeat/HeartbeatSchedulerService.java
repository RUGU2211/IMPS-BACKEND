package com.hitachi.imps.service.heartbeat;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.hitachi.imps.client.NpciMockClient;
import com.hitachi.imps.entity.InstitutionMaster;
import com.hitachi.imps.repository.InstitutionMasterRepository;
import com.hitachi.imps.service.ImpsIdGeneratorService;

/**
 * Sends outbound ReqHbt (ALIVE) to NPCI on a fixed schedule – one request per enrolled bank.
 *
 * Every 3 minutes, fetches all active institutions from public.institution_master
 * and sends one separate ReqHbt per bank. New banks added later are picked up automatically.
 *
 * Heartbeat Rule: System must generate heartbeat messages every 3 minutes per bank.
 */
@Service
@ConditionalOnProperty(prefix = "imps.heartbeat", name = "enabled", havingValue = "true")
public class HeartbeatSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatSchedulerService.class);

    @Autowired
    private NpciMockClient npciMockClient;

    @Autowired
    private ImpsIdGeneratorService idGenerator;

    @Autowired
    private InstitutionMasterRepository institutionMasterRepository;

    /**
     * Send ALIVE heartbeat to NPCI every 3 minutes – one ReqHbt per active bank in institution_master.
     * Dynamic: new banks get heartbeats automatically on next run.
     */
    @Scheduled(
        initialDelayString = "${imps.heartbeat.initial-delay-ms:15000}",
        fixedDelayString = "${imps.heartbeat.interval-ms:180000}"
    )
    public void sendScheduledHeartbeat() {
        List<InstitutionMaster> banks = institutionMasterRepository.findByActiveTrue();
        if (banks == null || banks.isEmpty()) {
            log.warn("No active institutions in institution_master – skipping heartbeat run");
            return;
        }

        log.info("Sending ReqHbt (ALIVE) to NPCI for {} enrolled bank(s)", banks.size());
        String ts = OffsetDateTime.now().toString();

        for (InstitutionMaster inst : banks) {
            String bankName = inst.getName() != null ? inst.getName() : inst.getBankCode();
            String bpc = ImpsIdGeneratorService.normalizeBpc(inst.getBankCode());
            String orgIdForBank = (inst.getRequestOrgId() != null && !inst.getRequestOrgId().isBlank())
                ? inst.getRequestOrgId()
                : (inst.getBankCode() != null ? inst.getBankCode() : "BANK");

            String msgId = idGenerator.generateMsgId(bpc);
            String txnId = idGenerator.generateTxnId(bpc);

            String reqHbtXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <upi:ReqHbt xmlns:upi="http://npci.org/upi/schema/">
                    <Head ver="1.0" ts="%s" orgId="%s" msgId="%s"/>
                    <Txn id="%s" note="Heartbeat Check" refId="ALIVE" refUrl="https://www.npci.org.in/" ts="%s" type="Hbt"/>
                    <HbtMsg type="ALIVE" value="NA"/>
                </upi:ReqHbt>
                """.formatted(ts, orgIdForBank, msgId, txnId, ts).trim();

            try {
                log.debug("Sending ReqHbt for bank {} (orgId={}, msgId={})", bankName, orgIdForBank, msgId);
                String response = npciMockClient.sendReqHbt(reqHbtXml, txnId);

                if (response == null || response.isBlank()) {
                    log.error("No heartbeat response from NPCI for bank {}. msgId={}", bankName, msgId);
                } else {
                    log.info("Heartbeat response received for bank {}, msgId={}", bankName, msgId);
                }
            } catch (Exception e) {
                log.error("Heartbeat send failed for bank {}. msgId={}, error={}", bankName, msgId, e.getMessage(), e);
            }
        }
    }
}
