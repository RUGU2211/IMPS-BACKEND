# Phase 1 Presentation Content – IMPS Backend (Hitachi Internship)
## Copy-paste ready for PPT | College Submission

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
- Project Details
- Learning and Experience
- Comments and Future Plan
- References

---

## Slide: Project Development – 3 Phases (Overview)
**Division of IMPS Backend Development**

| Phase | Focus | Status |
|-------|--------|--------|
| **Phase 1** | Foundation: NPCI→IMPS request handling, XML↔ISO conversion, routing, validation, persistence, REST + Socket, ACK-first flow | ✅ **Current (Submitted)** |
| **Phase 2** | End-to-end response flow (IMPS→NPCI outbound), security (SSL/TLS), performance & hardening | Planned |
| **Phase 3** | UAT, go-live readiness, monitoring, handover & documentation | Planned |

*This presentation covers **Phase 1** deliverables.*

---

## 1. Introduction to Internship

- **Duration:** [*Add your internship duration, e.g., 6 months*]
- **Role:** Software Development / Backend Development Intern
- **Project:** IMPS Backend – middleware between NPCI (National Payments Corporation of India) and the Bank Switch for IMPS (Immediate Payment Service) transactions.
- **Objective:** To design and implement the core backend that receives payment requests from NPCI in XML, converts them to ISO 8583 for the Switch, and manages routing, validation, and audit logging.
- **Deliverable for Phase 1:** Working request flow (NPCI → IMPS → Switch) with REST and TCP Socket support, ACK-first behaviour, and persistence in PostgreSQL.

---

## 2. About the Organization

- **Hitachi** is a global technology company with a strong presence in **Social Innovation Business** – combining OT (Operational Technology) and IT to deliver solutions in mobility, energy, industry, and finance.
- In **India**, Hitachi offers products and services in areas such as **digital payments**, **railways**, **power systems**, and **IT services**.
- The internship is in the domain of **payment systems**, specifically the **IMPS (Immediate Payment Service)** ecosystem regulated by **NPCI**.
- Working with Hitachi provided exposure to **enterprise-grade development practices**, **financial messaging standards (ISO 8583, NPCI XML)**, and **secure, auditable transaction handling**.

---

## 3. About the Department

- **Department:** [*Add exact department name if known, e.g., Digital Solutions / Payment Solutions / Engineering*]
- **Focus:** Development of **payment gateway and switch integration** solutions – middleware that connects banks and payment networks (e.g., NPCI) with internal bank switches.
- **Relevance to project:** The IMPS Backend is a core component that:
  - Receives requests from NPCI (REST or TCP Socket) in **XML**.
  - Converts to **ISO 8583** and communicates with the **Bank Switch** (REST or TCP Socket).
  - Performs **institution-based routing** (using IFSC/orgId), **validation**, and **audit logging** for compliance and troubleshooting.

---

## 4. Project Details

### 4.1 Problem Statement
- NPCI and the Bank Switch use **different message formats** (XML vs ISO 8583) and **different transport options** (REST and TCP Socket).
- A **middleware (IMPS Backend)** is required to:
  - Accept requests from NPCI (XML).
  - Convert to ISO 8583 and forward to the Switch.
  - Convert Switch responses back to XML and deliver to NPCI.
  - Ensure **routing by institution**, **duplicate detection**, **audit trail**, and **NPCI-compliant socket behaviour** (ACK on same connection, response on new connection).

### 4.2 Scope of Phase 1
- **Request types implemented:** ReqPay, ReqChkTxn, ReqHbt, ReqListAccPvd, ReqValAdd (and corresponding response handling).
- **Protocols:** REST (HTTP/HTTPS) and TCP Socket (plain and TLS).
- **Behaviour:** ACK sent immediately on same connection; processing (convert → Switch → persist) done asynchronously; response sent to NPCI on a new connection (Phase 3 flow).
- **Persistence:** PostgreSQL (`imps_db`) – `transaction` and `message_audit_log` tables.
- **Routing:** Using `institution_master` (switch_ip, switch_port / REST base URL) by orgId/IFSC.
- **Supporting components:** Mock Switch and Mock NPCI for integration testing.

### 4.3 Technology Stack

| Layer        | Technology |
|-------------|------------|
| Runtime     | Java 17    |
| Framework   | Spring Boot 3.3.5 |
| Build       | Maven 3.6+ |
| Database    | PostgreSQL (imps_db) |
| Persistence | Spring Data JPA (Hibernate) |
| Message formats | NPCI XML, ISO 8583 (jPOS 2.1.7) |
| Transport   | REST (HTTP/HTTPS), TCP Socket (plain/TLS) |

### 4.4 Architecture (High-Level)
- **NPCI** ↔ **IMPS Backend** (XML) – REST 8081/8443, Socket 9083/9443.
- **IMPS Backend** ↔ **Switch** (ISO 8583) – REST 8082/8444, Socket 9084/9444.
- **IMPS Backend** → **NPCI** (response) – REST 8083/8445, Socket 9085/9445.
- **Switch** → **IMPS Backend** (reverse/callback) – Socket 9086/9446.

### 4.5 Phase 1 Flow (Simplified)
1. NPCI sends **Req** (e.g., ReqPay) in XML to IMPS (REST or Socket).
2. IMPS sends **ACK** immediately (on same socket if socket mode); NPCI closes the request connection.
3. IMPS converts **XML → ISO**, resolves Switch address from `institution_master`, and sends request to Switch.
4. Switch responds with **Resp** (ISO); IMPS converts **ISO → XML**, updates `transaction` and `message_audit_log`.
5. IMPS sends response to NPCI (REST or new socket connection as per NPCI-compliant flow).

---

## 5. Learning and Experience

- **Technical:**
  - **Spring Boot** – REST APIs, async processing, configuration (profiles, .env).
  - **Financial messaging** – NPCI XML schema and **ISO 8583** (jPOS) for payment and check-transaction flows.
  - **TCP Socket programming** – length-prefixed messages (4-byte length + payload), TLS, client/server roles.
  - **Database** – JPA entities, repositories, transaction and audit table design.
  - **Routing & validation** – institution-based routing, duplicate txn_id handling, structured exception handling.

- **Process & Tools:**
  - Documentation (HLD, LLD, API docs, flow charts) and maintaining a clear project structure.
  - Use of **Postman** for API testing and **mock services** (Mock Switch, Mock NPCI) for integration testing.
  - Version control and collaborative development in an enterprise setting.

---

## 6. Comments and Future Plan

- **Comments:** Phase 1 establishes the core request path (NPCI → IMPS → Switch) with dual protocol support (REST + Socket) and full audit. The NPCI-compliant socket flow (ACK on same connection, response on new connection) is implemented and tested with mock components.
- **Future Plan (Phase 2 & 3):**
  - **Phase 2:** Strengthen outbound IMPS→NPCI flow for production, SSL/TLS rollout, performance tuning, and security hardening.
  - **Phase 3:** User acceptance testing (UAT), go-live readiness, monitoring/alerting, and formal handover with runbooks and operational documentation.

---

## 7. References

- NPCI IMPS technical specifications (message formats, socket behaviour).
- ISO 8583 – Financial transaction card-originated messages.
- Spring Boot 3.x and Spring Data JPA documentation.
- Project documentation: `docs/` (Architecture, LLD, API, DB, Integration, Security, Deployment, Runbook, Code Structure).
- jPOS 2.1.7 – ISO 8583 message handling.

---

*End of Phase 1 presentation content.*
