# IMPS-BACKEND

Hitachi IMPS project: IMPS Backend, Mock Switch, and NPCI Mock Client.

## Structure

- **Imps-backend** – IMPS backend (NPCI XML ↔ Switch ISO 8583), port 8081
- **imps_mock_switch** – Mock Switch (ISO 8583, auto responses), port 8082
- **npci_mock_client** – NPCI Mock Client (XML ACK), port 8083, no database

## Ports and connections

| Service            | Port | Database   |
|--------------------|------|------------|
| Imps-backend       | 8081 | imps_db    |
| imps_mock_switch   | 8082 | imps_db    |
| npci_mock_client  | 8083 | none       |

See **[PROJECT_CONNECTIONS.md](PROJECT_CONNECTIONS.md)** for full connection details and HTTP flow.

## Database setup

Single database **imps_db** (PostgreSQL):

```bash
psql -U postgres -c "CREATE DATABASE imps_db;"
psql -U postgres -d imps_db -f imps_full_schema.sql
```

## Run

1. Start IMPS Backend: `cd Imps-backend && mvn spring-boot:run`
2. Start Mock Switch: `cd imps_mock_switch && mvn spring-boot:run`
3. (Optional) Start NPCI Mock Client: `cd npci_mock_client && mvn spring-boot:run`
