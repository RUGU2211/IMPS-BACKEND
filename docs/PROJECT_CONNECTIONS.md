# Project Connections

Two databases: **imps_db** (IMPS Backend) and **switch_db** (Mock Switch).

## Ports and roles

| Service            | HTTP Port | HTTPS Port | Socket TCP | Socket TLS | Role |
|--------------------|-----------|------------|------------|------------|------|
| **Imps-backend**   | 8081 | 8443 (profile `ssl`) | 9083 | 9443 | NPCI XML ↔ Switch ISO; socket server; heartbeat; imps_db |
| **mock_switch** | 8082 | 8444 (profile `ssl`) | 9084 | 9444 | Mock Switch (ISO 8583); switch_db |
| **mock_npci** | 8083 | 8445 (profile `ssl`) | 9085 | 9445 | Mock NPCI (XML); stateless |

**Connection matrix:** Use **HTTP** (8081) or **HTTPS** (8443) for REST; use **TCP** (9083) or **TLS** (9443) for socket. Same pattern for Switch (9084/9444) and NPCI Mock (9085/9445). Postman: see [POSTMAN.md](POSTMAN.md). Socket testing: see [socket/SOCKET_GUIDE.md](socket/SOCKET_GUIDE.md) and [TESTING_GUIDE.md](TESTING_GUIDE.md).

**SSL/TLS:** When `ssl.enabled: true`, IMPS socket server listens on **9443** (TCP on 9083). When `npci.socket.ssl-enabled: true`, IMPS connects to NPCI on 9445. When `routing.switch.socket.ssl-enabled: true`, IMPS connects to Switch on 9444. For REST over HTTPS use `--spring.profiles.active=ssl` (server port **8443**).

## Database: imps_db (PostgreSQL)

- **URL:** `jdbc:postgresql://localhost:5432/imps_db`
- **Used by:** Imps-backend only

### Tables

| Table                 | Purpose |
|-----------------------|---------|
| transaction           | IMPS transaction state (INIT → ISO_SENT → SUCCESS/FAILED) |
| message_audit_log     | 4-stage audit per flow (NPCI_*_XML_IN, SWITCH_*_ISO_OUT/IN, NPCI_*_XML_OUT). All stages use txnId. |
| institution_master    | IMPS validation (IFSC, routing, ListAccPvd). Columns: url, spoc_name, spoc_email, spoc_phone, last_modified_ts (auto-updated). |
| account_type_mapping  | Account type ↔ ISO code |
| xml_path_req_pay      | ReqPay XPath config |
| response_xpath        | Response XPath config |

**IMPS-only access:** Only Imps-backend writes to transaction and message_audit_log. mock_switch and mock_npci do not use imps_db.

**Full schema:** run `sql/imps_full_schema.sql` from project root.

## Database: switch_db (PostgreSQL)

- **URL:** `jdbc:postgresql://localhost:5432/switch_db`
- **Used by:** mock_switch only

### Tables

| Table             | Purpose |
|-------------------|---------|
| account_master    | ValAdd name enquiry; ReqPay debit/credit; balance updates. Matches [IMPS_Req_API_Bodies](specs/IMPS_Req_API_Bodies.md) (Rugved, Chetan, Sajid, Madhav). |

**Full schema:** run `sql/switch_full_schema.sql` from project root. IFSC codes must match institution_master.ifsc_code in imps_db (HDFC0000001, ICIC0000001, SBIN0000001).

## Socket flow (default)

| Connection | Format | Plain Port | SSL Port |
|------------|--------|------------|----------|
| **NPCI → IMPS** | [4 bytes][XML] | 9083 | 9443 |
| **Switch → IMPS (reverse)** | [4 bytes][ISO] | 9086 | 9446 |
| **IMPS → Switch** | [4 bytes][ISO] | 9084 | 9444 |
| **IMPS → NPCI** | [4 bytes][XML] | 9085 | 9445 (when `npci.compliant-flow: true`) |

**Reverse flow (Switch → IMPS):** Same connection pattern as NPCI→IMPS. Switch connects to IMPS on 9086 (TCP) or 9446 (TLS), sends `[4-byte length big-endian][ISO]`, IMPS responds with `[4 bytes][ISO]`. Same flow for TCP/TLS as NPCI→IMPS.

See [socket/SOCKET_SSL_TLS.md](socket/SOCKET_SSL_TLS.md) for SSL setup.

**npci.compliant-flow: true (default):** NPCI-compliant socket flow.
- Phase 1: NPCI → IMPS Req → IMPS ACK (same socket) → NPCI closes. Client reads **once** (ACK).
- Phase 3: IMPS opens NEW connection to mock_npci:9085 → Resp → mock_npci ACK → IMPS closes.

**npci.compliant-flow: false:** Legacy same-connection flow. Client reads **twice** (ACK, then Resp).

**Message types:** ReqPay, ReqChkTxn, ReqValAdd, ReqHbt, ReqListAccPvd (and Resp*). See [socket/SOCKET_GUIDE.md](socket/SOCKET_GUIDE.md).

## Connection types (TCP, TLS, HTTP, HTTPS)

| Type | IMPS (NPCI) | IMPS (Switch reverse) | How to use |
|------|-------------|------------------------|------------|
| **HTTP** | Port 8081 | 8081 | Postman / curl: `http://localhost:8081/imps/...` |
| **HTTPS** | Port 8443 | 8443 | Run with `--spring.profiles.active=ssl`; Postman: `https://localhost:8443` |
| **TCP (socket)** | Port 9083 [4 bytes][XML] | Port 9086 [4 bytes][ISO] | Same framing; NPCI uses XML, Switch uses ISO. See SOCKET_GUIDE.md |
| **TLS (socket)** | Port 9443 | Port 9446 | Same as TCP but with TLS when `ssl.enabled: true` |

**Reverse flow (Switch → IMPS):** Same connection pattern as NPCI→IMPS—`[4 bytes][ISO]` on port 9086 (TCP) or 9446 (TLS). REST `https://{ip}:{port}/imps/{reqtype}/{txn_id}` with binary body is optional; socket is the primary connection type.

## HTTP flow (optional)

```
NPCI (mock 8083)  ←→  Imps-backend (8081)  ←→  Mock Switch (8082)
     XML                    XML ↔ ISO                   ISO
```

- **Imps-backend** calls **mock_npci** at `http://localhost:8083` (responses to NPCI).
- **Imps-backend** calls **mock_switch** at `http://localhost:8082` when REST mode (`routing.switch.rest.enabled: true`).
- **mock_switch** calls **Imps-backend** at `http://localhost:8081` for async responses (REST mode).
- **IMPS** TCP pings each bank switch every 3 min; console shows UP/DOWN banks with contact details. No auto ReqHbt from NPCI or Switch. Manual ReqHbt (socket, HTTP, SSL) triggers full flow with DB logging.

## Run with .env

Each app reads config from the environment. Put a **`.env`** in `Imps-backend/`, `mock_switch/`, or `mock_npci/` (see **[docs/sample-env](sample-env)**) and load it before starting:

- **Linux/Mac:** `cd <app-dir> && set -a && source .env && set +a && mvn spring-boot:run`
- **Windows PowerShell:** `cd <app-dir>; Get-Content .env | ForEach-Object { if ($_ -match '^\s*([^#][^=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process') } }; mvn spring-boot:run`

Add `-Dspring-boot.run.profiles=ssl` for HTTPS/TLS ports. See [TESTING_GUIDE.md](TESTING_GUIDE.md) for full run commands.

## Config summary

- **Imps-backend** `application.yml`: `socket.*`, `npci.compliant-flow`, `npci.socket.*` (host, port), `routing.switch.socket.*`, `routing.switch.rest.*`, `imps.*`
- **mock_npci** `application.yml`: `socket.*`
- **mock_switch** `application.yml`: `socket.*`, `imps.imps_ip`, `imps.imps_port`, `mock.*`
- **mock_npci** `application.yml`: server port, socket port (no DB).
