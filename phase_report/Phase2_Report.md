# SVKM's NMIMS  
## Mukesh Patel School of Technology Management & Engineering  
## Department of Computer Engineering  

---

# A REPORT ON  
# **"IMPS Backend Transaction Processing System"**  
## Phase 2 – End-to-End Response Flow, Security & Performance  

## By  
## Rugved Manoj Kharde  
## 70612400081  
## A029  
## Master of Computer Applications (MCA)  

### Hitachi Payment Services Pvt. Ltd.  

### May 2026  

---

# A PROJECT REPORT ON  
# **"IMPS Backend Transaction Processing System – Phase 2"**  
## By  
## Rugved Manoj Kharde  

A Report submitted in partial fulfilment of the requirements of 2 years Master of Computer Applications (MCA) Program of Mukesh Patel School of Technology, Management and Engineering, NMIMS.

---

### Under the Supervision of:

**Industry Mentor**  
Mr. Chetan Mokashi  
(Deputy Vice President at Hitachi Payments Ltd.)

**College Mentor**  
Ms. Swarnalata Bollavarapu  

---

# TABLE OF CONTENTS

| | |
|---|---|
| TABLE OF CONTENTS | |
| ABSTRACT | |
| **Chapter 1: INTRODUCTION** | |
| &nbsp;&nbsp;&nbsp;&nbsp;1.1 Phase 1 Recap | |
| &nbsp;&nbsp;&nbsp;&nbsp;1.2 Phase 2 Objectives | |
| **Chapter 2: PHASE 2 PROJECT DETAILS** | |
| &nbsp;&nbsp;&nbsp;&nbsp;2.1 End-to-End Response Flow (IMPS → NPCI) | |
| &nbsp;&nbsp;&nbsp;&nbsp;2.2 Security (SSL/TLS) | |
| &nbsp;&nbsp;&nbsp;&nbsp;2.3 Performance & Hardening | |
| &nbsp;&nbsp;&nbsp;&nbsp;2.4 Supporting Projects (Brief) | |
| **Chapter 3: CONCLUSION** | |
| &nbsp;&nbsp;&nbsp;&nbsp;3.1 Learnings | |
| &nbsp;&nbsp;&nbsp;&nbsp;3.2 Next Steps (Phase 3) | |
| **Chapter 4: REFERENCES** | |

---

# ABSTRACT

This report presents **Phase 2** of the IMPS Backend Transaction Processing System at Hitachi Payment Services Pvt. Ltd. Phase 2 builds on Phase 1 by ensuring production-ready **end-to-end response delivery** from IMPS to NPCI over REST, **security** via SSL/TLS for all REST and socket connections, and **performance and hardening** including connection pooling, timeouts, retries, and handling of Switch unavailability (BANK_DOWN). Imps-backend remains the core deliverable; mock_switch and mock_npci are updated only as needed to support testing over HTTPS/TLS. Phase 2 does not introduce a TCP socket for NPCI—NPCI communication remains exclusively over REST (HTTP/HTTPS).

---

# Chapter 1  
# INTRODUCTION  

## 1.1 Phase 1 Recap

**What Phase 1 Delivered (Imps-backend):**

- NPCI → Imps-backend (REST, XML): receive, validate, ACK, convert to ISO, route by institution.
- Imps-backend → Switch: send ISO, receive response (sync or callback at /imps/resppay/{txnId}).
- Persistence: transaction, message_audit_log (imps_db); dynamic routing via institution_master.
- **Supporting:** mock_switch, mock_npci for testing.

Phase 2 builds on this with production-ready response delivery to NPCI, security, and performance.

## 1.2 Phase 2 Objectives

- **End-to-end response flow:** Ensure Imps-backend reliably delivers response XML to NPCI over REST (configurable NPCI base URL, retries/timeouts, error handling).
- **Security:** SSL/TLS for all REST and socket connections (Imps-backend, mock_switch, mock_npci); certificate management; secure configuration.
- **Performance & hardening:** Connection pooling, timeouts, resource limits; load testing; handling high volume and Switch unavailability (BANK_DOWN, retries).
- **Imps-backend remains the core;** mock_switch and mock_npci are updated only as needed to support testing (e.g. HTTPS, TLS).

---

# Chapter 2  
# PHASE 2 PROJECT DETAILS  

## 2.1 End-to-End Response Flow (IMPS → NPCI)

After Imps-backend receives the Switch response (sync or callback), it converts ISO → XML, updates transaction (SUCCESS/FAILED), and writes audit (NPCI_*_XML_OUT). **Imps-backend** sends response XML to NPCI via **NpciRestClient** (HTTP POST to configured NPCI base URL). Phase 2 focuses on:

- Configurable NPCI base URL, timeouts, and retry policy.
- Logging and error handling; no TCP socket for NPCI in production (REST only).
- Failure handling: if NPCI endpoint is unreachable, Imps-backend logs and may retry or mark for manual follow-up; transaction and audit are already persisted.

## 2.2 Security (SSL/TLS)

- **Imps-backend:** HTTPS (e.g. port 8443) for REST; TLS for socket (e.g. 9446) when enabled. Keystore/truststore configuration via application config.
- **mock_switch / mock_npci:** HTTPS and TLS enabled for testing end-to-end with Imps-backend over secure channels.
- Certificates: generation and rotation; no secrets in code; config-driven paths and passwords.
- **Focus:** Imps-backend as the single point that talks to both NPCI and Switch; securing both sides is part of Phase 2.

## 2.3 Performance & Hardening

- **Imps-backend:** Async processing (from Phase 1); RestTemplate/HTTP client timeouts and connection pooling; database connection pool tuning; optional rate limiting or circuit breaker for Switch calls when Switch is down (BANK_DOWN).
- **Monitoring:** Health endpoints, metrics (e.g. request count, latency, error rate) for Imps-backend; logging and audit (message_audit_log) already in place.
- **Testing:** Load testing with mock_switch and mock_npci to validate Imps-backend under load; failure scenarios (Switch down, NPCI timeout).

## 2.4 Supporting Projects (Brief)

- **mock_switch:** Simulates Bank Switch (ISO 8583). Phase 2 may add HTTPS/TLS and configurable response delay for testing. Uses switch_db. Not part of production.
- **mock_npci:** Simulates NPCI (XML, ACK/Resp). Phase 2 may add HTTPS/TLS. Stateless. Used only to test Imps-backend response delivery.
- **Emphasis:** All business logic and security hardening are in **Imps-backend**; mocks only support testing.

---

# Chapter 3  
# CONCLUSION  

## 3.1 Learnings

Production-ready REST client configuration; SSL/TLS in Java/Spring; performance tuning and resilience (timeouts, retries, BANK_DOWN handling).

## 3.2 Next Steps (Phase 3)

UAT with bank/NPCI integration, go-live checklist, monitoring/alerting, runbooks, and handover.

---

# Chapter 4  
# REFERENCES  

1. NPCI IMPS technical specifications.
2. Spring Boot 3.x documentation – https://spring.io/projects/spring-boot
3. Project documentation (E:\Hitachi_Project\docs) – Architecture, API, Security, Configuration, Deployment.

---

*End of Phase 2 Report*
