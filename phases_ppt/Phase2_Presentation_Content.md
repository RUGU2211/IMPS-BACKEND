# Phase 2 Presentation – IMPS Backend (Hitachi Internship)
## Copy-paste ready for PPT | Focus: Imps-backend hardening; supporting: mock_switch, mock_npci

---

## Slide: Title
**IMPS Backend – Phase 2**
*End-to-End Flow, Security & Performance*
*Hitachi Internship Project*

---

## Slide: Agenda
- Phase 2 Objectives
- End-to-End Response Flow (IMPS → NPCI)
- Security (SSL/TLS)
- Performance & Hardening
- Supporting Projects (brief)
- Learnings & Next Steps

---

## Slide: Phase 1 Recap
**What Phase 1 Delivered (Imps-backend)**

- NPCI → Imps-backend (REST, XML): receive, validate, ACK, convert to ISO, route by institution.
- Imps-backend → Switch: send ISO, receive response (sync or callback at /imps/resppay/{txnId}).
- Persistence: transaction, message_audit_log (imps_db); dynamic routing via institution_master.
- **Supporting:** mock_switch, mock_npci for testing.

**Phase 2 builds on this** with production-ready response delivery, security, and performance.

---

## 1. Phase 2 Objectives

- **End-to-end response flow:** Ensure Imps-backend reliably delivers response XML to NPCI over REST (configurable NPCI base URL, retries/timeouts, error handling).
- **Security:** SSL/TLS for all REST and socket connections (Imps-backend, mock_switch, mock_npci); certificate management; secure configuration.
- **Performance & hardening:** Connection pooling, timeouts, resource limits; load testing; handling high volume and Switch unavailability (BANK_DOWN, retries).
- **Imps-backend remains the core;** mock_switch and mock_npci are updated only as needed to support testing (e.g. HTTPS, TLS).

---

## 2. End-to-End Response Flow (IMPS → NPCI)

- After Imps-backend receives Switch response (sync or callback), it converts ISO → XML, updates transaction (SUCCESS/FAILED), and writes audit (NPCI_*_XML_OUT).
- **Imps-backend** sends response XML to NPCI via **NpciRestClient** (HTTP POST to configured NPCI base URL). Phase 2 focuses on: configurable URL, timeouts, retry policy, and logging; no TCP socket for NPCI in production (REST only).
- Failure handling: if NPCI endpoint is unreachable, Imps-backend logs and may retry or mark for manual follow-up; transaction and audit already persisted.

---

## 3. Security (SSL/TLS)

- **Imps-backend:** HTTPS (e.g. 8443) for REST; TLS for socket (e.g. 9443, 9446) when enabled. Keystore/truststore configuration via application config.
- **mock_switch / mock_npci:** HTTPS and TLS enabled for testing end-to-end with Imps-backend over secure channels.
- Certificates: generation and rotation; no secrets in code; config-driven paths and passwords.
- **Focus:** Imps-backend as the single point that talks to both NPCI and Switch; securing both sides is part of Phase 2.

---

## 4. Performance & Hardening

- **Imps-backend:** Async processing (already in Phase 1); RestTemplate/HTTP client timeouts and connection pooling; database connection pool tuning; optional rate limiting or circuit breaker for Switch calls when Switch is down (BANK_DOWN).
- **Monitoring:** Health endpoints, metrics (e.g. request count, latency, error rate) for Imps-backend; logging and audit (message_audit_log) already in place.
- **Testing:** Load testing with mock_switch and mock_npci to validate Imps-backend under load; failure scenarios (Switch down, NPCI timeout).

---

## 5. Supporting Projects (Brief)

- **mock_switch:** Simulates Bank Switch (ISO 8583). Phase 2 may add HTTPS/TLS and configurable response delay for testing. Uses switch_db. Not part of production.
- **mock_npci:** Simulates NPCI (XML, ACK/Resp). Phase 2 may add HTTPS/TLS. Stateless. Used only to test Imps-backend response delivery.
- **Emphasis:** All business logic and security hardening are in **Imps-backend**; mocks only support testing.

---

## 6. Learnings & Next Steps

- **Learnings:** Production-ready REST client configuration; SSL/TLS in Java/Spring; performance tuning and resilience (timeouts, retries, BANK_DOWN handling).
- **Next (Phase 3):** UAT with bank/NPCI integration, go-live checklist, monitoring/alerting, runbooks, and handover.

---

*End of Phase 2 presentation.*
