# IMPS-BACKEND

Hitachi IMPS project: IMPS Backend, Mock Switch, and NPCI Mock Client.

## Structure

- **Imps-backend** – IMPS backend (NPCI XML ↔ Switch ISO 8583), HTTP 8081, Socket 9083. All APIs under `/imps`. Only IMPS writes to `transaction` and `message_audit_log` in imps_db.
- **mock_switch** – Mock Switch (ISO 8583, auto responses), HTTP 8082, Socket 9084. Uses switch_db (account_master only).
- **mock_npci** – Mock NPCI (XML ACK), HTTP 8083, Socket 9085. Stateless, no database.
- **docs/** – All documentation (connections, socket guide, SSL, specs, implementation plans).
- **sql/** – Database schemas (imps_full_schema.sql, switch_full_schema.sql).
- **postman/** – Postman collection for REST APIs.
- **certs/** – SSL keystores (run `certs/generate-certs.bat` to create).

## Ports and connections

| Service       | HTTP | Socket | Database  |
|---------------|------|--------|-----------|
| Imps-backend  | 8081 | 9083   | imps_db   |
| mock_switch   | 8082 | 9084   | switch_db |
| mock_npci     | 8083 | 9085   | none      |

See **[docs/PROJECT_CONNECTIONS.md](docs/PROJECT_CONNECTIONS.md)** for full connection details and SSL ports.

## Heartbeat flows

- **IMPS switch check (every 3 min):** IMPS TCP pings each bank switch, updates institution_master.active. Console shows UP/DOWN banks with contact details (spoc, email, phone, url). No auto ReqHbt from NPCI or Switch.
- **Manual ReqHbt (socket, HTTP, SSL):** When NPCI or Switch sends ReqHbt manually, IMPS runs full flow: DB check, response, log to transaction + message_audit_log.

## Socket flow (npci.compliant-flow: true, default)

- **Phase 1:** NPCI → IMPS Req → IMPS ACK (same socket) → NPCI closes. Client reads **once** (ACK).
- **Phase 3:** IMPS opens NEW connection to mock_npci:9085 → Resp → mock_npci ACK → IMPS closes.

Set `npci.compliant-flow: false` for legacy same-connection flow (client reads twice).

## Documentation

| Document | Purpose |
|----------|---------|
| [docs/PROJECT_CONNECTIONS.md](docs/PROJECT_CONNECTIONS.md) | Ports, databases, config |
| [docs/TESTING_GUIDE.md](docs/TESTING_GUIDE.md) | HTTP + TCP + TLS testing guide |
| [docs/socket/SOCKET_GUIDE.md](docs/socket/SOCKET_GUIDE.md) | Socket protocol, PowerShell APIs, troubleshooting |
| [docs/socket/SOCKET_SSL_TLS.md](docs/socket/SOCKET_SSL_TLS.md) | SSL/TLS keytool, OpenSSL, firewall |
| [docs/guides/IMPS_Flow_Chart.md](docs/guides/IMPS_Flow_Chart.md) | End-to-end flow diagrams |
| [docs/README.md](docs/README.md) | Full doc index (specs, implementation plans) |

## Console logging

Each project logs with clear prefixes:
- **Imps-backend:** `[IMPS]` – Req/Resp received, transaction created/updated, message_audit_log
- **mock_switch:** `[MOCK_SWITCH]` – Req* received, Resp* sent/forwarded
- **mock_npci:** `[MOCK_NPCI]` – Req*/Resp* received, ACK sent

## Database setup

Two databases (PostgreSQL). Run from project root:

```bash
psql -U postgres -c "CREATE DATABASE imps_db;"
psql -U postgres -c "CREATE DATABASE switch_db;"
psql -U postgres -d imps_db -f sql/imps_full_schema.sql
psql -U postgres -d switch_db -f sql/switch_full_schema.sql
```

Test data in schemas matches [docs/specs/IMPS_Req_API_Bodies.md](docs/specs/IMPS_Req_API_Bodies.md) (Rugved, Chetan, Sajid, Madhav accounts; BANK01/BANK02/BANK03 institutions).

## Run

Configuration is in **application.yml** (direct values). Edit paths in application.yml if your project location differs from `E:/Hitachi_Project`.

**Linux / Mac (Bash):**

```bash
# Terminal 1 – IMPS Backend
cd Imps-backend
mvn spring-boot:run

# Terminal 2 – Mock Switch
cd mock_switch
mvn spring-boot:run

# Terminal 3 – NPCI Mock (for compliant flow)
cd mock_npci
mvn spring-boot:run
```

**Windows (PowerShell):**

```powershell
# Terminal 1 – IMPS Backend
cd Imps-backend
mvn spring-boot:run

# Terminal 2 – Mock Switch
cd mock_switch
mvn spring-boot:run

# Terminal 3 – NPCI Mock
cd mock_npci
mvn spring-boot:run
```

**HTTPS (ssl profile):** Add `-Dspring-boot.run.profiles=ssl` to the end of the `mvn spring-boot:run` command.
