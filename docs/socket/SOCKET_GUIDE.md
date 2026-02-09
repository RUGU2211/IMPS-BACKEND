# IMPS Socket – Testing & PowerShell APIs

Test the IMPS NPCI socket with PowerShell only (no extra tools).  
**XML format:** All bodies must follow [NPCI_IMPS_Message_Formats](../specs/NPCI_IMPS_Message_Formats.md).

**npci.compliant-flow: true (default):** Read **once** (ACK only). Resp is sent by IMPS to mock_npci:9085 on a separate connection.  
**npci.compliant-flow: false:** Read **twice** (ACK, then Resp on same socket).

---

## Socket message types & ports

| API | Switch required | Plain port | SSL port |
|-----|-----------------|------------|----------|
| ReqHbt | No | 9083 | 9443 |
| ReqListAccPvd | No | 9083 | 9443 |
| ReqPay | Yes (mock_switch) | 9083 | 9443 |
| ReqChkTxn | Yes (mock_switch) | 9083 | 9443 |
| ReqValAdd | Yes (mock_switch) | 9083 | 9443 |

**Test from own PC:** `$HostParam='localhost'`. **From another PC:** `$HostParam='<IMPS server IP>'` (e.g. `192.168.1.38`).

**ID format:** msgId and Txn id must be **35 chars** (3 BPC + 32), e.g. `HBT00000000000000000000000000000001`.

---

## Prerequisites

1. Start **IMPS Backend** (socket enabled, port 9083)
2. For ReqPay/ReqChkTxn/ReqValAdd: start **mock_switch** (port 9084)
3. For **npci.compliant-flow: true**: start **mock_npci** (port 9085) so IMPS can send Resp outbound
4. REST optional: set `routing.switch.rest.enabled: true` in IMPS for HTTP instead of socket

---

## Protocol

- **Request:** `[4 bytes length big-endian][XML payload]`
- **Format:** NPCI↔IMPS XML; IMPS↔Switch ISO
- **txn_id:** Same value across the full flow (Rules 021/022)

### ACK flow (compliant)

1. NPCI sends Req (XML).
2. IMPS sends ACK (XML) on same socket → NPCI closes.
3. IMPS processes, connects to Switch 9084, gets Resp.
4. IMPS opens **new** connection to mock_npci:9085, sends Resp, reads ACK, closes.
5. **Client reads once:** ACK only.

### ACK flow (legacy)

1. NPCI sends Req → IMPS sends ACK → IMPS sends Resp on same socket.
2. **Client reads twice:** ACK, then Resp.

| Step | From | To | Message | Format |
|------|------|-----|---------|--------|
| 1 | NPCI | IMPS | Req | XML |
| 2 | IMPS | NPCI | ACK | XML |
| 3 | IMPS | Switch | Req | ISO |
| 4 | Switch | IMPS | Resp | ISO |
| 5 | IMPS | NPCI | Resp | XML (compliant: new conn to 9085) |
| 6 | NPCI | IMPS | ACK | XML |

---

## 1. Generic helper (PowerShell)

**Compliant (read once – ACK only):** `Send-SocketXml-Compliant`.  
**Legacy (read twice – ACK + Resp):** `Send-SocketXml`. Parameters: `HostParam`, `PortParam`, `Xml`.

```powershell
# Compliant flow: read once (ACK only)
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

# Legacy flow: read twice (ACK, then Resp)
function Send-SocketXml {
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
    $ack = [System.Text.Encoding]::UTF8.GetString($respBuf)
    $s.Read($lenBuf, 0, 4) | Out-Null
    [Array]::Reverse($lenBuf)
    $respLen = [System.BitConverter]::ToInt32($lenBuf, 0)
    $respBuf = New-Object byte[] $respLen
    $s.Read($respBuf, 0, $respLen) | Out-Null
    $resp = [System.Text.Encoding]::UTF8.GetString($respBuf)
    $s.Close(); $tcp.Close()
    return $resp
}
```

---

## 2. ReqHbt (heartbeat) – no Switch

```powershell
$HostParam='localhost'; $PortParam=9083
$xml='<upi:ReqHbt xmlns:upi="http://npci.org/upi/schema/"><Head ver="1.0" ts="2026-02-06T10:00:00+05:30" orgId="BANK01" msgId="HBT00000000000000000000000000000001"/><Txn id="HBT00000000000000000000000000000001" note="Heartbeat" refId="1" refUrl="https://npci.org.in/" ts="2026-02-06T10:00:00+05:30" type="Hbt"/><HbtMsg type="ALIVE" value="NA"/></upi:ReqHbt>'
Send-SocketXml-Compliant -HostParam $HostParam -PortParam $PortParam -Xml $xml
```

**One-liner (compliant):**
```powershell
$tcp=New-Object System.Net.Sockets.TcpClient('localhost',9083);$s=$tcp.GetStream();$xml='<upi:ReqHbt xmlns:upi="http://npci.org/upi/schema/"><Head ver="1.0" ts="2026-02-06T10:00:00+05:30" orgId="BANK01" msgId="HBT00000000000000000000000000000001"/><Txn id="HBT00000000000000000000000000000001" note="Heartbeat" refId="1" refUrl="https://npci.org.in/" ts="2026-02-06T10:00:00+05:30" type="Hbt"/><HbtMsg type="ALIVE" value="NA"/></upi:ReqHbt>';$b=[System.Text.Encoding]::UTF8.GetBytes($xml);$len=[System.BitConverter]::GetBytes([int][uint32]$b.Length);[Array]::Reverse($len);$s.Write($len,0,4);$s.Write($b,0,$b.Length);$s.Flush();$lb=New-Object byte[] 4;$s.Read($lb,0,4)|Out-Null;[Array]::Reverse($lb);$rl=[System.BitConverter]::ToInt32($lb,0);$rb=New-Object byte[] $rl;$s.Read($rb,0,$rl)|Out-Null;[System.Text.Encoding]::UTF8.GetString($rb)
```

---

## 3. ReqListAccPvd – no Switch

```powershell
$HostParam='localhost'; $PortParam=9083
$xml='<upi:ReqListAccPvd xmlns:upi="http://npci.org/upi/schema/"><Head ver="2.0" ts="2026-02-06T10:00:00+05:30" orgId="BANK01" msgId="LAP00000000000000000000000000000001" prodType="IMPS"/><Txn id="LAP00000000000000000000000000000001" type="ListAccPvd"/></upi:ReqListAccPvd>'
Send-SocketXml-Compliant -HostParam $HostParam -PortParam $PortParam -Xml $xml
```

---

## 4. ReqPay – requires mock_switch

```powershell
$HostParam='localhost'; $PortParam=9083
$xml='<ns2:ReqPay xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/"><Head ver="2.0" ts="2026-02-06T10:30:00+05:30" orgId="BANK01" msgId="PAY00000000000000000000000000000001" prodType="IMPS"/><Txn note="P2A Fund Transfer" custRef="023113001276" refId="001276" refUrl="https://www.npci.org.in/" ts="2026-02-06T10:30:00+05:30" refCategory="00" type="PAY" purpose="00" initiationMode="00" id="PAY00000000000000000000000000000001"/><Payer addr="BANK01@psp" name="Rugved Kharde" seqNum="1" type="ENTITY" code="4814"><Info><Identity id="1234567890123456|HDFC0000001" type="BANK" verifiedName="Rugved Kharde"/><Rating verifiedAddress="TRUE"/></Info><Device><Tag name="MOBILE" value="919494916511"/><Tag name="LOCATION" value=""/><Tag name="TYPE" value="MOB"/><Tag name="cardAccpTrId" value="NPC16511"/><Tag name="cardAccIdCode" value=""/></Device><Ac addrType="ACCOUNT"><Detail name="ACNUM" value="1234567890123456"/><Detail name="IFSC" value="HDFC0000001"/><Detail name="ACTYPE" value="SAVINGS"/></Ac><Creds><Cred subType="NA" type="PreApproved"><Data>MDB8QVBQUk9WRUQ</Data></Cred></Creds><Amount value="100.00" curr="INR"/></Payer><Payees><Payee seqNum="0" type="PERSON" code="0000"><Amount value="100.00" curr="INR"/><Ac addrType="ACCOUNT"><Detail name="IFSC" value="ICIC0000001"/><Detail name="ACTYPE" value="SAVINGS"/><Detail name="ACNUM" value="1111222233334444"/></Ac></Payee></Payees></ns2:ReqPay>'
Send-SocketXml-Compliant -HostParam $HostParam -PortParam $PortParam -Xml $xml
```

---

## 5. ReqChkTxn – requires mock_switch

```powershell
$HostParam='localhost'; $PortParam=9083
$xml='<ns2:ReqChkTxn xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/"><Head ver="2.0" ts="2026-02-06T10:44:03+05:30" orgId="BANK01" msgId="CHK00000000000000000000000000000001" prodType="IMPS"/><Txn custRef="023113001279" id="CHK00000000000000000000000000000001" initiationMode="00" note="Check Transaction Status" orgRrn="023113001279" orgTxnDate="2026-02-06T10:43:31+05:30" orgTxnId="PAY00000000000000000000000000000099" purpose="00" refCategory="00" refId="001279" refUrl="https://www.npci.org.in/" subType="PAY" ts="2026-02-06T10:43:31+05:30" type="VR"/><Payer addr="BANK01@psp" name="Rugved Kharde" seqNum="1" type="ENTITY" code="4814"><Info><Identity id="1234567890123456|HDFC0000001" type="ACCOUNT" verifiedName="Rugved Kharde"/><Rating verifiedAddress="TRUE"/></Info><Device><Tag name="MOBILE" value="919494916511"/><Tag name="LOCATION" value=""/><Tag name="TYPE" value="MOB"/><Tag name="cardAccpTrId" value="NPC16511"/><Tag name="cardAccIdCode" value=""/></Device><Ac addrType="ACCOUNT"><Detail name="ACNUM" value="1234567890123456"/><Detail name="IFSC" value="HDFC0000001"/><Detail name="ACTYPE" value="SAVINGS"/></Ac><Amount value="1000.00" curr="INR"/></Payer><Payees><Payee seqNum="0" type="PERSON" code="0000"><Amount value="1000.00" curr="INR"/><Ac addrType="ACCOUNT"><Detail name="IFSC" value="ICIC0000001"/><Detail name="ACTYPE" value="SAVINGS"/><Detail name="ACNUM" value="1111222233334444"/></Ac></Payee></Payees></ns2:ReqChkTxn>'
Send-SocketXml-Compliant -HostParam $HostParam -PortParam $PortParam -Xml $xml
```

---

## 6. ReqValAdd – requires mock_switch

```powershell
$HostParam='localhost'; $PortParam=9083
$xml='<ns2:ReqValAdd xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/"><Head msgId="VAL00000000000000000000000000000001" orgId="BANK01" prodType="IMPS" ts="2026-02-06T14:48:23+05:30" ver="2.0"/><Txn custRef="023314480540" id="VAL00000000000000000000000000000001" initiationMode="00" note="Name Enquiry Request" refId="860454" refUrl="https://www.npci.org.in/" ts="2026-02-06T14:48:01+05:30" type="NameEnq"/><Payer addr="BANK01@psp" code="4814" name="ENQUIRER NAME" seqNum="1" type="ENTITY"><Info><Identity id="12110100020142|HDFC0000001" type="BANK" verifiedName="ENQUIRER NAME"/><Rating verifiedAddress="TRUE"/></Info><Device><Tag name="cardAccpTrId" value="DNB67667"/><Tag name="cardAccIdCode" value="DNB917667667667"/><Tag name="MOBILE" value="917667667667"/><Tag name="LOCATION" value="HDFC BANK MOB7667667667IN"/><Tag name="TYPE" value="MOB"/></Device><Ac addrType="MOBILE"><Detail name="MMID" value="4002111"/><Detail name="MOBNUM" value="919494916511"/><Detail name="ACTYPE" value="SAVINGS"/></Ac></Payer><Payee code="0000" seqNum="0" type="PERSON"><Ac addrType="ACCOUNT"><Detail name="IFSC" value="ICIC0000001"/><Detail name="ACNUM" value="1111222233334444"/><Detail name="ACTYPE" value="DEFAULT"/></Ac></Payee></ns2:ReqValAdd>'
Send-SocketXml-Compliant -HostParam $HostParam -PortParam $PortParam -Xml $xml
```

---

## 7. Reverse flow – Switch → IMPS ([4 bytes][ISO], port 9086/9446)

Switch connects to IMPS on **9086** (TCP) or **9446** (TLS). Protocol: `[4 bytes length big-endian][ISO]` — same framing as NPCI→IMPS, but payload is ISO instead of XML.

**PowerShell helper (plain TCP):**

```powershell
function Send-SocketIso-Switch {
    param([string]$HostParam='localhost',[int]$PortParam=9086,[byte[]]$IsoBytes)
    $tcp = New-Object System.Net.Sockets.TcpClient($HostParam, $PortParam)
    $s = $tcp.GetStream()
    $len = [System.BitConverter]::GetBytes([int][uint32]$IsoBytes.Length)
    [Array]::Reverse($len)
    $s.Write($len, 0, 4)
    $s.Write($IsoBytes, 0, $IsoBytes.Length)
    $s.Flush()
    $lenBuf = New-Object byte[] 4
    $s.Read($lenBuf, 0, 4) | Out-Null
    [Array]::Reverse($lenBuf)
    $respLen = [System.BitConverter]::ToInt32($lenBuf, 0)
    $respBuf = New-Object byte[] $respLen
    $s.Read($respBuf, 0, $respLen) | Out-Null
    $s.Close(); $tcp.Close()
    return $respBuf
}
```

Use with ISO bytes from your packager (e.g. 0200 financial, 0800 heartbeat). IMPS returns `[4 bytes][ISO]` response (0210 or 0810 ACK).

---

## 8. TLS (SslStream) – port 9443

When `ssl.enabled: true`, use port **9443** and wrap the stream with `SslStream`:

```powershell
$hostParam = 'localhost'; $portParam = 9443
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

Keystore/keytool: see [SOCKET_SSL_TLS.md](SOCKET_SSL_TLS.md).

---

## 9. Test from another PC

Use IMPS server IP instead of localhost:

```powershell
$HostParam='192.168.1.38'; $PortParam=9083
# Then run any Send-SocketXml-Compliant / Send-SocketXml with -HostParam $HostParam -PortParam $PortParam -Xml $xml
```

---

## Socket ports and modes

| Service | Port | Role |
|---------|------|------|
| IMPS | 9083 / 9443 | NPCI socket server ([4 bytes][XML]) |
| IMPS | 9086 / 9446 | Switch socket server – reverse flow ([4 bytes][ISO]) |
| mock_switch | 9084 / 9444 | Switch socket server ([4 bytes][ISO]) |
| mock_npci | 9085 / 9445 | Receives Resp from IMPS (compliant flow) |

**Reverse flow (Switch → IMPS):** Same connection pattern as NPCI→IMPS. Switch connects to IMPS on 9086 (TCP) or 9446 (TLS), sends `[4 bytes][ISO]`, receives `[4 bytes][ISO]`. Use PowerShell with ISO bytes instead of XML.

| Connection | Config |
|------------|--------|
| NPCI → IMPS | `socket.enabled: true` |
| IMPS → Switch | `routing.switch.socket.enabled: true`, `institution_master` |
| IMPS → NPCI | `npci.compliant-flow: true`, `npci.socket.host:port` |

---

## Troubleshooting

| Symptom | Check |
|--------|--------|
| Connection refused | IMPS running? `socket.enabled: true`? Port 9083? mock_npci on 9085 (compliant)? |
| ACK instead of RespHbt/RespPay | Use 35-char msgId and Txn id (Rules 021/022) |
| No response / timeout | mock_switch running (ReqPay etc.)? mock_npci running (compliant)? Response timeout 60s? |
| Invalid length | Length = 4 bytes big-endian, matching XML byte count |

---

## Reference

| Document | Purpose |
|----------|---------|
| [NPCI_IMPS_Message_Formats](../specs/NPCI_IMPS_Message_Formats.md) | Req/Resp XML structures |
| [PROJECT_CONNECTIONS](../PROJECT_CONNECTIONS.md) | Ports, config, databases |
| [SOCKET_SSL_TLS](SOCKET_SSL_TLS.md) | Keytool, OpenSSL, firewall |
