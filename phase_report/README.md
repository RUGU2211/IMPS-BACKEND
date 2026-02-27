# Phase Reports – IMPS Backend Transaction Processing System

This folder contains **three phase-specific reports** for the IMPS Backend project (Hitachi Internship, NMIMS MCA).

| File | Description |
|------|-------------|
| **Phase1_Report.md** | **Phase 1** – Foundation: NPCI→IMPS request flow, XML↔ISO conversion, routing, validation, persistence, REST, ACK-first flow. Scope limited to Phase 1 deliverables; aligned with `phases_ppt/Phase1_Presentation_Content.md`. |
| **Phase2_Report.md** | **Phase 2** – End-to-end response flow (IMPS→NPCI), SSL/TLS, performance & hardening. Builds on Phase 1; aligned with `phases_ppt/Phase2_Presentation_Content.md`. |
| **Phase3_Report.md** | **Phase 3** – **Full cumulative report**: Phase 1 + Phase 2 + Phase 3 work (UAT, go-live, monitoring, handover). Same content as the final project report in `docs/Final_Project_Report.md`, with diagram paths adjusted for this folder (`../docs/diagrams/`). |

**Phase breakdown (reference):**

- **Phase 1:** Imps-backend request path, REST, dynamic routing, persistence (imps_db), mock_switch & mock_npci for testing.
- **Phase 2:** Response delivery to NPCI, SSL/TLS, performance, resilience (timeouts, retries, BANK_DOWN).
- **Phase 3:** UAT, go-live readiness, monitoring, runbooks, handover; full project documentation.

All reports use the same formal structure (cover, certificate where applicable, TOC, chapters, references). Diagram paths in Phase 3 are relative to `phase_report` (e.g. `../docs/diagrams/01_System_Architecture.png`).
