# Diagrams – IMPS Project (Report-Ready)

All diagram images for the project and the final report are kept in this folder:
**E:\Hitachi_Project\docs\diagrams**

Use only the filenames listed below. Do not store diagrams elsewhere.

---

## Naming rules – direct use in report

- **Use only these exact filenames** so the report (Final_Project_Report.md) can reference them directly.
- **Delete or rename** any file whose name looks auto-generated, for example:
  - Names containing: workspaceStorage, AppData_Roaming_Cursor, c__Users_
  - Long names with UUIDs (e.g. ...3fc6be1c-af25-4bc1-b49b-....png)
  - If you paste an image from Cursor/IDE and it saves with such a name, rename it to one of the report filenames below (e.g. 01_System_Architecture.png) or delete it.

---

## Report diagram index – use in Final_Project_Report.md

These are the **official diagram filenames** for the college report. Insert them at the places indicated in docs/Final_Project_Report.md.

| # | Filename | Figure | Exact position in Final_Project_Report.md |
|---|----------|--------|------------------------------------------|
| 1 | 01_System_Architecture.png | Figure 1 | Chapter 1 – after section **1.2 Overview** (last paragraph) |
| 2 | 02_Ports_Integration_Table.png | Figure 2 | Chapter 2 – after **Ports and transport** (in 2.2 Project Scope) |
| 3 | 03_Request_Flow_Three_Connections.png | Figure 3 | Chapter 2 – after **Connection flows** (in 2.2 Project Scope) |
| 4 | 04_Transaction_Status_Flow.png | Figure 4 | Chapter 2 – after **2.6 ISO 8583** (end of "How the backend uses ISO 8583") |
| 5 | 05_Internal_Architecture.png | Figure 5 | Chapter 2 – after **2.3 Project Methodology** (last bullet) |
| 6 | 06_OneLine_Request_Flow.png | Figure 6 | Chapter 2 – after **Connection flows** (with Figure 3, in 2.2) |
| 7 | 07_Services_Ports_Table.png | Figure 7 | Chapter 2 – after **Ports and transport** (with Figure 2, in 2.2) |
| – | FULL_PROJECT_ARCHITECTURE.png | Full project | Chapter 2 – **2.8 Diagrams** (summary at end of section) |

**Full project (optional):**

| Filename | Description | Where in report |
|----------|-------------|------------------|
| FULL_PROJECT_ARCHITECTURE.png | Full project – system context, connections, internal IMPS, databases | Chapter 2, Diagrams section (2.8) or appendix |

---

## Other diagram files (reference / older)

| Filename | Description |
|----------|-------------|
| IMPS_Flow_Chart_Diagram.png | Flow from IMPS_Flow_Chart – high-level components, ACK-first flow |
| Project_Connections_Diagram.png | Ports, services, connection flow from Project_Connections.txt |
| Project_Connections_Request_Flow_Diagram.png | Three connections (NPCI→IMPS→Switch) with stages |
| Phase1_IMPS_Flow_Chart.png | Phase 1 flow chart |

When adding new diagrams, save them under docs/diagrams/ with a **short, descriptive name** (e.g. 08_Deployment_Flow.png) and update this README.

---

## Reference docs

- docs/01_Architecture/IMPS_Flow_Chart.txt – Flow chart source
- docs/01_Architecture/Project_Connections.txt – Connections source
- docs/01_Architecture/FULL_PROJECT_DIAGRAM_REFERENCE.txt – Full diagram reference
