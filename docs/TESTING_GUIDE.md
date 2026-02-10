# Guide: Testing IMPS APIs Locally (HTTP + TCP with TLS)

## 1. Prerequisites

### Databases

```powershell
psql -U postgres -c "CREATE DATABASE imps_db;"
psql -U postgres -c "CREATE DATABASE switch_db;"
psql -U postgres -d imps_db -f sql/imps_full_schema.sql
psql -U postgres -d switch_db -f sql/switch_full_schema.sql
```

### SSL keystores

```powershell
cd E:\Hitachi_Project
Remove-Item certs\*.jks, certs\*.cer -ErrorAction SilentlyContinue
.\certs\generate-certs.bat
```

### Start all services

```powershell
# Terminal 1 – IMPS Backend
cd Imps-backend
mvn spring-boot:run

# Terminal 2 – mock_switch
cd mock_switch
mvn spring-boot:run

# Terminal 3 – mock_npci
cd mock_npci
mvn spring-boot:run
```

**HTTPS (SSL profile)** – use the same command with `-Dspring-boot.run.profiles=ssl` for each app so tests can use HTTPS and fixed ports:

| Service     | HTTP (default) | HTTPS (ssl profile) |
|------------|----------------|----------------------|
| IMPS       | 8081           | **8443**             |
| mock_switch| 8082           | **8082**             |
| mock_npci  | 8083           | **8445**             |

```powershell
# Terminal 1 – IMPS (HTTPS 8443)
cd Imps-backend && mvn spring-boot:run -Dspring-boot.run.profiles=ssl

# Terminal 2 – mock_switch (HTTPS 8082)
cd mock_switch && mvn spring-boot:run -Dspring-boot.run.profiles=ssl

# Terminal 3 – mock_npci (HTTPS 8445)
cd mock_npci && mvn spring-boot:run -Dspring-boot.run.profiles=ssl
```

When IMPS runs with `ssl`, it calls Switch at `https://localhost:8082` and NPCI mock at `https://localhost:8445` (self-signed certs are trusted for dev).

---

## 2. How to check Switch → IMPS connection

The **Switch** sends requests **to** IMPS (reverse flow). You can verify the connection in two ways: **REST (Postman)** or **Socket**.

### Prerequisites

1. **IMPS Backend must be running.**
   - HTTP only: `cd Imps-backend && mvn spring-boot:run` → IMPS listens on **8081**.
   - HTTPS (for Postman with 8443): `mvn spring-boot:run -Dspring-boot.run.profiles=ssl` → IMPS listens on **8443**.
2. Optional: **mock_switch** running if you want IMPS to forward to a switch (8082/8444).

### Option A: Check via Postman (REST / HTTPS)

1. Start IMPS with the **ssl** profile so it listens on **8443**:
   ```powershell
   cd E:\Hitachi_Project\Imps-backend
   mvn spring-boot:run -Dspring-boot.run.profiles=ssl
   ```
2. In Postman, open the collection **IMPS API Collection (Dynamic)** → folder **Switch - IMPS flow**.
3. Pick a request (e.g. **ReqPay (ISO) - Switch to IMPS**).
4. Set **Body** → **Binary** → **Select file** → choose `postman/samples/iso_reqpay.bin` (or set the path in the request; path is relative to the collection folder).
5. Set **URL** to `https://192.168.1.38:8443/imps/reqpay/{{txnId}}` (or `https://localhost:8443/...` if testing on the same machine). Disable **SSL certificate verification** in Postman (Settings) for self-signed certs.
6. Click **Send**.

**Success:** Status **200** and response body is **binary ISO** (or XML if IMPS returns an error in XML).  
**Connection failure:** Connection refused / ECONNREFUSED → IMPS not running or wrong host/port.  
**Unpack error (e.g. DE-32):** Body was not sent as binary → use **Body → Binary → file** only.  
**"NPCI MOCK SEND FAILED … Connection refused":** IMPS forwards ReqPay (and other requests) to the **NPCI mock** on port **8083**. Start **mock_npci** so the full flow works: `cd mock_npci && mvn spring-boot:run`.

### Option B: Check via Socket (TCP or TLS)

Switch → IMPS uses the **same framing** as NPCI → IMPS but with **ISO** instead of XML, on a **different port**:

| Mode | IMPS port | Format        |
|------|-----------|---------------|
| TCP  | **9086**  | [4 bytes][ISO] |
| TLS  | **9446**  | [4 bytes][ISO] |

1. Start IMPS (socket server is on by default; ports 9086 and 9446 are open when `socket.server.switch.enabled: true`).
2. From the Switch (or a test client), open a TCP connection to **host:9086** (or **host:9446** for TLS).
3. Send **4 bytes** (big-endian length of the ISO), then the **ISO 8583 bytes**.
4. Read **4 bytes** (response length), then the **response ISO** bytes.

**Success:** IMPS accepts the connection and returns a response ISO (e.g. 0210 for ReqPay).  
**Connection failure:** Cannot connect → check IMPS is running, firewall, and that you are using port **9086** (TCP) or **9446** (TLS) for Switch → IMPS (not 9083/9443, which are for NPCI → IMPS).

Sample ISO binaries for socket testing: use the same files under `postman/samples/` (e.g. `iso_reqpay.bin`). Prepend the 4-byte length (big-endian) before sending. See [socket/SOCKET_GUIDE.md](socket/SOCKET_GUIDE.md) for scripts.

### Quick checklist

| Step | Action |
|------|--------|
| 1 | IMPS running (`mvn spring-boot:run` or with `ssl` profile for 8443). |
| 2 | For REST: Postman → **Switch - IMPS flow** → Body **Binary** → select `postman/samples/iso_reqpay.bin` → Send to `https://&lt;host&gt;:8443/imps/reqpay/{{txnId}}`. |
| 3 | For socket: Connect to **&lt;host&gt;:9086** (TCP) or **&lt;host&gt;:9446** (TLS), send [4 bytes][ISO], read [4 bytes][ISO]. |
| 4 | Check IMPS console for logs: `[IMPS] ReqPay ISO received from Switch txnId=...` and no unpack error. |

---

## 3. HTTP (REST) Testing

### Postman

1. Import **`postman/IMPS_API_Collection.postman_collection.json`**
2. **Variables (HTTP):**
   - `impsBaseUrl`: `http://localhost:8081`
   - `switchBaseUrl`: `http://localhost:8082`
   - `npciMockBaseUrl`: `http://localhost:8083`
3. **Variables (HTTPS):** Use `impsBaseUrlHttps`: `https://localhost:8443` when IMPS runs with `--spring.profiles.active=ssl`. Disable SSL certificate verification in Postman for dev.
4. **Request format (NPCI → IMPS, XML):** `POST {{impsBaseUrl}}/imps/{reqtype}/{{txnId}}`, Content-Type: `application/xml`.  
   Example: `POST http://localhost:8081/imps/reqpay/PAY00000000000000000000000000000001`
5. **Reverse flow (Switch → IMPS, ISO):** Use folder **Reverse flow - Switch → IMPS (ISO)**. Body: **binary** (select ISO 8583 file). Content-Type: `application/octet-stream`. Response: binary ISO. See [POSTMAN.md](POSTMAN.md).

### curl examples

```powershell
# ReqHbt
curl -X POST "http://localhost:8081/imps/reqhbt/HBT00000000000000000000000000000001" `
  -H "Content-Type: application/xml" `
  -d '<upi:ReqHbt xmlns:upi="http://npci.org/upi/schema/">...</upi:ReqHbt>'

# ReqPay (needs mock_switch up)
curl -X POST "http://localhost:8081/imps/reqpay/PAY00000000000000000000000000000001" `
  -H "Content-Type: application/xml" `
  -d @reqpay-body.xml
```

---

## 4. TCP Socket (Plain) Testing

**Port:** 9083

### PowerShell (from [SOCKET_GUIDE.md](socket/SOCKET_GUIDE.md))

**1. Define the helper (compliant flow – read once):**

```powershell
function Send-SocketXml-Compliant {
    param([string]$HostParam='localhost',[int]$PortParam=9083,[string]$Xml)
    $tcp = New-Object System.Net.Sockets.TcpClient($HostParam, $PortParam)
    $s = $tcp.GetStream()
    $b = [System.Text.Encoding]::UTF8.GetBytes($Xml)
    $len = [System.BitConverter]::GetBytes([int][uint32]$b.Length)
    [Array]::Reverse($len)
    $s.Write($len, 0, 4)
    $s.Write($b, 0, $b.Length)
    $s.Flush()
    $lenBuf = New-Object byte[] 4
    $s.Read($lenBuf, 0, 4) | Out-Null
    [Array]::Reverse($lenBuf)
    $respLen = [System.BitConverter]::ToInt32($lenBuf, 0)
    $respBuf = New-Object byte[] $respLen
    $s.Read($respBuf, 0, $respLen) | Out-Null
    $s.Close(); $tcp.Close()
    return [System.Text.Encoding]::UTF8.GetString($respBuf)
}
```

**2. Send ReqHbt:**

```powershell
$xml = '<upi:ReqHbt xmlns:upi="http://npci.org/upi/schema/"><Head ver="1.0" ts="2026-02-06T10:00:00+05:30" orgId="BANK01" msgId="HBT00000000000000000000000000000001"/><Txn id="HBT00000000000000000000000000000001" note="Heartbeat" refId="1" refUrl="https://npci.org.in/" ts="2026-02-06T10:00:00+05:30" type="Hbt"/><HbtMsg type="ALIVE" value="NA"/></upi:ReqHbt>'
Send-SocketXml-Compliant -HostParam localhost -PortParam 9083 -Xml $xml
```

**Expected:** ACK XML. For ReqPay/ReqChkTxn/ReqValAdd, mock_switch must be running and IDs must be 35 chars. Full scripts are in [SOCKET_GUIDE.md](socket/SOCKET_GUIDE.md).

---

## 5. TCP Socket with TLS Testing

### Step 1: Enable TLS on IMPS

In `Imps-backend/src/main/resources/application.yml`:

```yaml
ssl:
  enabled: true   # was false
```

Ensure keystore paths are correct (e.g. `file:../certs/imps-keystore.jks` when running from Imps-backend).

### Step 2: Restart IMPS

IMPS will listen on **9443** instead of 9083 for NPCI.

### Step 3: TLS test with PowerShell

Use `SslStream` and port **9443**:

```powershell
$hostParam = 'localhost'
$portParam = 9443
$tcp = New-Object System.Net.Sockets.TcpClient($hostParam, $portParam)
$ssl = New-Object System.Net.Security.SslStream($tcp.GetStream(), $false, ({ param($s,$c,$ch,$e) $true }))
$ssl.AuthenticateAsClient($hostParam)
$xml = '<upi:ReqHbt xmlns:upi="http://npci.org/upi/schema/"><Head ver="1.0" ts="2026-02-06T10:00:00+05:30" orgId="BANK01" msgId="HBT00000000000000000000000000000001"/><Txn id="HBT00000000000000000000000000000001" note="Heartbeat" refId="1" refUrl="https://npci.org.in/" ts="2026-02-06T10:00:00+05:30" type="Hbt"/><HbtMsg type="ALIVE" value="NA"/></upi:ReqHbt>'
$b = [System.Text.Encoding]::UTF8.GetBytes($xml)
$len = [System.BitConverter]::GetBytes([int][uint32]$b.Length)
[Array]::Reverse($len)
$ssl.Write($len, 0, 4)
$ssl.Write($b, 0, $b.Length)
$ssl.Flush()
$lenBuf = New-Object byte[] 4
$ssl.Read($lenBuf, 0, 4) | Out-Null
[Array]::Reverse($lenBuf)
$respLen = [System.BitConverter]::ToInt32($lenBuf, 0)
$respBuf = New-Object byte[] $respLen
$ssl.Read($respBuf, 0, $respLen) | Out-Null
[System.Text.Encoding]::UTF8.GetString($respBuf)
$ssl.Close(); $tcp.Close()
```

> `({ param($s,$c,$ch,$e) $true })` accepts the self-signed cert for **dev only**; use proper cert validation in production.

### Step 4: OpenSSL quick check

```bash
openssl s_client -connect localhost:9443
```

---

## 6. HTTP over HTTPS (REST + TLS)

For HTTPS on IMPS (port **8443**):

```powershell
cd Imps-backend
mvn spring-boot:run -Dspring-boot.run.profiles=ssl
```

Then:

```powershell
curl -k https://localhost:8443/imps/reqhbt/HBT00000000000000000000000000000001 -X POST -H "Content-Type: application/xml" -d "..."
```

`-k` skips cert verification for dev.

---

## 7. Test matrix

| API         | HTTP (8081) | HTTPS (8443) | Socket TCP (9083) | Socket TLS (9443) | Switch needed |
|-------------|-------------|--------------|-------------------|-------------------|---------------|
| ReqHbt      | Yes         | Yes          | Yes               | Yes               | No            |
| ReqListAccPvd | Yes       | Yes          | Yes               | Yes               | No            |
| ReqPay      | Yes         | Yes          | Yes               | Yes               | Yes (9084)    |
| ReqChkTxn   | Yes         | Yes          | Yes               | Yes               | Yes (9084)    |
| ReqValAdd   | Yes         | Yes          | Yes               | Yes               | Yes (9084)    |

**Reverse flow (Switch → IMPS):** Same APIs as above; Switch sends **ISO** to IMPS (REST: `application/octet-stream`). Test via Postman folder “Reverse flow - Switch → IMPS (ISO)” with binary body, or via TCP/TLS to IMPS if Switch uses socket.

---

## 8. Order of testing

1. Start DBs and apply schemas
2. Generate certs and start all three apps
3. **Plain TCP (9083):** PowerShell ReqHbt/ReqPay/ReqChkTxn/ReqValAdd
4. **HTTP (8081):** Postman or curl
5. **TCP TLS (9443):** Enable `ssl.enabled: true`, restart IMPS, run TLS PowerShell/OpenSSL tests
6. **HTTPS (8443):** Run with `--spring.profiles.active=ssl`, then curl/Postman over `https://localhost:8443`

---

## Reference

| Document | Purpose |
|----------|---------|
| [POSTMAN.md](POSTMAN.md) | Postman collection, variables (HTTP/HTTPS), folders, reverse-flow ISO format |
| [PROJECT_CONNECTIONS.md](PROJECT_CONNECTIONS.md) | Ports (HTTP/HTTPS/TCP/TLS), DB, config |
| [SOCKET_GUIDE.md](socket/SOCKET_GUIDE.md) | Full socket protocol and PowerShell scripts |
| [SOCKET_SSL_TLS.md](socket/SOCKET_SSL_TLS.md) | TLS keytool, OpenSSL, firewall |
| [IMPS_Req_API_Bodies.md](specs/IMPS_Req_API_Bodies.md) | Sample request bodies, test accounts |
| [NPCI_IMPS_Message_Formats.md](specs/NPCI_IMPS_Message_Formats.md) | Req/Resp XML structures |
