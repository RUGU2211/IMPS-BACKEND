# Project Connections

All services use a single database **imps_db**. No other databases are used.

## Ports and roles

| Service            | Port | Role |
|--------------------|------|------|
| **Imps-backend**   | 8081 | NPCI XML ↔ Switch ISO; heartbeat scheduler; uses imps_db |
| **imps_mock_switch** | 8082 | Mock Switch (ISO 8583); receives from Backend, forwards responses; uses imps_db |
| **npci_mock_client** | 8083 | Mock NPCI (XML); no database, stateless |

## Database: imps_db (PostgreSQL)

- **URL:** `jdbc:postgresql://localhost:5432/imps_db`
- **Used by:** Imps-backend, imps_mock_switch
- **Not used by:** npci_mock_client

### Tables

| Table                 | Used by        | Purpose |
|-----------------------|----------------|---------|
| transaction           | Imps-backend   | IMPS transaction state |
| message_audit_log     | Imps-backend   | 4-stage audit per flow (npci_xml_in, switch_iso_out, switch_iso_in, npci_xml_out) |
| account_master        | Imps-backend   | Switch/NPCI validation (account-level) |
| institution_master     | Imps-backend   | IMPS validation (IFSC, routing, ListAccPvd) |
| account_type_mapping  | Imps-backend   | Account type ↔ ISO code |
| xml_path_req_pay      | Imps-backend   | ReqPay XPath config |
| response_xpath        | Imps-backend   | Response XPath config |
| audit_log             | imps_mock_switch | Mock Switch ISO message audit |

**Full schema (drop + create + seed):** run `imps_full_schema.sql` from project root.

## HTTP flow

```
NPCI (mock 8083)  ←→  Imps-backend (8081)  ←→  Mock Switch (8082)
     XML                    XML ↔ ISO                   ISO
```

- **Imps-backend** calls **npci_mock_client** at `http://localhost:8083` (responses to NPCI).
- **Imps-backend** calls **imps_mock_switch** at `http://localhost:8082` (ISO requests/responses).
- **imps_mock_switch** calls **Imps-backend** at `http://localhost:8081` (e.g. `/switch/resppay/2.1`) to post responses.

## Config summary

- **Imps-backend** `application.yml`: `routing.npci.base-url`, `routing.switch.base-url`, `imps.*`
- **imps_mock_switch** `application.yml`: `imps.backend.base-url`, `mock.*`
- **npci_mock_client** `application.yml`: server port only (no DB).
