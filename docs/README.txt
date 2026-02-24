# IMPS Project Documentation

All documentation is organized in numbered folders. Use this index to navigate.

Environment: Each project (Imps-backend, mock_npci, mock_switch) uses .env for config.
Copy .env.example to .env in each project folder. Default profile: prod,ssl.

---

## Folder Structure (01–13)

| # | Folder | Contents |
|---|--------|----------|
| — | [diagrams](diagrams/) | All project diagram images (PNG); keep diagrams here, not in repo root |
| **01** | [01_Architecture](01_Architecture/) | HLD, Deployment Architecture, Project Connections, IMPS Flow Chart |
| **02** | [02_Low_Level_Design](02_Low_Level_Design/) | Module Design, Flow Diagrams, Implementation Plan, Common Code Specs |
| **03** | [03_API_Documentation](03_API_Documentation/) | API Endpoints, Sample Requests, IMPS_Req_API_Bodies, NPCI_IMPS_Message_Formats, Error Codes |
| **04** | [04_Database](04_Database/) | Schema Reference |
| **05** | [05_Integration](05_Integration/) | NPCI XML Spec, XML-to-ISO Mapping, SOCKET_GUIDE |
| **06** | [06_Security](06_Security/) | TLS Configuration, Certificate Management |
| **07** | [07_Configuration](07_Configuration/) | application.yml Reference, Port Config |
| **08** | [08_Deployment_and_DevOps](08_Deployment_and_DevOps/) | Deployment Guide, Testing Guide |
| **09** | [09_Error_and_Exception_Handling](09_Error_and_Exception_Handling/) | Error Code Definitions |
| **10** | [10_Runbook](10_Runbook/) | Production Runbook, Troubleshooting |
| **11** | [11_Code_Structure](11_Code_Structure/) | Package Structure, Code Review Findings |

---

## Quick Links

| Document | Purpose |
|----------|---------|
| [HLD_System_Architecture](01_Architecture/HLD_System_Architecture.txt) | High-level design, tech stack |
| [Project_Connections](01_Architecture/Project_Connections.txt) | Ports, DB, connection flow |
| [IMPS_Flow_Chart](01_Architecture/IMPS_Flow_Chart.txt) | End-to-end flow diagrams |
| [diagrams](diagrams/) | All diagram images (PNG) – single location for diagrams |
| [FULL_PROJECT_DIAGRAM_REFERENCE](01_Architecture/FULL_PROJECT_DIAGRAM_REFERENCE.txt) | Full project architecture diagram reference |
| [SOCKET_GUIDE](05_Integration/SOCKET_GUIDE.txt) | Socket protocol, PowerShell, SSL/TLS |
| [Testing_Guide](08_Deployment_and_DevOps/Testing_Guide.txt) | HTTP/HTTPS/TCP/TLS testing |
| [Troubleshooting_Guide](10_Runbook/Troubleshooting_Guide.txt) | Common issues and fixes |
| [Code_Review_Findings](11_Code_Structure/Code_Review_Findings.txt) | Code review findings and recommendations |
| [IMPS_BACKEND_CODE_EXPLANATION](IMPS_BACKEND_CODE_EXPLANATION.txt) | In-detail code explanation and data flow |
