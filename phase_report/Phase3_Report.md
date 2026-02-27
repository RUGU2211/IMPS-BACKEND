# SVKM's NMIMS  
## Mukesh Patel School of Technology Management & Engineering  
## Department of Computer Engineering  

---

# A REPORT ON  
# **"IMPS Backend Transaction Processing System"**  
## By  
## Rugved Manoj Kharde  
## 70612400081  
## A029  
## Master of Computer Applications (MCA)  

### Hitachi Payment Services Pvt. Ltd.  

### May 2026  

---

# A PROJECT REPORT ON  
# **"IMPS Backend Transaction Processing System"**  
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

# Completion Certificate  

---

**SVKM's NMIMS**  
Mukesh Patel School of Technology Management & Engineering  
Vile Parle (W), Mumbai - 400 056.

**PROJECT REPORT – Semester IV – MCA**

Submitted in Partial Fulfillment of the requirements for Technical Project/Training for MCA Semester IV

| | |
|---|---|
| **Name of the Student:** | Rugved Manoj Kharde |
| **Roll No. & Batch:** | A029 |
| **Academic Year:** | 2025-2026 |
| **Name of the Discipline:** | MCA |
| **Name and Address of the Company:** | HITACHI PAYMENT SERVICES PRIVATE LIMITED, G Corp Tech Park, 9th Floor, Sai Nagar, Anand Nagar, Kasarvadavali, Ghodbunder Road, Thane (W) – 400615, Maharashtra, India |
| **Training Period:** | December 15, 2025  To  June 13, 2026 |

**THIS IS TO CERTIFY THAT**  
Mr. Rugved Manoj Kharde (Exam Seat No. _____________) has satisfactorily completed his Training/Project Work, submitted the training report and appeared for the Presentation & Viva as required.

| External Examiner | Internal Examiner | Head of Dept. | Chairperson/Dean |
|------------------|-------------------|---------------|-------------------|
| __________ | __________ | __________ | __________ |

**Date:**  
**Place:**  
**Seal of the University**

---

# TABLE OF CONTENTS

| | |
|---|---|
| CERTIFICATE OF COMPLETION | |
| ACKNOWLEDGEMENT | |
| TABLE OF CONTENTS | |
| ABSTRACT | |
| **Chapter 1: INTRODUCTION** | |
| &nbsp;&nbsp;&nbsp;&nbsp;1.1 About the Company | |
| &nbsp;&nbsp;&nbsp;&nbsp;1.2 Overview | |
| **Chapter 2: PROJECT DETAILS** | |
| &nbsp;&nbsp;&nbsp;&nbsp;2.1 Project Aim | |
| &nbsp;&nbsp;&nbsp;&nbsp;2.2 Project Scope | |
| &nbsp;&nbsp;&nbsp;&nbsp;2.3 Project Methodology | |
| &nbsp;&nbsp;&nbsp;&nbsp;2.4 Project Timeline | |
| &nbsp;&nbsp;&nbsp;&nbsp;2.5 Technologies Used | |
| &nbsp;&nbsp;&nbsp;&nbsp;2.6 ISO 8583 Standard and Payment Message Format | |
| &nbsp;&nbsp;&nbsp;&nbsp;2.7 Future Scope | |
| &nbsp;&nbsp;&nbsp;&nbsp;2.8 Diagrams | |
| **Chapter 3: CONCLUSION** | |
| &nbsp;&nbsp;&nbsp;&nbsp;3.1 Learnings | |
| &nbsp;&nbsp;&nbsp;&nbsp;3.2 Conclusion | |
| **Chapter 4: REFERENCES** | |

---

# ABSTRACT

This report presents the design and implementation of the IMPS (Immediate Payment Service) Backend Transaction Processing System, a middleware developed during an internship at Hitachi Payment Services Pvt. Ltd. The system sits between the National Payments Corporation of India (NPCI) and the Bank Switch, enabling real-time inter-bank fund transfers by translating between NPCI's XML-based messages and the Switch's ISO 8583 format.

**In production, NPCI communicates with the IMPS Backend only through REST API (HTTP/HTTPS); there is no TCP socket interface for NPCI.** The backend receives payment requests in XML over REST, validates and acknowledges them promptly by returning an ACK in the HTTP response body, converts each request to ISO 8583, routes requests to the appropriate Bank Switch by institution (IFSC/orgId) using the institution_master table, receives Switch responses (synchronously or via callback), converts them back to XML, updates transaction state and audit log, and delivers responses to NPCI over REST. The backend maintains full transaction lifecycle (INIT → ISO_SENT → SUCCESS/FAILED) and complete message audit (message_audit_log) in PostgreSQL. It also supports the reverse flow (Switch → IMPS → NPCI) and the Switch-to-IMPS callback for asynchronous responses. The project includes the Imps-backend application as the core deliverable, with Mock Switch and Mock NPCI used only for integration testing. Technologies used include Java 17, Spring Boot 3.3, Maven, PostgreSQL, and jPOS for ISO 8583 message handling.

---

# Chapter 1  
# INTRODUCTION  

## 1.1 About the Company

Hitachi is a global technology company with a strong focus on Social Innovation Business, combining Operational Technology (OT) and Information Technology (IT) to deliver solutions across mobility, energy, industry, and finance. In India, Hitachi operates in areas such as digital payments, railways, power systems, and IT services.

Hitachi Payment Services Pvt. Ltd. is engaged in payment solutions and the IMPS ecosystem regulated by the National Payments Corporation of India (NPCI). The internship was undertaken in the domain of payment systems. Working with Hitachi provided exposure to enterprise-grade development practices, financial messaging standards (ISO 8583, NPCI XML), and secure, auditable transaction handling expected in banking and payment infrastructure.

## 1.2 Overview

IMPS (Immediate Payment Service) allows real-time, 24×7 inter-bank fund transfers in India. NPCI operates the IMPS network and defines XML-based message formats for participants. **NPCI communicates with banks and payment gateways only through REST API (HTTP/HTTPS); the project is designed and documented with NPCI ↔ IMPS communication exclusively over REST.** There is no TCP socket interface between NPCI and the IMPS Backend in the production design.

Banks and payment service providers use an internal Bank Switch that speaks ISO 8583 for financial messages. The IMPS Backend developed in this project acts as middleware between NPCI and the Bank Switch: it accepts requests from NPCI in XML over REST (e.g. POST /imps/reqpay/{txnId}), validates and sends an immediate ACK in the HTTP response, converts each request to ISO 8583, routes it to the correct Switch by institution (IFSC/orgId), receives the Switch response (sync or callback), converts it back to XML, updates the transaction and audit log, and sends the response to NPCI over REST. It also accepts requests from the Switch (reverse flow) and converts them to XML for NPCI over REST, and receives response callbacks from the Switch (e.g. POST /imps/resppay/{txnId}) when the Switch responds asynchronously. The system performs validation, institution-based routing, duplicate transaction detection, and end-to-end audit logging. A periodic heartbeat mechanism (e.g. every 3 minutes) checks Switch connectivity and updates the institution_master table. The project comprises the Imps-backend application (core), Mock Switch (ISO 8583, switch_db), and Mock NPCI (XML, stateless) for integration testing only.

![01_System_Architecture](diagrams/01_System_Architecture.png)  
*Figure 1: System architecture – NPCI (REST), IMPS Backend, Bank Switch*

---

# Chapter 2  
# PROJECT DETAILS  

## 2.1 Project Aim

The aim of the project was to design and implement the complete IMPS Backend so that it:

1. **End-to-end REST flow with NPCI:** Receives payment and related requests from NPCI over REST only (XML), validates and acknowledges them promptly by returning an ACK in the HTTP response body, converts XML to ISO 8583, routes requests to the appropriate Bank Switch by institution (IFSC/orgId), receives Switch responses (sync or async via callback), converts them to XML, updates transaction state, and delivers responses to NPCI over REST. **NPCI is assumed to communicate with IMPS only via REST API (HTTP/HTTPS); the backend does not rely on TCP socket for NPCI in the production flow.**

2. **Reverse flow and callback:** Supports the reverse flow where the Switch sends requests to IMPS, IMPS converts to XML and forwards to NPCI over REST, and returns the NPCI response as ISO to the Switch; and accepts Switch response callbacks (e.g. RespPay, RespChkTxn) via REST or socket and delivers to NPCI over REST.

3. **Audit and transaction state:** Maintains a complete audit trail (message_audit_log) and transaction lifecycle (INIT → ISO_SENT → SUCCESS/FAILED) in PostgreSQL.

4. **Operational support:** Supports heartbeat (ReqHbt/RespHbt) and periodic Switch connectivity checks.

## 2.2 Project Scope

### Message types and APIs (REST – NPCI uses REST only)

Request/response pairs supported: ReqPay/RespPay, ReqChkTxn/RespChkTxn, ReqHbt/RespHbt, ReqListAccPvd/RespListAccPvd, ReqValAdd/RespValAdd. All are exposed under the base path /imps (e.g. /imps/reqpay/{txnId}, /imps/resppay/{txnId}). NPCI sends requests as XML (Content-Type: application/xml) to these endpoints. The backend returns an ACK (XML) in the HTTP response body. After processing, it sends the financial response (e.g. RespPay) to NPCI via REST (HTTP POST to the configured NPCI base URL). The Switch, when using REST, sends request/response as ISO 8583 (Content-Type: application/octet-stream) to the same /imps paths. Content-Type distinguishes XML (application/xml) vs ISO (application/octet-stream).

### Entry points

- **REST:** ImpsController and NpciController – POST /imps/{reqtype|resptype}/{txnId} for NPCI (XML) and for Switch (ISO). All paths under /imps. **NPCI communicates only via these REST endpoints (HTTP 8081 or HTTPS 8443); there is no TCP socket for NPCI in the primary design.**
- **Socket (Switch only):** SwitchSocketServer on 9086 (TCP) / 9446 (TLS) receives [4-byte length][ISO] from Switch for reverse flow and callback.

### Connection flows (REST for NPCI)

- **Request flow (NPCI-originated):** (1) NPCI sends HTTP POST with Req (XML) to IMPS; IMPS responds with ACK (XML) in the same HTTP response and closes the connection. (2) IMPS converts XML to ISO, resolves Switch address from institution_master, and sends Req (ISO) to the Switch (REST or socket); Switch returns Resp (ISO). (3) IMPS converts Resp to XML, updates transaction and audit, and sends Resp (XML) to NPCI over REST (new HTTP POST to NPCI).
- **Reverse flow (Switch-originated):** Switch → IMPS (Req ISO), IMPS → NPCI (Req XML) over REST, NPCI → IMPS (Resp XML) over REST, IMPS → Switch (Resp ISO).
- **Callback flow:** Switch sends Resp (ISO) to IMPS via POST /imps/resppay/{txnId} (or respchktxn, resphbt, etc.) or via socket; IMPS converts to XML, updates transaction, and sends to NPCI over REST.

![03_Request_Flow_Three_Connections](diagrams/03_Request_Flow_Three_Connections.png)  
*Figure 3: Request flow – NPCI → IMPS → Switch → IMPS → NPCI (REST)*

![06_OneLine_Request_Flow](diagrams/06_OneLine_Request_Flow.png)  
*Figure 6: One-line request flow*

### Ports and transport

- **Imps-backend:** HTTP 8081, HTTPS 8443 (ssl profile); Socket TCP 9086, Socket TLS 9446 (Switch reverse/callback). **NPCI connects to IMPS only on 8081/8443 (REST).**
- **mock_switch:** HTTP 8082, HTTPS 8444; Socket 9084, TLS 9444.
- **mock_npci:** HTTP 8083, HTTPS 8445 (REST only for testing).
- IMPS connects outbound to Switch (9084/9444) and to NPCI (configured NPCI URL) for response delivery. **For NPCI, REST (HTTP/HTTPS) is the only transport.**

![02_Ports_Integration_Table](diagrams/02_Ports_Integration_Table.png)  
*Figure 2: Ports and integration table*

![07_Services_Ports_Table](diagrams/07_Services_Ports_Table.png)  
*Figure 7: Services and ports summary*

### Routing and configuration

Institution-based routing via institution_master (switch_ip, switch_port for socket; REST base URL when routing.switch.rest.enabled). SwitchAddressResolver resolves by request_org_id (Head @orgId from NPCI XML) or IFSC. Configuration via application.yml and .env.

### Persistence and audit

- **imps_db (PostgreSQL):** transaction (txn_id, switch_status INIT/ISO_SENT/SUCCESS/FAILED, req/resp timestamps, resp_xml); message_audit_log (txn_id, stage, raw message – NPCI_*_XML_IN, SWITCH_*_ISO_OUT, SWITCH_*_ISO_IN, NPCI_*_XML_OUT); institution_master (IFSC, switch address, active, spoc details); account_type_mapping, xml_path_req_pay, response_xpath.
- **switch_db (PostgreSQL):** account_master, used by mock_switch for ReqPay debit/credit and ValAdd name enquiry.

### Heartbeat

Periodic (e.g. every 3 min): IMPS TCP pings each bank switch, updates institution_master.active; console shows UP/DOWN with spoc/email/phone/url. Manual ReqHbt from NPCI or Switch triggers full flow with DB check and logging.

### Supporting components

Mock Switch (ISO 8583, HTTP 8082/8444, Socket 9084/9444, switch_db) – receives Req from IMPS, returns Resp; can callback IMPS at /imps/resppay/{txnId} etc. Mock NPCI (XML, HTTP 8083/8445, REST only, stateless) – receives Resp from IMPS, returns ACK. Both are for testing only.

## 2.3 Project Methodology

The project followed an iterative development approach:

1. **Requirements and design:** Analysis of NPCI message formats (XML, REST-only for NPCI), ISO 8583 usage, and connection flows; high-level design (HLD) and low-level design (LLD); definition of three logical steps (NPCI→IMPS over REST, IMPS→Switch, IMPS→NPCI over REST), ACK behaviour, reverse flow, and callback flow.

2. **Implementation:** Spring Boot application with clear layering. Entry points: ImpsController, NpciController (REST for NPCI and Switch); SwitchSocketServer (9086/9446) for Switch. Orchestration: ImpsInboundService (extract msgId/txnId, validate, send ACK, dispatch to Req* services). Processing: ReqPayService, ReqChkTxnService, ReqHbtService, ReqValAddService, ReqListAccPvdService; RespPayService, RespChkTxnService, etc.; XmlToIsoConverter, IsoToXmlConverter; SwitchAddressResolver (institution_master); ISwitchClient (RestSwitchClient, SocketSwitchClient), NpciRestClient for all NPCI communication. Persistence: TransactionService, MessageAuditService. Exception handling: GlobalExceptionHandler. Config: application.yml, profiles, .env.

3. **Testing:** Unit and integration testing using Mock Switch and Mock NPCI; Postman collections for REST APIs; validation of ACK-first flow, request flow, reverse flow, and callback flow. **NPCI flow tested exclusively over REST.**

4. **Documentation:** Architecture (HLD, Project Connections, IMPS Flow Chart), LLD, API documentation, database schema, integration guides, security, configuration, deployment, runbook, code structure in docs folder.

![05_Internal_Architecture](diagrams/05_Internal_Architecture.png)  
*Figure 5: Internal architecture – entry points, services, converters, persistence*

## 2.4 Project Timeline

The project was executed over the internship period (December 15, 2025 to June 13, 2026). Development included: requirements and HLD/LLD; implementation of REST entry points for NPCI (primary) and Switch; implementation of request flow (NPCI → IMPS → Switch) and response delivery (IMPS → NPCI) over REST; implementation of reverse flow and Switch-to-IMPS callback; converters (XmlToIsoConverter, IsoToXmlConverter), routing (SwitchAddressResolver, institution_master), persistence and audit; heartbeat and Switch connectivity check; SSL/TLS support; Mock Switch and Mock NPCI; and documentation and testing. Deliverables: working Imps-backend with all flows, transaction and message_audit_log persistence, Postman collection for REST APIs, SQL schemas (imps_full_schema.sql, switch_full_schema.sql), and full documentation.

## 2.5 Technologies Used

| Layer | Technology |
|-------|-------------|
| Runtime | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Build | Maven 3.6+ |
| Database | PostgreSQL (imps_db, switch_db) |
| Persistence | Spring Data JPA (Hibernate) |
| Message formats | NPCI XML, ISO 8583 (jPOS 2.1.7) |
| Transport | REST (HTTP/HTTPS) for NPCI; REST or socket for Switch as configured |
| Configuration | application.yml, .env (per project) |
| Testing / tools | Postman, Mock Switch, Mock NPCI |

Key libraries: Lombok, Jackson, PostgreSQL driver, jPOS for ISO 8583 pack/unpack, Spring RestTemplate. SSL/TLS via keystore/truststore for HTTPS and TLS sockets. Certificates can be generated via project certs.

## 2.6 ISO 8583 Standard and Payment Message Format

### Introduction to ISO 8583

ISO 8583 is an international standard (ISO/IEC 8583) for systems that exchange electronic financial transaction card-originated messages. It is widely used by switches, payment networks, and banks worldwide to carry payment instructions and responses. The standard ensures interoperability between different vendors and banks and provides a common language for financial messaging.

### Purpose and role in this project

The standard defines a common message format so that different systems (e.g. NPCI, bank switches, acquirers) can interoperate. In IMPS, the Bank Switch typically speaks ISO 8583; NPCI uses its own XML schema and **communicates with the IMPS Backend only over REST**. The IMPS Backend's role is to convert between NPCI XML and ISO 8583 so that NPCI can remain on XML over REST while the Switch continues to use ISO 8583. The backend uses the jPOS library (version 2.1.7) to pack and unpack ISO 8583 messages.

### Overall message structure

An ISO 8583 message consists of three main parts:

**1. Message Type Indicator (MTI)**  
The MTI is a 4-digit numeric code that identifies the message class and function. The first digit indicates the message class (e.g. 0 = reserved, 2 = financial, 8 = network management); the remaining digits indicate the message function (request vs response) and version. Examples used in this project:
- **02xx** – Financial messages (e.g. 0200 = financial request, 0210 = financial response).
- **08xx** – Network management (e.g. 0800 = network request, 0810 = network response).

**2. Bitmaps**  
One or more bitmaps indicate which data elements (DEs) are present in the message. Each bit corresponds to a DE number (bit 1 = DE 1, bit 2 = DE 2, and so on). If the bit is set to 1, that DE is present. The primary bitmap typically covers DEs 1–64; a secondary bitmap can extend to DEs 65–128. The packager (e.g. ImpsIsoPackager) defines which DEs are used and their layout for compatibility with the Switch.

**3. Data elements (DEs)**  
Each data element is a numbered field with a defined format (fixed or variable length, numeric, alphanumeric, or binary). The format and length of each DE are defined by the standard or by a private/country variant. The IMPS Backend uses a custom packager that defines which DEs are used and their lengths for the Switch.

### Key data elements used in this project

| DE | Description | Usage in IMPS |
|----|-------------|----------------|
| DE 2 | Primary account number (PAN) or identifier | Payer/payee account or identifier |
| DE 3 | Processing code | 400000 fund transfer, 380000 balance/status, 310000 name enquiry, 320000 list account provider |
| DE 4 | Amount, transaction | Transaction amount (numeric) |
| DE 11 | System trace audit number (STAN) | Unique per transaction |
| DE 12 | Local transaction time | HHMMSS |
| DE 13 | Local transaction date | MMDD |
| DE 24 | Function code | e.g. 831 for network management (heartbeat) |
| DE 37 | Retrieval reference number (RRN) | Transaction reference |
| DE 39 | Response code | 00 = approved, 51 = insufficient funds, 14 = invalid card/account, 96 = system malfunction |
| DE 48 | Optional private data | Status or additional data in network messages |
| DE 120 | Private additional data | Custom data; in this project often carries txn_id linking ISO to NPCI XML |

Additional DEs may be used for payer/payee account data, IFSC, and other IMPS-specific fields as per the project's XML-to-ISO mapping.

### Message types and MTI in this project

- **0200 – Financial request:** Used for ReqPay (fund transfer), ReqChkTxn (transaction status), ReqValAdd (name enquiry), ReqListAccPvd (list account provider). DE 3 (processing code) distinguishes the subtype (e.g. 40 = fund transfer, 38 = balance/status, 31 = name enquiry, 32 = list account provider).
- **0210 – Financial response:** Used for RespPay, RespChkTxn, RespValAdd, RespListAccPvd. Same structure as the request plus DE 39 (response code).
- **0800 – Network management request:** Used for ReqHbt (heartbeat). DE 24 (function code) may carry 831 or similar for network management.
- **0810 – Network management response:** Used for RespHbt. DE 39 and DE 48 may carry status and optional data.

### Format of payment messages (detailed)

**ReqPay (payment request)**  
MTI 0200. DE 3 = 400000 (or equivalent for fund transfer). DE 4 = amount. DE 11 = STAN. DE 12, DE 13 = time and date. Payer and payee account/identifier fields as per mapping. DE 120 (or project-specific DE) = txn_id from NPCI. The XmlToIsoConverter builds this from the NPCI ReqPay XML (payer, payee, amount, txnId, etc.) using the project's field mapping.

**RespPay (payment response)**  
MTI 0210. Same structure as the request plus DE 39 = response code (00 = success, 51 = insufficient funds, 14 = invalid account, 96 = error). The Switch returns this; the IsoToXmlConverter unpacks it and maps the required fields into NPCI RespPay XML, which is then sent to NPCI over REST.

**ReqChkTxn / RespChkTxn**  
MTI 0200/0210 with DE 3 indicating transaction status enquiry (e.g. 38). DE 120 or similar carries the original txn_id for which status is requested.

**ReqValAdd / RespValAdd**  
MTI 0200/0210 with DE 3 indicating name enquiry (e.g. 31). Used to resolve account number to beneficiary name.

**ReqListAccPvd / RespListAccPvd**  
MTI 0200/0210 with DE 3 indicating list account provider (e.g. 32). Used to list account providers for a given institution.

**ReqHbt / RespHbt**  
MTI 0800/0810. Network management; DE 24 (e.g. 831) and DE 39 (result). Used for connectivity check between IMPS and Switch.

### How the backend uses ISO 8583

The XmlToIsoConverter builds an ISOMsg (jPOS object) from the NPCI XML: it sets MTI, then sets each DE (3, 4, 11, 12, 13, 37, 120, etc.) from the parsed XML. The message is then packed to binary using the project's packager and sent to the Switch (REST as application/octet-stream or socket as [4-byte length][binary]). The IsoToXmlConverter receives the response binary, unpacks it into an ISOMsg, reads MTI and DEs (especially DE 39, amount, and any custom DEs), and builds the corresponding NPCI response XML. That response XML is delivered to NPCI over REST. This ensures that payment messages between IMPS and the Switch conform to the ISO 8583 format expected by the bank's switch, while the backend talks to NPCI only in XML over REST.

![04_Transaction_Status_Flow](diagrams/04_Transaction_Status_Flow.png)  
*Figure 4: Transaction status flow – INIT → ISO_SENT → SUCCESS/FAILED*

## 2.7 Future Scope

Possible enhancements: additional message types or NPCI spec updates; performance and load testing; enhanced monitoring (health, latency, error rates) and alerting; multi-region or high-availability deployment; extended observability (metrics, distributed tracing). Production go-live would involve UAT with bank/NPCI integration, security review, runbooks, and operational handover.

## 2.8 Diagrams

All diagrams are taken from **docs/diagrams/** and are placed in their specific positions in the report as follows:

| Figure | Diagram file | Position in report |
|--------|--------------|--------------------|
| Figure 1 | 01_System_Architecture.png | **Chapter 1** – after section 1.2 Overview |
| Figure 2 | 02_Ports_Integration_Table.png | **Chapter 2** – after "Ports and transport" (in 2.2 Project Scope) |
| Figure 3 | 03_Request_Flow_Three_Connections.png | **Chapter 2** – after "Connection flows" (in 2.2 Project Scope) |
| Figure 4 | 04_Transaction_Status_Flow.png | **Chapter 2** – after section 2.6 ISO 8583 (end of "How the backend uses ISO 8583") |
| Figure 5 | 05_Internal_Architecture.png | **Chapter 2** – after section 2.3 Project Methodology |
| Figure 6 | 06_OneLine_Request_Flow.png | **Chapter 2** – after "Connection flows" (with Figure 3, in 2.2 Project Scope) |
| Figure 7 | 07_Services_Ports_Table.png | **Chapter 2** – after "Ports and transport" (with Figure 2, in 2.2 Project Scope) |
| Full project | FULL_PROJECT_ARCHITECTURE.png | **Chapter 2** – below (summary diagram for section 2.8) |

![FULL_PROJECT_ARCHITECTURE](diagrams/FULL_PROJECT_ARCHITECTURE.png)  
*Figure: Full project architecture*

---

# Chapter 3  
# CONCLUSION  

## 3.1 Learnings

**Technical:** Hands-on experience with Spring Boot (REST APIs, async processing, profiles, configuration), financial messaging (NPCI XML and ISO 8583 via jPOS), TCP Socket programming where applicable for the Switch (length-prefixed framing, TLS), and JPA-based persistence (transaction and audit design). Understanding of institution-based routing (SwitchAddressResolver, institution_master), duplicate transaction handling, reverse and callback flows, and structured exception handling in a payment context. **NPCI communication exclusively over REST in the production design.**

**Process and tools:** Importance of clear documentation (HLD, LLD, API, flows), use of mock services (Mock Switch, Mock NPCI) and Postman for integration testing, and version control and project structure in a multi-module codebase (Imps-backend, mock_switch, mock_npci).

## 3.2 Conclusion

The IMPS Backend Transaction Processing System project successfully delivered a complete middleware that bridges NPCI (XML over REST only) and the Bank Switch (ISO 8583). It supports the full request flow (NPCI → IMPS → Switch) with ACK-then-close behaviour over REST, the reverse flow (Switch → IMPS → NPCI), and the Switch-to-IMPS callback for asynchronous responses. **NPCI communicates with IMPS only via REST API (HTTP/HTTPS).** The system provides institution-based dynamic routing, validation, duplicate detection, transaction state management (INIT → ISO_SENT → SUCCESS/FAILED), and full message audit in PostgreSQL. Heartbeat and Switch connectivity checks are included. The implementation is documented and tested with Mock Switch and Mock NPCI. The internship provided valuable exposure to payment systems, enterprise development practices, and the Hitachi work environment.

---

# Chapter 4  
# REFERENCES  

1. NPCI IMPS technical specifications (message formats and API behaviour).
2. ISO 8583 – Financial transaction card-originated messages (ISO/IEC 8583).
3. Spring Boot 3.x documentation – https://spring.io/projects/spring-boot
4. Spring Data JPA reference – https://spring.io/projects/spring-data-jpa
5. jPOS 2.1.7 – ISO 8583 message handling – https://jpos.org/
6. Project documentation (E:\Hitachi_Project\docs) – Architecture (HLD, Project Connections, IMPS Flow Chart), LLD, API, Database, Integration, Security, Configuration, Deployment, Runbook, Code Structure.
7. PostgreSQL documentation – https://www.postgresql.org/docs/

---

*End of Report*
