# IMPS project documentation

## Quick links

| Document | Purpose |
|----------|---------|
| [PROJECT_CONNECTIONS.md](PROJECT_CONNECTIONS.md) | Ports, databases, config, socket/HTTP flow |
| [TESTING_GUIDE.md](TESTING_GUIDE.md) | HTTP + TCP + TLS testing (Postman, curl, PowerShell) |
| [socket/SOCKET_GUIDE.md](socket/SOCKET_GUIDE.md) | Socket protocol, PowerShell copy-paste APIs, TLS, troubleshooting |
| [socket/SOCKET_SSL_TLS.md](socket/SOCKET_SSL_TLS.md) | SSL/TLS keytool, OpenSSL, firewall, keystore paths |

## By folder

### specs/
- **NPCI_IMPS_Message_Formats.md** – Req/Resp XML structures (authoritative)
- **IMPS_Req_API_Bodies.md** – Sample request bodies, test accounts (Rugved, Chetan, Sajid, Madhav)
- **IMPS_Common_Code_Technical_Specifications_Appendix_Rules.md** – Common code and rules

### guides/
- **IMPS_Flow_Chart.md** – End-to-end flow diagrams, ACK flow
- **SWITCH_CHECK_DB_LOGGING_GUIDE.md** – Switch check (SWITCH_CHECK) logging to transaction and message_audit_log

### implementation/
- **IMPLEMENTATION_PLAN_NPCI_COMPLIANT_FLOW.md** – NPCI-compliant socket flow (phases A–E)
- **IMPLEMENTATION_PLAN_SSL_TLS.md** – SSL/TLS implementation plan (phases A–G)
