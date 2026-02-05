# Socket-Based IMPS Flow — Implementation Plan

This document describes the plan to move from REST to **TCP socket** communication for NPCI ↔ IMPS and IMPS ↔ Switch, with **all configuration from application.yml** (no hardcoded URLs, ports, or hosts).

---

## 1. Dynamic Configuration — No Hardcoding

- **All** connection settings must come from **application.yml** (or profile-specific yml).
- **No** hardcoded:
  - Hosts / IPs (e.g. no `localhost`, `127.0.0.1` in code for UAT/prod)
  - Ports
  - URLs or base paths
- Any change (e.g. Switch URL, NPCI port, thread pool size) is done by **editing application.yml only**; no code change or rebuild required for config-only updates.

---

## 2. Target Architecture (Both Directions)

```
NPCI/Switch                    IMPS App                      Switch/NPCI
   |                               |                               |
   |  TCP Socket (IP:Port)         |  TCP Socket (IP:Port)         |
   |  -------------------------->  |  -------------------------->  |
   |  Request (XML/ISO)            |  Request (XML/ISO)             |
   |  <---------------------------  |  <---------------------------   |
   |  ACK / Response (same socket)  |  ACK / Response (same socket)  |
```

- **IMPS as server:** NPCI/Switch connect to IMPS (listen host/port from config).
- **IMPS as client:** IMPS connects to Switch/NPCI (host/port from config).
- **Persistent TCP:** IMPS–Switch communication uses **persistent** TCP connections; ACK and responses go on the **same open connection**.
- **Payload:** Same XML/ISO as today; only transport changes (socket framing added).

---

## 3. Configuration in application.yml

All socket and routing settings are read from **application.yml** (or environment/profiles). See **application.yml** under `routing.*` (current REST) and `socket.*` (future socket) for the config keys. Example structure (values are examples only):

- **Server (IMPS listens):**
  - Bind address (e.g. `0.0.0.0`), port(s) for NPCI and/or Switch inbound.
- **Client (IMPS connects out):**
  - NPCI: host, port (no localhost in UAT/prod).
  - Switch: host, port.
- **Threading:**
  - Acceptor threads, I/O pool size, processing pool size (all from yml).
- **Framing:**
  - Length-prefix size, encoding (e.g. UTF-8) from yml if needed.

Code must **only** read these via `@Value` or `@ConfigurationProperties`; no fallback to hardcoded defaults for prod/UAT.

---

## 4. Persistent TCP and Same-Connection ACK

- **IMPS–Switch:** Uses **persistent** TCP socket connections.
- **ACK and responses** are sent back on the **same** open connection (no new connection per message).
- **NPCI ↔ IMPS:** Same rule where NPCI/Switch keep connections open: ACK and response on the same connection.
- Connection reuse and timeouts (idle, read, etc.) should be configurable via application.yml.

---

## 5. Multi-Threading

- **Acceptor:** Dedicated thread(s) to accept new connections; config (e.g. count) from yml.
- **Per-connection I/O:** Each persistent connection handled by a worker (thread-per-connection or NIO pool); pool size from yml.
- **Processing pool:** Parsing, validation, ACK build, business logic run on a **separate thread pool** (size from yml); socket threads only do I/O.
- **Outbound (IMPS → Switch/NPCI):** Connection pool size and worker pool size from yml.
- **Ordering:** Per-connection message order preserved so ACK/response always go on the same connection in correct order.
- **Thread safety:** Shared state (connection registry, config, metrics) must be thread-safe.

---

## 6. Message Framing

- **Payload:** Same XML/ISO as today.
- **Framing:** Length-prefix (or agreed delimiter) for “one message” on the wire; length size/format configurable from yml if needed.
- **Encoding:** Typically UTF-8; from yml.

---

## 7. ACK Flow (Same Connection)

- **Inbound:** Accept → read message → process → send ACK/response on **same** connection → keep connection open for next message.
- **Outbound:** Use persistent connection → send request → read ACK/response on **same** connection → reuse connection.

---

## 8. Environment Rules (UAT / SWACAT / Prod)

- **Do not use** `localhost` / `127.0.0.1` for NPCI or Switch in UAT/prod; use **server IP or hostname** from application.yml.
- **Bind server to** `0.0.0.0` (from yml) so the process listens on all interfaces; port(s) from yml.
- **Firewall:** Ports used for socket server and client must be open as per environment.

---

## 9. Phased Rollout

1. **Define config schema:** Add all socket/routing/threading keys to application.yml (with placeholders or current REST equivalents).
2. **Refactor existing REST clients:** Ensure all URLs/ports/hosts are read from config (no hardcoding).
3. **Socket server (inbound):** Implement TCP server; bind address and ports from yml; read message → existing logic → write ACK on same connection.
4. **Socket client (outbound):** Implement persistent TCP client; host/port from yml; send/read on same connection.
5. **Multi-threading:** Wire pool sizes and acceptor count from yml; ensure same-connection semantics and thread safety.
6. **Testing:** Unit tests with in-memory sockets; integration tests with config-driven host/port.

---

## 10. One-Line Summary

**IMPS uses TCP sockets for NPCI↔IMPS and IMPS↔Switch; persistent connections; ACK and responses on the same connection; multi-threading with pool sizes from config; all URLs, hosts, ports, and threading parameters come from application.yml — no hardcoding.**
