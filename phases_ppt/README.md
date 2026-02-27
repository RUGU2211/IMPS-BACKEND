# Phases PPT – IMPS Backend (Hitachi Internship)

This folder contains **presentation content for all three phases** of the IMPS Backend project. Content is copy-paste ready for PowerPoint or similar. **Focus is on Imps-backend;** mock_npci and mock_switch are covered briefly as supporting test projects.

---

## Files

| File | Description |
|------|-------------|
| **Phase1_Presentation_Content.md** | Phase 1 – Foundation: NPCI→IMPS request flow, XML↔ISO, routing, validation, persistence, REST. Imps-backend + mock_switch, mock_npci (testing). |
| **Phase2_Presentation_Content.md** | Phase 2 – End-to-end response flow (IMPS→NPCI), SSL/TLS, performance & security hardening. Imps-backend focus. |
| **Phase3_Presentation_Content.md** | Phase 3 – UAT, go-live readiness, monitoring, alerting, runbooks, handover. Imps-backend focus. |
| **All_Phases_Final_Presentation_Content.md** | **Combined presentation** – Phase 1 + Phase 2 + Phase 3 in one deck. Use this for the final/all-phases submission. Imps-backend is the core; mock_switch and mock_npci are supporting. |

---

## Project components (reference)

- **Imps-backend** – Core middleware (NPCI ↔ Bank Switch; XML ↔ ISO 8583; routing; persistence in imps_db).
- **mock_switch** – Simulates Bank Switch (ISO 8583, switch_db). For testing only.
- **mock_npci** – Simulates NPCI (XML). For testing only.

The main project repository also has **Phase1_Presentation_Content.md** at the root; the version in this folder is aligned with the three-phase structure and Imps-backend focus.
