# Diagrams – IMPS Project

All project diagram images are kept in this folder: **E:\Hitachi_Project\docs\diagrams**

Do not store diagrams in the repository root or in other locations (e.g. assets outside docs). Keep everything here for a single source of truth.

---

## 7 report diagrams (PNG – direct download)

Use these in the final report. All in **E:\Hitachi_Project\docs\diagrams\** (same style as "IMPS Backend - Full Project Architecture (Detailed)").

| # | Filename | Description |
|---|----------|-------------|
| 1 | **01_System_Architecture.png** | High-level system architecture – NPCI, IMPS Backend, mock_switch, mock_npci; ports and formats |
| 2 | **02_Ports_Integration_Table.png** | Ports and integration table (NPCI→IMPS, IMPS→Switch, IMPS→NPCI, Switch→IMPS) |
| 3 | **03_Request_Flow_Three_Connections.png** | Request flow – three connections (ACK-first, Conn 1/2/3 with steps) |
| 4 | **04_Transaction_Status_Flow.png** | Transaction status flow – INIT → ISO_SENT → SUCCESS/FAILED |
| 5 | **05_Internal_Architecture.png** | Internal architecture – entry points, orchestration, services, converters, routing/clients, persistence |
| 6 | **06_OneLine_Request_Flow.png** | End-to-end one-line request flow (NPCI → IMPS → Switch → IMPS → NPCI) |
| 7 | **07_Services_Ports_Table.png** | Services and ports summary table (Imps-backend, mock_switch, mock_npci) |

---

## Other diagram files

| Diagram | Description |
|---------|-------------|
| IMPS_Flow_Chart_Diagram.png | Flow from IMPS_Flow_Chart.txt – high-level components, socket ACK-first flow |
| Project_Connections_Diagram.png | Ports, services, connection flow from Project_Connections.txt |
| Project_Connections_Request_Flow_Diagram.png | Three connections (NPCI→IMPS→Switch) with stages |
| FULL_PROJECT_ARCHITECTURE.png | Full project diagram – system context, connections, internal IMPS architecture, databases |

When you generate or add new diagrams, save them under **docs/diagrams/** and update this README or the relevant architecture doc (e.g. FULL_PROJECT_DIAGRAM_REFERENCE.txt) with the filename.

---

## Reference docs

- [01_Architecture/IMPS_Flow_Chart.txt](../01_Architecture/IMPS_Flow_Chart.txt) – Flow chart source
- [01_Architecture/Project_Connections.txt](../01_Architecture/Project_Connections.txt) – Connections source
- [01_Architecture/FULL_PROJECT_DIAGRAM_REFERENCE.txt](../01_Architecture/FULL_PROJECT_DIAGRAM_REFERENCE.txt) – Full diagram reference
