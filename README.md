# IMPS-BACKEND

Hitachi IMPS project: IMPS Backend, Mock Switch, and NPCI Mock Client.

## Structure

- **Imps-backend** – IMPS backend (NPCI XML ↔ Switch ISO 8583)
- **imps_mock_switch** – Mock Switch (auto/manual responses)
- **npci_mock_client** – NPCI Mock Client

## Ports

- IMPS Backend: 8081
- Mock Switch: 8082
- NPCI Mock Client: 8083

## Run

1. Start IMPS Backend: `cd Imps-backend && mvn spring-boot:run`
2. Start Mock Switch: `cd imps_mock_switch && mvn spring-boot:run`
3. (Optional) Start NPCI Mock Client: `cd npci_mock_client && mvn spring-boot:run`

Manual trigger (Mock Switch): set `mock.trigger.enabled=true` to enable `/trigger/*` endpoints.
