# IMPS Backend – All Phases Final Presentation (Hitachi Internship)
## Phase 1 + Phase 2 + Phase 3 | Copy-paste ready for PPT | Focus: Imps-backend; supporting: mock_npci, mock_switch

---

## Slide: Title
**IMPS Backend – Complete Project (Phases 1, 2 & 3)**
*Hitachi Internship | Final Presentation*
*SVKM's NMIMS Mukesh Patel School of Technology Management & Engineering*

---

## Slide: Agenda
- Introduction & Organization
- Project Overview (3 Phases)
- **Imps-backend** – Core Work (All Phases)
- Supporting Projects – mock_switch, mock_npci
- Technology & Architecture
- Learnings, Conclusion & References

---

## Slide: Project Structure
**What This Project Contains**

| Component | Role | Focus in presentation |
|-----------|------|------------------------|
| **Imps-backend** | Middleware between NPCI and Bank Switch. Converts XML ↔ ISO 8583, routes, validates, persists, delivers response. | **Primary** – all phases and flows |
| **mock_switch** | Simulates Bank Switch (ISO 8583). Used only for testing Imps-backend. | Supporting – brief |
| **mock_npci** | Simulates NPCI (XML, ACK/Resp). Used only for testing Imps-backend. | Supporting – brief |

---

## 1. Introduction & Organization

- **Internship:** [Duration], Backend Development, Hitachi.
- **Organization:** Hitachi – Social Innovation Business; India: digital payments, railways, power systems, IT services.
- **Department:** Payment gateway and switch integration. **Imps-backend** is the core deliverable; mock_switch and mock_npci support testing.

---

## 2. Project Overview – Three Phases

| Phase | Focus | Status |
|-------|--------|--------|
| **Phase 1** | Foundation: NPCI→IMPS request handling, XML↔ISO conversion, dynamic routing, validation, persistence (imps_db), REST, ACK-first flow. Supporting: mock_switch, mock_npci for testing. | Completed |
| **Phase 2** | End-to-end response flow (IMPS→NPCI), SSL/TLS, performance & security hardening. Imps-backend remains core; mocks updated for HTTPS/TLS testing. | Completed |
| **Phase 3** | UAT, go-live readiness, monitoring/alerting, runbooks, handover. All centred on Imps-backend. | Completed |

---

## 3. Imps-backend – Core Work (All Phases)

### 3.1 What Imps-backend Does
- Sits **between NPCI and the Bank Switch.** NPCI sends requests in **XML (REST only)**; Bank Switch uses **ISO 8583**. **Imps-backend** does:
  - **Receive** (REST): NPCI POSTs XML to /imps/reqpay/{txnId}, etc. Validate (duplicate txnId, mandatory fields, institution). Return **ACK** in HTTP response. Process **asynchronously**.
  - **Convert & route:** XML → ISO 8583 (XmlToIsoConverter). **Dynamic routing:** extract orgId from XML, lookup **institution_master** → Switch URL/host:port. Send ISO to Switch (REST or socket as configured).
  - **Receive response:** Switch responds (sync or callback POST to /imps/resppay/{txnId} with ISO). Convert ISO → XML (IsoToXmlConverter). Update **transaction** (SUCCESS/FAILED) and **message_audit_log**.
  - **Deliver to NPCI:** Send response XML to NPCI over REST (NpciRestClient). No TCP socket for NPCI in production.

### 3.2 Phase 1 – Foundation (Imps-backend)
- REST APIs under /imps (reqpay, reqchktxn, reqhbt, reqlistaccpvd, reqvaladd; resppay, respchktxn, etc.). Dynamic path: /imps/{apiType}/{txnId}. Content-Type: XML (NPCI) or octet-stream (Switch callback).
- Persistence: **imps_db** – transaction (INIT → ISO_SENT → SUCCESS/FAILED), message_audit_log (NPCI_*_XML_IN, SWITCH_*_ISO_OUT/IN, NPCI_*_XML_OUT), institution_master (routing).
- Validation: duplicate txnId, ReqPay rules, institution/IFSC; BANK_DOWN when Switch (institution) is inactive.

### 3.3 Phase 2 – Hardening (Imps-backend)
- Reliable response delivery to NPCI (configurable URL, timeouts, retries). SSL/TLS for all connections (Imps-backend, and mocks for test). Performance: connection pooling, timeouts, handling Switch unavailability.

### 3.4 Phase 3 – Go-Live (Imps-backend)
- UAT with bank/NPCI (or staging). Go-live checklist, monitoring (health, latency, errors), alerting (BANK_DOWN, NPCI failure). Runbooks and handover documentation for **Imps-backend** operations.

---

## 4. Supporting Projects – mock_switch, mock_npci

- **mock_switch:** Simulates the Bank Switch. Accepts ISO 8583 from Imps-backend (REST or socket), returns response ISO (e.g. RespPay 0210). Uses **switch_db** (e.g. account_master). Used only so that Imps-backend can be tested without a real switch. **Not deployed in production.**
- **mock_npci:** Simulates NPCI. Accepts XML from Imps-backend (e.g. RespPay), returns ACK. Stateless. Used only so that Imps-backend can be tested without live NPCI. **Not deployed in production.**
- **Summary:** All business logic, routing, conversion, and persistence are in **Imps-backend**. mock_switch and mock_npci are test doubles only.

---

## 5. Technology & Architecture

### 5.1 Technology (Imps-backend)
| Layer | Technology |
|-------|-------------|
| Runtime | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Build | Maven 3.6+ |
| Database | PostgreSQL (imps_db) |
| Message formats | NPCI XML, ISO 8583 (jPOS 2.1.7) |
| Transport | REST (HTTP/HTTPS); optional socket for Switch |

### 5.2 Architecture (High-Level)
- **NPCI** (REST, XML) ↔ **Imps-backend** (convert, route, persist, audit) ↔ **Bank Switch** (ISO 8583, REST or socket). **Imps-backend** sends response to NPCI over REST.
- **mock_switch** and **mock_npci** sit in test environment only, simulating Switch and NPCI for Imps-backend.

---

## 6. Learnings, Conclusion & References

- **Learnings:** Spring Boot REST, NPCI XML and ISO 8583 (jPOS), dynamic routing (institution_master), persistence and audit, SSL/TLS, performance and resilience, UAT and operations (runbooks, monitoring).
- **Conclusion:** The project delivered **Imps-backend** as a production-ready middleware between NPCI (REST, XML) and the Bank Switch (ISO 8583), with full request/response flow, dynamic routing, validation, and audit. **mock_switch** and **mock_npci** support integration testing only. All three phases (foundation, hardening, go-live) are complete.
- **References:** NPCI IMPS specifications; ISO 8583; Spring Boot and Spring Data JPA; jPOS.

---

*End of All Phases Final presentation.*
