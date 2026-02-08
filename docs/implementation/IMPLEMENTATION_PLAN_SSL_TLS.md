# IMPS Socket – SSL/TLS Implementation Plan

**Purpose:** Add one-way TLS and mutual TLS (mTLS) support for **all** IMPS socket connections. Covers both flows: **NPCI → IMPS → Switch** and **Switch → IMPS → NPCI**.

**Reference:** NPCI UAT/PROD use mTLS mandatory. Bank Switch UAT/PROD typically use TLS. Local dev can use plain TCP or one-way TLS.

**Usage:** See [SOCKET_SSL_TLS](../socket/SOCKET_SSL_TLS.md) for keytool, OpenSSL, and firewall commands. Config: `ssl.enabled`, `npci.socket.ssl-enabled`, `routing.switch.socket.ssl-enabled` in `application.yml`.

---

## 1. Full Flow Coverage

### Flow 1: NPCI → IMPS → Switch (forward)

| Step | Connection | IMPS Role | SSL Coverage |
|------|------------|-----------|--------------|
| 1 | NPCI → IMPS (inbound) | Server | ✅ NpciSocketServer (port 9083/9443) |
| 2 | IMPS → Switch (outbound) | Client | ✅ SocketSwitchClient (Switch presents cert) |
| 3 | Switch → IMPS (response) | Same socket as step 2 | ✅ Covered – response on TLS pipe |
| 4 | IMPS → NPCI (outbound) | Client | ✅ NpciSocketClient (port 9085/9445) |

### Flow 2: Switch → IMPS → NPCI (reverse)

| Step | Connection | IMPS Role | SSL Coverage |
|------|------------|-----------|--------------|
| 1 | Switch → IMPS (inbound) | **Socket:** N/A – Switch responds on same conn IMPS opened | ✅ Already covered by IMPS→Switch SSL |
| 1b | Switch → IMPS (REST mode) | **HTTP:** ImpsController 8081 | ⚠️ HTTPS = Spring Boot server.ssl (separate config) |
| 2 | IMPS → NPCI (outbound) | Client | ✅ NpciSocketClient |

**Important:** In **socket mode**, Switch does not open a connection to IMPS. IMPS opens to Switch; Switch responds on the same connection. So IMPS→Switch SSL covers both request and response. In **REST mode**, Switch POSTs to IMPS HTTP – use Spring Boot `server.ssl` for HTTPS.

---

## 2. What Changes When SSL Is Enabled

| Without SSL | With SSL |
|-------------|----------|
| `Laptop ── TCP ──▶ IMPS (IP:PORT)` | `Laptop ── TLS/TCP ──▶ IMPS (IP:PORT)` |
| Plain connection | Certificate verification |
| Client trusts IMPS (one-way) | With mTLS: IMPS also trusts client cert |

---

## 3. Scope of SSL in This Project

| Component | Direction | SSL Required? | Plan Phase |
|-----------|-----------|---------------|------------|
| **NpciSocketServer** (IMPS) | NPCI → IMPS (inbound) | ✅ Yes | Phase B |
| **NpciSocketClient** (IMPS) | IMPS → NPCI (outbound) | ✅ Yes | Phase C |
| **SocketSwitchClient** (IMPS) | IMPS → Switch (outbound) | ✅ Yes – bank UAT/PROD | Phase F (new) |
| Switch response on same conn | Switch → IMPS (response) | ✅ Same TLS pipe | Phase F |
| **REST: ImpsController 8081** | Switch → IMPS (REST mode) | ⚠️ HTTPS separate | Phase G (optional) |
| **mock_npci** | Receives from IMPS | Optional – for local mTLS testing | Phase D |

---

## 3. Environment Matrix

| Environment | TLS Mode | Notes |
|-------------|----------|-------|
| Local dev | Plain TCP or one-way TLS | `ssl.enabled: false` or one-way |
| Bank UAT | TLS (one-way or mTLS) | Per bank setup |
| NPCI UAT | **mTLS mandatory** | Both sides verify certs |
| PROD | **mTLS mandatory** | Both sides verify certs |

---

## 4. Case 1 – One-Way TLS (Simpler)

**Flow:** IMPS has server cert → Client verifies IMPS → Client does NOT send its own cert.

### 4.1 IMPS Server Config (application.yml)

```yaml
socket:
  enabled: true
  server:
    bind-host: 0.0.0.0
    npci:
      port: 9443   # Different from 9083 when SSL enabled

ssl:
  enabled: true
  key-store: imps-keystore.jks
  key-store-password: changeit
  key-alias: imps
  # trust-store not needed for one-way
  # client-auth: want (optional) or omit
```

**Keystore contains:** IMPS private key + IMPS public certificate.

### 4.2 Client Setup (Testing Laptop)

1. Export IMPS public cert:
   ```bash
   keytool -exportcert -alias imps -keystore imps-keystore.jks -file imps-public.cer
   ```
2. Import into client truststore:
   ```bash
   keytool -importcert -alias imps -file imps-public.cer -keystore client-truststore.jks
   ```
3. Test with OpenSSL: `openssl s_client -connect 192.168.1.38:9443`
4. Test with PowerShell (SslStream) – see Section 7.

---

## 5. Case 2 – Mutual TLS (mTLS, NPCI-Style)

**Flow:** IMPS has server cert + trusts client cert → Client has client cert + trusts IMPS cert → Both verify each other.

### 5.1 IMPS Server Config (application.yml)

```yaml
ssl:
  enabled: true
  key-store: imps-keystore.jks
  key-store-password: changeit
  key-alias: imps

  trust-store: imps-truststore.jks
  trust-store-password: changeit
  client-auth: need   # Require client certificate
```

**imps-keystore.jks:** IMPS private key + IMPS public cert.  
**imps-truststore.jks:** NPCI/client public cert(s) or CA cert(s).

### 5.2 Client Setup (NPCI / Testing Laptop)

- **client-keystore.jks:** Private key + client cert
- **client-truststore.jks:** Trust IMPS cert (or CA)
- Test: `openssl s_client -connect 192.168.1.38:9443 -cert client.crt -key client.key -CAfile imps-ca.crt`

---

## 6. Implementation Tasks (Phased)

### Phase A: Config and SslConfig

| Task | Description |
|------|-------------|
| A1 | Add `ssl` block to `application.yml` (enabled, key-store, key-store-password, key-alias, trust-store, trust-store-password, client-auth) |
| A2 | Create `SslConfig.java` – `@ConfigurationProperties(prefix = "ssl")` |
| A3 | Add conditional: when `ssl.enabled: true`, use port 9443 (or configurable `socket.server.npci.ssl-port`) |
| A4 | Support `client-auth`: `none`, `want`, `need` |

### Phase B: NpciSocketServer – SSL Server Socket

| Task | Description |
|------|-------------|
| B1 | Create `SSLServerSocket` when `ssl.enabled: true` instead of `ServerSocket` |
| B2 | Load KeyManagerFactory from key-store |
| B3 | Load TrustManagerFactory from trust-store (when mTLS) |
| B4 | Configure SSLContext, create SSLServerSocketFactory |
| B5 | Wrap `accept()` → return `SSLSocket`; handlers use SSLSocket's streams (same protocol: [4-byte][XML]) |
| B6 | Set `client-auth` on SSLServerSocket: setNeedClientAuth(true) when `client-auth: need` |
| B7 | Log client cert subject on connect (for audit) |

### Phase C: NpciSocketClient – Outbound SSL

| Task | Description |
|------|-------------|
| C1 | When IMPS connects to NPCI (Phase 3), use `SSLSocket` if NPCI expects TLS |
| C2 | Add `npci.socket.ssl-enabled`, `npci.socket.trust-store` (optional) for outbound |
| C3 | NpciSocketClient: use SSLContext with trust-store to connect to npci.socket.host:port |
| C4 | If mTLS outbound: add client key-store for IMPS client cert when calling NPCI |

### Phase D: Certificate and Keystore Setup

| Task | Description |
|------|-------------|
| D1 | Document keytool commands for generating IMPS keystore (self-signed for dev) |
| D2 | Document export/import for client trust |
| D3 | Add `src/main/resources/certs/` (gitignored) or document path for keystores |
| D4 | Provide sample `application-ssl.yml` or profile `ssl` |

### Phase E: Testing and Documentation

| Task | Description |
|------|-------------|
| E1 | Update SOCKET_GUIDE.md – add TLS PowerShell example (SslStream) |
| E2 | Add socket/SOCKET_SSL_TLS.md – OpenSSL commands, keytool, firewall |
| E3 | Update docs/PROJECT_CONNECTIONS.md – SSL ports, env matrix |
| E4 | Add firewall rule doc: `netsh advfirewall firewall add rule name="IMPS SSL Socket" dir=in action=allow protocol=TCP localport=9443` |

### Phase F: SocketSwitchClient – IMPS → Switch SSL (NEW)

| Task | Description |
|------|-------------|
| F1 | Add `routing.switch.socket.ssl-enabled`, `routing.switch.socket.trust-store`, `trust-store-password` |
| F2 | When `ssl-enabled: true`, use `SSLSocket` instead of `Socket` to connect to Switch |
| F3 | Load TrustManagerFactory from trust-store (trust Switch cert) |
| F4 | If mTLS: add `routing.switch.socket.key-store` for IMPS client cert when connecting to Switch |
| F5 | Protocol unchanged: [4-byte length][ISO] over TLS pipe – both Req and Resp on same SSLSocket |
| F6 | institution_master: optional `switch_ssl_enabled`, `switch_trust_store_path` per institution |

**Note:** Switch response (Switch → IMPS) travels on the same TLS connection IMPS opened. No separate server needed.

### Phase G: REST Mode – Switch → IMPS HTTPS (Optional)

| Task | Description |
|------|-------------|
| G1 | When using REST: Switch POSTs to IMPS `http://localhost:8081/imps/resppay/{txnId}` |
| G2 | For HTTPS: configure Spring Boot `server.ssl` (key-store, port 8443) |
| G3 | Update `routing.imps.backend-base-url` to `https://...` when SSL enabled |
| G4 | This is separate from socket SSL – different port, different config |

### Config for Switch Socket SSL (Phase F)

```yaml
routing:
  switch:
    socket:
      enabled: true
      host: localhost
      port: 9084
      ssl-enabled: true
      trust-store: switch-truststore.jks
      trust-store-password: changeit
      # For mTLS (IMPS presents client cert to Switch):
      # key-store: imps-client-keystore.jks
      # key-store-password: changeit
      # key-alias: imps-client
```

---

## 7. Code Changes Summary

| File | Change |
|------|--------|
| `application.yml` | Add `ssl` block; `routing.switch.socket.ssl-enabled`, trust-store; optional port overrides |
| `SslConfig.java` (new) | SSL properties binding (NPCI server, NPCI client, Switch client) |
| `NpciSocketServer.java` | Use SSLServerSocket when ssl.enabled; same handleConnection logic |
| `NpciSocketClient.java` | Use SSLSocket when npci.socket.ssl-enabled |
| `SocketSwitchClient.java` | Use SSLSocket when routing.switch.socket.ssl-enabled |
| `SocketConfig.java` | Optional ssl-port or port logic |
| `RoutingConfig.java` | Switch socket SSL config (trust-store, key-store for mTLS) |

---

## 8. PowerShell TLS Test (Conceptual)

```powershell
$client = New-Object System.Net.Sockets.TcpClient
$client.Connect("192.168.1.38", 9443)

$ssl = New-Object System.Net.Security.SslStream(
    $client.GetStream(), $false,
    ({ param($s,$c,$ch,$e) $true })  # Accept cert – dev only
)
$ssl.AuthenticateAsClient("IMPS")

# Then: [4-byte length][XML] as before
$xml = '<upi:ReqHbt ...'
$b = [System.Text.Encoding]::UTF8.GetBytes($xml)
$len = [System.BitConverter]::GetBytes([int][uint32]$b.Length)
[Array]::Reverse($len)
$ssl.Write($len, 0, 4)
$ssl.Write($b, 0, $b.Length)
$ssl.Flush()
# Read response from $ssl
```

---

## 9. What Will NOT Work After SSL

| Tool | Why |
|------|-----|
| telnet | No TLS support |
| plain nc | No TLS |
| Postman | HTTP only |
| curl | HTTP only (not raw socket) |

Use: OpenSSL `s_client`, Java SSLSocket, PowerShell SslStream, Python ssl.wrap_socket.

---

## 10. Firewall Reminder

```bash
netsh advfirewall firewall add rule name="IMPS SSL Socket" dir=in action=allow protocol=TCP localport=9443
```

---

## 11. One-Line Takeaway

> **With SSL, you don't "just connect" — you must trust certificates.  
> With mTLS, both sides must trust each other.**

---

## 12. Implementation Order (Recommended)

| Order | Phase | Effort | Dependency |
|-------|-------|--------|------------|
| 1 | Phase A: Config | Low | None |
| 2 | Phase B: NpciSocketServer SSL | Medium | Phase A |
| 3 | Phase D: Cert/keystore setup | Low | - |
| 4 | Phase E: Docs + PowerShell | Low | Phase B |
| 5 | Phase C: NpciSocketClient SSL | Medium | Phase A (when NPCI uses TLS) |
| 6 | Phase F: SocketSwitchClient SSL | Medium | Phase A (when Switch uses TLS) |
| 7 | Phase G: REST HTTPS (optional) | Low | - |

---

## 13. Flow Diagram with SSL

```
Flow 1: NPCI → IMPS → Switch
────────────────────────────
NPCI ──TLS──▶ NpciSocketServer (9083/9443)  [IMPS server cert]
              │
              ▼
         SocketSwitchClient ──TLS──▶ Switch (9084/9444)  [Switch server cert; IMPS trusts]
              │                              │
              │◀────── ISO Resp ─────────────┘  [same TLS pipe]
              ▼
         NpciSocketClient ──TLS──▶ NPCI (9085/9445)  [NPCI server cert; IMPS trusts]

Flow 2: Switch → IMPS → NPCI (REST mode)
────────────────────────────────────────
Switch ──HTTP/HTTPS──▶ ImpsController (8081/8443)  [IMPS server cert for HTTPS]
              │
              ▼
         NpciSocketClient ──TLS──▶ NPCI  [or NpciMockClient REST]
```

---

## 14. Checklist for Architects

- [ ] `ssl.enabled`, `ssl.key-store`, `ssl.trust-store`, `ssl.client-auth` config
- [ ] NpciSocketServer uses SSLServerSocket when ssl.enabled (NPCI → IMPS)
- [ ] NpciSocketClient uses SSLSocket when npci.socket.ssl-enabled (IMPS → NPCI)
- [ ] SocketSwitchClient uses SSLSocket when routing.switch.socket.ssl-enabled (IMPS → Switch)
- [ ] Switch response (Switch → IMPS) on same TLS pipe – no extra work
- [ ] REST mode: server.ssl for HTTPS when Switch → IMPS (optional)
- [ ] Keytool / cert setup documented
- [ ] PowerShell and OpenSSL test commands documented
- [ ] Firewall rules for 9443, 9444, 9445 documented
- [ ] Environment matrix (local/UAT/PROD) documented
