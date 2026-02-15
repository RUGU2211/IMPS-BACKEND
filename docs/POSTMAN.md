# Postman – IMPS API Collection

Use the Postman collection to test IMPS over **HTTP**, **HTTPS**, and to simulate the **reverse flow** (Switch → IMPS with ISO).

## Collection file

- **Path:** `postman/IMPS_API_Collection.postman_collection.json`
- **Import:** Postman → Import → Upload the file (or drag-and-drop).

---

## Connection types and variables

| Variable | Default | Use for |
|----------|---------|---------|
| `impsBaseUrl` | `http://localhost:8081` | IMPS REST (HTTP) |
| `impsBaseUrlHttps` | `https://localhost:8443` | IMPS REST over HTTPS (profile `ssl`) |
| `switchBaseUrl` | `http://localhost:8082` | Mock Switch REST |
| `switchBaseUrlHttps` | `https://localhost:8444` | Mock Switch HTTPS (if enabled) |
| `npciMockBaseUrl` | `http://localhost:8083` | NPCI Mock REST |
| `npciMockBaseUrlHttps` | `https://localhost:8445` | NPCI Mock HTTPS (if enabled) |
| `txnId` | (35 chars) | Pre-request script sets random; override as needed |
| `msgId` | (35 chars) | Same as above |
| `orgId` | `BANK01` | NPCI org |
| `npciOrgId` | `NPCI` | NPCI org for responses |

**TCP / TLS (no Postman):** Socket testing uses PowerShell or other TCP client. See [TESTING_GUIDE.md](TESTING_GUIDE.md) and [socket/SOCKET_GUIDE.md](socket/SOCKET_GUIDE.md).

| Connection | Protocol | Port | Postman? |
|------------|----------|------|----------|
| IMPS REST | HTTP | 8081 | Yes – use `{{impsBaseUrl}}` |
| IMPS REST | HTTPS | 8443 | Yes – use `{{impsBaseUrlHttps}}` (disable SSL cert verification for dev) |
| IMPS Socket | TCP | 9083 | No – use PowerShell / SOCKET_GUIDE |
| IMPS Socket | TLS | 9443 | No – use PowerShell with SslStream |
| Mock Switch | HTTP | 8082 | Yes – `{{switchBaseUrl}}` |
| Mock Switch Socket | TCP/TLS | 9084 / 9444 | No |
| NPCI Mock | HTTP | 8083 | Yes – `{{npciMockBaseUrl}}` |
| NPCI Mock Socket | TCP/TLS | 9085 / 9445 | No |

---

## Folders and format

### 1. IMPS – NPCI to IMPS (Port 8081)

- **Direction:** NPCI → IMPS (simulate NPCI sending requests).
- **Format:** XML.
- **URL:** `POST {{impsBaseUrl}}/imps/{reqtype}/{{txnId}}`.
- **Headers:** `Content-Type: application/xml`, `Accept: application/xml`.
- **Body:** Raw XML (ReqPay, ReqChkTxn, ReqValAdd, ReqHbt, ReqListAccPvd).
- **IDs:** `msgId` and `Txn @id` must be **35 characters** (3 BPC + 32). Pre-request script sets `txnId` and `msgId` randomly.

### 2. NPCI Mock – IMPS to NPCI (Port 8083)

- **Direction:** IMPS → NPCI (simulate IMPS sending responses to NPCI).
- **Format:** XML.
- **URL:** `POST {{npciMockBaseUrl}}/npci/{resptype}/{{txnId}}`.
- **Use:** After NPCI sends Req to IMPS, IMPS forwards to Switch and then sends Resp to NPCI Mock at this base URL (or via socket to 9085/9445).

### 3. Switch – IMPS to Switch (Port 8082)

- **Direction:** IMPS → Switch (Req) and Switch → IMPS (Resp).
- **Format:** XML in these examples; Mock Switch can accept XML or ISO.
- **Req to Switch:** `POST {{switchBaseUrl}}/imps/reqpay/{{txnId}}` (and other req types).
- **Resp to IMPS:** `POST {{impsBaseUrl}}/imps/resppay/{{txnId}}` (and other resp types). Mock Switch forwards response to IMPS.

### 4. Reverse flow – Switch → IMPS (ISO)

**Primary: Socket [4 bytes][ISO]** — Same connection pattern as NPCI→IMPS. IMPS listens on **9086** (TCP) or **9446** (TLS). Switch sends `[4 bytes][ISO]`, receives `[4 bytes][ISO]`. See SOCKET_GUIDE.md.

**Optional: REST (Body → binary)**
- **Direction:** Switch sends **Request** as binary ISO 8583; IMPS returns **Response** as binary ISO.
- **Format:** Binary ISO (Body → binary in Postman).
- **URL:** `POST {{impsBaseUrl}}/imps/reqpay/{{txnId}}` (and `/imps/reqchktxn/`, `/imps/reqhbt/`, `/imps/reqlistaccpvd/`, `/imps/reqvaladd/`).
- **Headers:** `Content-Type: application/octet-stream`, `Accept: application/octet-stream`.
- **Body:** In Postman, choose **Body → binary** and select an ISO 8583 binary file (e.g. 0200 for financial request). If you don’t have a file, use a TCP/socket tool or build ISO with your own packager.
- **Response:** Binary ISO (e.g. 0210 or 0810 ACK).
- **HTTPS:** Use `{{impsBaseUrlHttps}}` instead of `{{impsBaseUrl}}` when testing with profile `ssl` (port 8443). Turn off “SSL certificate verification” in Postman for self-signed certs.

### 5. Reference – Response Codes

- **Reference only** (DO NOT SEND). Lists response codes and types.

---

## Testing HTTP vs HTTPS

1. **HTTP (default):** Ensure `impsBaseUrl` = `http://localhost:8081`. Run IMPS with `mvn spring-boot:run` (no profile). See [TESTING_GUIDE.md](TESTING_GUIDE.md) for full run commands. Use folders 1–4 with `{{impsBaseUrl}}`.
2. **HTTPS:** Start IMPS with `-Dspring-boot.run.profiles=ssl` (port 8443). Set request URL to `{{impsBaseUrlHttps}}` or duplicate a request and change the host to `https://localhost:8443`. In Postman → Settings → turn off “SSL certificate verification” for local testing.

---

## Proper format summary

| Flow | Connection | Format | Port |
|------|------------|--------|------|
| NPCI → IMPS | Socket | [4 bytes][XML] | 9083 / 9443 |
| **Switch → IMPS (reverse)** | **Socket** | **[4 bytes][ISO]** | **9086 / 9446** |
| Switch → IMPS (reverse) | REST | Binary ISO body | 8081 / 8443 |
| IMPS → Switch | Socket | [4 bytes][ISO] | 9084 / 9444 |
| IMPS → NPCI | Socket | [4 bytes][XML] | 9085 / 9445 |

Reverse flow uses the same connection pattern as NPCI→IMPS: length-prefixed framing. For Switch the payload is ISO instead of XML.

---

## See also

- [TESTING_GUIDE.md](TESTING_GUIDE.md) – HTTP, TCP, TLS, HTTPS steps and test matrix.
- [PROJECT_CONNECTIONS.md](PROJECT_CONNECTIONS.md) – Ports, DB, socket/HTTP config.
- [socket/SOCKET_GUIDE.md](socket/SOCKET_GUIDE.md) – Socket protocol and PowerShell scripts.
- [socket/SOCKET_SSL_TLS.md](socket/SOCKET_SSL_TLS.md) – TLS keytool and OpenSSL.
