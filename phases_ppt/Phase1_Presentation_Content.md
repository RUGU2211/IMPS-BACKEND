# Phase 1 Presentation – IMPS Backend (Hitachi Internship)
## Copy-paste ready for PPT | Focus: Imps-backend; supporting: mock_switch, mock_npci

---

## Slide: Title
**IMPS Backend – Phase 1**
*Internship Project | Hitachi*
*SVKM's NMIMS Mukesh Patel School of Technology Management & Engineering*

---

## Slide: Agenda
- Introduction to Internship
- About the Organization
- About the Department
- Project Details (Phase 1)
- Learning and Experience
- Comments and Future Plan
- References

---

## Slide: Project Development – 3 Phases (Overview)
**Division of IMPS Backend Development**

| Phase | Focus | Status |
|-------|--------|--------|
| **Phase 1** | Foundation: NPCI→IMPS request handling, XML↔ISO conversion, routing, validation, persistence, REST, ACK-first flow | ✅ **Completed** |
| **Phase 2** | End-to-end response flow (IMPS→NPCI), security (SSL/TLS), performance & hardening | Planned |
| **Phase 3** | UAT, go-live readiness, monitoring, handover & documentation | Planned |

*This presentation covers **Phase 1** deliverables.*

---

## 1. Introduction to Internship

- **Duration:** [*Add your internship duration*]
- **Role:** Software Development / Backend Development Intern
- **Project:** **Imps-backend** – middleware between NPCI and the Bank Switch for IMPS transactions.
- **Objective:** Implement the core backend that receives payment requests from NPCI in XML (REST), converts them to ISO 8583 for the Switch, and manages routing, validation, and audit logging.
- **Phase 1 deliverable:** Working request flow (NPCI → IMPS → Switch) with REST support, ACK-first behaviour, and persistence in PostgreSQL. **Supporting projects:** mock_switch and mock_npci (for integration testing only).

---

## 2. About the Organization

- **Hitachi** – global technology company, Social Innovation Business (OT + IT) in mobility, energy, industry, and finance.
- In **India:** digital payments, railways, power systems, IT services.
- Internship in **payment systems** (IMPS ecosystem, NPCI). Exposure to **enterprise practices**, **ISO 8583**, **NPCI XML**, and **auditable transaction handling**.

---

## 3. About the Department

- **Department:** [*Add department name*]
- **Focus:** Payment gateway and switch integration – middleware connecting NPCI with bank switches.
- **Relevance:** **Imps-backend** receives XML from NPCI (REST), converts to ISO 8583, routes by institution (IFSC/orgId), and performs validation and audit logging. mock_switch and mock_npci are test doubles used only to test the backend.

---

## 4. Project Details – Phase 1

### 4.1 Problem Statement
- NPCI uses **XML**; Bank Switch uses **ISO 8583**. A **middleware (Imps-backend)** is required to convert, route, validate, and audit.
- **Imps-backend** accepts requests from NPCI (REST, XML), converts to ISO 8583, forwards to the Switch, converts responses back to XML, and delivers to NPCI.

### 4.2 Scope of Phase 1 (Imps-backend focus)
- **Request types:** ReqPay, ReqChkTxn, ReqHbt, ReqListAccPvd, ReqValAdd (and response handling).
- **Protocol:** REST (HTTP/HTTPS) for NPCI ↔ IMPS; REST or socket for IMPS ↔ Switch (configurable).
- **Behaviour:** ACK returned immediately in HTTP response; processing (convert → Switch → persist) asynchronous; response sent to NPCI over REST.
- **Persistence (Imps-backend only):** PostgreSQL **imps_db** – `transaction`, `message_audit_log`, `institution_master`.
- **Routing:** Dynamic routing via `institution_master` (orgId/IFSC → Switch URL/host:port).
- **Supporting projects:** **mock_switch** (simulates Bank Switch, ISO 8583, switch_db); **mock_npci** (simulates NPCI, XML, stateless). Both used only for testing Imps-backend.

### 4.3 Technology Stack (Imps-backend)
| Layer | Technology |
|-------|-------------|
| Runtime | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Build | Maven 3.6+ |
| Database | PostgreSQL (imps_db) |
| Message formats | NPCI XML, ISO 8583 (jPOS 2.1.7) |
| Transport | REST (HTTP/HTTPS) |

### 4.4 Architecture (High-Level)
- **NPCI** ↔ **Imps-backend** (XML, REST).
- **Imps-backend** ↔ **Switch** (ISO 8583, REST or socket).
- **Imps-backend** → **NPCI** (response, REST).
- **Switch** → **Imps-backend** (callback, e.g. /imps/resppay/{txnId}).

### 4.5 Phase 1 Flow (Simplified)
1. NPCI sends **Req** (XML) to Imps-backend (POST /imps/reqpay/{txnId}).
2. Imps-backend validates, audits, creates transaction (INIT), returns **ACK** in HTTP response.
3. Imps-backend converts **XML → ISO**, resolves Switch from `institution_master`, sends request to Switch.
4. Switch responds (ISO); Imps-backend converts **ISO → XML**, updates transaction (SUCCESS/FAILED), audits.
5. Imps-backend sends response to NPCI over REST.

---

## 5. Learning and Experience

- **Technical:** Spring Boot REST APIs, async processing; NPCI XML and ISO 8583 (jPOS); JPA (transaction, message_audit_log); dynamic routing (institution_master); validation and exception handling.
- **Process:** Postman for API testing; mock_switch and mock_npci for end-to-end testing of Imps-backend; documentation and project structure.

---

## 6. Comments and Future Plan

- **Comments:** Phase 1 delivers the core request path (NPCI → IMPS → Switch) with REST, full audit, and dynamic routing. Supporting projects (mock_switch, mock_npci) enable testing without live NPCI/Switch.
- **Phase 2:** Outbound IMPS→NPCI flow, SSL/TLS, performance, security hardening.
- **Phase 3:** UAT, go-live, monitoring, runbooks, handover.

---

## 7. References

- NPCI IMPS technical specifications.
- ISO 8583 – Financial transaction card-originated messages.
- Spring Boot and Spring Data JPA documentation.
- jPOS – ISO 8583 message handling.

---

*End of Phase 1 presentation.*
