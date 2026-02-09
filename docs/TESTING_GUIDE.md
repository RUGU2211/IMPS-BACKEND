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

---

## 2. HTTP (REST) Testing

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

## 3. TCP Socket (Plain) Testing

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

## 4. TCP Socket with TLS Testing

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

## 5. HTTP over HTTPS (REST + TLS)

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

## 6. Test matrix

| API         | HTTP (8081) | HTTPS (8443) | Socket TCP (9083) | Socket TLS (9443) | Switch needed |
|-------------|-------------|--------------|-------------------|-------------------|---------------|
| ReqHbt      | Yes         | Yes          | Yes               | Yes               | No            |
| ReqListAccPvd | Yes       | Yes          | Yes               | Yes               | No            |
| ReqPay      | Yes         | Yes          | Yes               | Yes               | Yes (9084)    |
| ReqChkTxn   | Yes         | Yes          | Yes               | Yes               | Yes (9084)    |
| ReqValAdd   | Yes         | Yes          | Yes               | Yes               | Yes (9084)    |

**Reverse flow (Switch → IMPS):** Same APIs as above; Switch sends **ISO** to IMPS (REST: `application/octet-stream`). Test via Postman folder “Reverse flow - Switch → IMPS (ISO)” with binary body, or via TCP/TLS to IMPS if Switch uses socket.

---

## 7. Order of testing

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
