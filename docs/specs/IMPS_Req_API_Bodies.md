# IMPS Request APIs – Copy-Paste Bodies with {placeholders}

This doc covers **two flows only**: (1) **NPCI → IMPS** (XML request bodies below) and (2) **Switch → IMPS** (binary ISO; see [SOCKET_GUIDE.md](../socket/SOCKET_GUIDE.md)).

**Usage:** Copy the XML block, then replace each `{placeholder}` with your value. All IDs must be **35 chars** (3 BPC + 32).

**Paths:** `{baseUrl}/imps/{reqtype}/{txnId}` — e.g. `https://192.168.1.38:8443/imps/reqpay/{txnId}`  
**Content-Type:** `application/xml` for NPCI → IMPS. For Switch → IMPS (ISO): `application/octet-stream` and binary ISO 8583 (no XML body; see socket guide).

**Source:** [NPCI_IMPS_Message_Formats.md](NPCI_IMPS_Message_Formats.md) | [SOCKET_GUIDE.md](../socket/SOCKET_GUIDE.md)

---

## Placeholder reference

| Placeholder | Example | Description |
|-------------|---------|-------------|
| `{baseUrl}` | https://192.168.1.38:8443 | IMPS REST base URL (HTTPS, port 8443 with ssl profile) |
| `{orgId}` | BANK01 | Organisation ID (3–20 chars) |
| `{msgId}` | BAN5t2Dk18UFMIMFENLBga12345678901234 | Head msgId, 35 chars |
| `{txnId}` | BAN5t2Dk18UFMIMFENLBgb12345678901234 | Txn id, 35 chars |
| `{ts}` | 2026-01-25T10:30:00.000+05:30 | ISO timestamp |
| `{payerAcNum}` | 1234567890123456 | Payer account number |
| `{payerIfsc}` | HDFC0000001 | Payer IFSC |
| `{payerName}` | Rugved Kharde | Payer verified name |
| `{payerMobile}` | 919494916511 | Payer mobile |
| `{payeeAcNum}` | 1111222233334444 | Payee account number |
| `{payeeIfsc}` | ICIC0000001 | Payee IFSC |
| `{amount}` | 1000.00 | Amount in INR |
| `{custRef}` | 023113001276 | Customer reference |
| `{refId}` | 001276 | Reference ID |
| `{orgTxnId}` | BAN5t2Dk18UFMIMG40acxy12345678901234 | Original txn id (ChkTxn) |
| `{orgRrn}` | 023113001279 | Original RRN |
| `{orgTxnDate}` | 2026-01-25T10:43:31.000+05:30 | Original txn date |
| `{initiationMode}` | 00 | 00=Default, 12=FIR |
| `{mmid}` | 4002111 | MMID (ValAdd) |
| `{mobNum}` | 919494916511 | Mobile number (ValAdd) |

---

## Two flows (aligned with Postman)

| Flow | Direction | Body | Path pattern |
|------|-----------|------|--------------|
| **NPCI - IMPS req flow** | NPCI → IMPS | XML (copy-paste from sections below) | `{baseUrl}/imps/{reqpay,reqchktxn,reqhbt,reqlistaccpvd,reqvaladd}/{txnId}` |
| **Switch - IMPS flow** | Switch → IMPS | Binary ISO 8583 | Same path; `Content-Type: application/octet-stream`. See [SOCKET_GUIDE.md](../socket/SOCKET_GUIDE.md). |

Postman collection: **IMPS API Collection (Dynamic)** — folders *NPCI - IMPS req flow* and *Switch - IMPS flow* only.

---

## ReqPay – P2A Fund Transfer (NPCI → IMPS)

**Path:** `{baseUrl}/imps/reqpay/{txnId}`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqPay xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0" ts="{ts}" orgId="{orgId}" msgId="{msgId}" prodType="IMPS"/>
    <Txn note="P2A Fund Transfer" custRef="{custRef}" refId="{refId}" refUrl="https://www.npci.org.in/"
         ts="{ts}" refCategory="00" type="PAY" purpose="00" initiationMode="{initiationMode}" id="{txnId}"/>
    <Payer addr="{orgId}@psp" name="{payerName}" seqNum="1" type="ENTITY" code="4814">
        <Info>
            <Identity id="{payerAcNum}|{payerIfsc}" type="BANK" verifiedName="{payerName}"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>
        <Device>
            <Tag name="MOBILE" value="{payerMobile}"/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value="MOB"/>
            <Tag name="cardAccpTrId" value="NPC16511"/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>
        <Ac addrType="ACCOUNT">
            <Detail name="ACNUM" value="{payerAcNum}"/>
            <Detail name="IFSC" value="{payerIfsc}"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>
        <Creds>
            <Cred subType="NA" type="PreApproved"><Data>MDB8QVBQUk9WRUQ</Data></Cred>
        </Creds>
        <Amount value="{amount}" curr="INR"/>
    </Payer>
    <Payees>
        <Payee seqNum="0" type="PERSON" code="0000">
            <Amount value="{amount}" curr="INR"/>
            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value="{payeeIfsc}"/>
                <Detail name="ACTYPE" value="SAVINGS"/>
                <Detail name="ACNUM" value="{payeeAcNum}"/>
            </Ac>
        </Payee>
    </Payees>
</ns2:ReqPay>
```

---

## ReqPay – P2P (Chetan to Madhav)

**Path:** `{baseUrl}/imps/reqpay/{txnId}`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqPay xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0" ts="{ts}" orgId="{orgId}" msgId="{msgId}" prodType="IMPS"/>
    <Txn note="P2P Fund Transfer" custRef="{custRef}" refId="{refId}" refUrl="https://www.npci.org.in/"
         ts="{ts}" refCategory="00" type="PAY" purpose="00" initiationMode="{initiationMode}" id="{txnId}"/>
    <Payer addr="{orgId}@psp" name="Chetan Mokashi" seqNum="1" type="ENTITY" code="4814">
        <Info>
            <Identity id="9876543210987654|HDFC0000001" type="BANK" verifiedName="Chetan Mokashi"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>
        <Device>
            <Tag name="MOBILE" value="919491916510"/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value="MOB"/>
            <Tag name="cardAccpTrId" value="NPC09101"/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>
        <Ac addrType="ACCOUNT">
            <Detail name="ACNUM" value="9876543210987654"/>
            <Detail name="IFSC" value="HDFC0000001"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>
        <Creds>
            <Cred subType="NA" type="PreApproved"><Data>MDB8QVBQUk9WRUQ</Data></Cred>
        </Creds>
        <Amount value="500.00" curr="INR"/>
    </Payer>
    <Payees>
        <Payee seqNum="0" type="PERSON" code="0000">
            <Amount value="500.00" curr="INR"/>
            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value="SBIN0000001"/>
                <Detail name="ACTYPE" value="SAVINGS"/>
                <Detail name="ACNUM" value="5555666677778888"/>
            </Ac>
        </Payee>
    </Payees>
</ns2:ReqPay>
```

---

## ReqPay – FIR (Foreign Inward Remittance)

**Path:** `{baseUrl}/imps/reqpay/{txnId}` — `{initiationMode}` = 12

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqPay xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0" ts="{ts}" orgId="{orgId}" msgId="{msgId}" prodType="IMPS"/>
    <Txn note="Foreign Inward Remittance" custRef="{custRef}" refId="{refId}" refUrl="https://www.npci.org.in/"
         ts="{ts}" refCategory="00" type="PAY" purpose="00" initiationMode="12" id="{txnId}"/>
    <Payer addr="{orgId}@psp" name="{payerName}" seqNum="1" type="ENTITY" code="4814">
        <Info>
            <Identity id="{payerAcNum}|{payerIfsc}" type="BANK" verifiedName="{payerName}"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>
        <Device>
            <Tag name="MOBILE" value="{payerMobile}"/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value="MOB"/>
            <Tag name="cardAccpTrId" value="NPC16511"/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>
        <Ac addrType="ACCOUNT">
            <Detail name="ACNUM" value="{payerAcNum}"/>
            <Detail name="IFSC" value="{payerIfsc}"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>
        <Creds>
            <Cred subType="NA" type="PreApproved"><Data>MDB8QVBQUk9WRUQ</Data></Cred>
        </Creds>
        <Amount value="10000.00" curr="INR"/>
        <Institution route="RDA" type="BANK">
            <Name value="TRANSWISE" acNum="123456789456" ifsc="PCIN0234123"/>
            <Purpose code="Credit to Beneficiary in INR" note="Foreign Inward Remittance"/>
            <Originator name="John William" refNo="1234567891" type="INDIVIDUAL">
                <Address location="Marlin Apartment Limehouse London"/>
            </Originator>
            <Beneficiary name="Sajid Mulla"/>
        </Institution>
    </Payer>
    <Payees>
        <Payee seqNum="0" type="PERSON" code="0000">
            <Amount value="10000.00" curr="INR"/>
            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value="{payeeIfsc}"/>
                <Detail name="ACTYPE" value="SAVINGS"/>
                <Detail name="ACNUM" value="{payeeAcNum}"/>
            </Ac>
        </Payee>
    </Payees>
</ns2:ReqPay>
```

---

## ReqPay – CREDIT (NPCI to Beneficiary Bank)

**Path:** `{baseUrl}/imps/reqpay/{txnId}` — Head orgId = NPCI, Txn type = CREDIT

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqPay xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0" ts="{ts}" orgId="NPCI" msgId="{msgId}" prodType="IMPS"/>
    <Txn note="Credit to Beneficiary" custRef="{custRef}" refId="{refId}" refUrl="https://www.npci.org.in/"
         ts="{ts}" refCategory="00" type="CREDIT" purpose="00" initiationMode="{initiationMode}" id="{txnId}"/>
    <Payer addr="{orgId}@psp" name="{payerName}" seqNum="1" type="ENTITY" code="4814">
        <Info>
            <Identity id="{payerAcNum}|{payerIfsc}" type="BANK" verifiedName="{payerName}"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>
        <Device>
            <Tag name="MOBILE" value="{payerMobile}"/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value="MOB"/>
            <Tag name="cardAccpTrId" value="NPC16511"/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>
        <Ac addrType="ACCOUNT">
            <Detail name="ACNUM" value="{payerAcNum}"/>
            <Detail name="IFSC" value="{payerIfsc}"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>
        <Creds>
            <Cred subType="NA" type="PreApproved"><Data>MDB8QVBQUk9WRUQ</Data></Cred>
        </Creds>
        <Amount value="{amount}" curr="INR"/>
    </Payer>
    <Payees>
        <Payee seqNum="0" type="PERSON" code="0000">
            <Amount value="{amount}" curr="INR"/>
            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value="{payeeIfsc}"/>
                <Detail name="ACTYPE" value="SAVINGS"/>
                <Detail name="ACNUM" value="{payeeAcNum}"/>
            </Ac>
        </Payee>
    </Payees>
</ns2:ReqPay>
```

---

## ReqChkTxn – Check Status Request

**Path:** `{baseUrl}/imps/reqchktxn/{txnId}`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqChkTxn xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0" ts="{ts}" orgId="{orgId}" msgId="{msgId}" prodType="IMPS"/>
    <Txn custRef="{custRef}" id="{txnId}" initiationMode="00" note="Check Transaction Status"
         orgRrn="{orgRrn}" orgTxnDate="{orgTxnDate}" orgTxnId="{orgTxnId}" purpose="00"
         refCategory="00" refId="{refId}" refUrl="https://www.npci.org.in/" subType="PAY" ts="{ts}" type="VR"/>
    <Payer addr="{orgId}@psp" name="{payerName}" seqNum="1" type="ENTITY" code="4814">
        <Info>
            <Identity id="{payerAcNum}|{payerIfsc}" type="ACCOUNT" verifiedName="{payerName}"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>
        <Device>
            <Tag name="MOBILE" value="{payerMobile}"/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value="MOB"/>
            <Tag name="cardAccpTrId" value="NPC16511"/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>
        <Ac addrType="ACCOUNT">
            <Detail name="ACNUM" value="{payerAcNum}"/>
            <Detail name="IFSC" value="{payerIfsc}"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>
        <Amount value="{amount}" curr="INR"/>
    </Payer>
    <Payees>
        <Payee seqNum="0" type="PERSON" code="0000">
            <Amount value="{amount}" curr="INR"/>
            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value="{payeeIfsc}"/>
                <Detail name="ACTYPE" value="SAVINGS"/>
                <Detail name="ACNUM" value="{payeeAcNum}"/>
            </Ac>
        </Payee>
    </Payees>
</ns2:ReqChkTxn>
```

---

## ReqHbt – Heartbeat ALIVE

**Path:** `{baseUrl}/imps/reqhbt/{txnId}`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<upi:ReqHbt xmlns:upi="http://npci.org/upi/schema/">
    <Head ver="1.0" ts="{ts}" orgId="{orgId}" msgId="{msgId}"/>
    <Txn id="{txnId}" note="Heartbeat Check" refId="123456" refUrl="https://www.npci.org.in/" ts="{ts}" type="Hbt"/>
    <HbtMsg type="ALIVE" value="NA"/>
</upi:ReqHbt>
```

---

## ReqHbt – Heartbeat EOD

**Path:** `{baseUrl}/imps/reqhbt/{txnId}`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<upi:ReqHbt xmlns:upi="http://npci.org/upi/schema/">
    <Head ver="1.0" ts="{ts}" orgId="NPCI" msgId="{msgId}"/>
    <Txn id="{txnId}" note="End of Day Signal" refId="123456" refUrl="https://www.npci.org.in/" ts="{ts}" type="Hbt"/>
    <HbtMsg type="EOD" value="2026-01-25"/>
</upi:ReqHbt>
```

---

## ReqListAccPvd – List Providers

**Path:** `{baseUrl}/imps/reqlistaccpvd/{txnId}`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqListAccPvd xmlns:ns2="http://npci.org/upi/schema/">
    <Head ver="2.0" ts="{ts}" orgId="{orgId}" msgId="{msgId}"/>
    <Txn id="{txnId}" note="List Account Provider Fetch" refId="123456" refUrl="https://www.npci.org.in/" ts="{ts}" type="ListAccPvd"/>
</ns2:ReqListAccPvd>
```

---

## ReqValAdd – Name Enquiry (NameEnq)

**Path:** `{baseUrl}/imps/reqvaladd/{txnId}`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqValAdd xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head msgId="{msgId}" orgId="{orgId}" prodType="IMPS" ts="{ts}" ver="2.0"/>
    <Txn custRef="{custRef}" id="{txnId}" initiationMode="00" note="Name Enquiry Request" refId="{refId}"
         refUrl="https://www.npci.org.in/" ts="{ts}" type="NameEnq"/>
    <Payer addr="{orgId}@psp" code="4814" name="ENQUIRER NAME" seqNum="1" type="ENTITY">
        <Info>
            <Identity id="12110100020142|{payerIfsc}" type="BANK" verifiedName="ENQUIRER NAME"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>
        <Device>
            <Tag name="cardAccpTrId" value="DNB67667"/>
            <Tag name="cardAccIdCode" value="DNB917667667667"/>
            <Tag name="MOBILE" value="{mobNum}"/>
            <Tag name="LOCATION" value="HDFC BANK MOB7667667667IN"/>
            <Tag name="TYPE" value="MOB"/>
        </Device>
        <Ac addrType="MOBILE">
            <Detail name="MMID" value="{mmid}"/>
            <Detail name="MOBNUM" value="{mobNum}"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>
    </Payer>
    <Payee code="0000" seqNum="0" type="PERSON">
        <Ac addrType="ACCOUNT">
            <Detail name="IFSC" value="{payeeIfsc}"/>
            <Detail name="ACNUM" value="{payeeAcNum}"/>
            <Detail name="ACTYPE" value="DEFAULT"/>
        </Ac>
    </Payee>
</ns2:ReqValAdd>
```

---

## ReqValAdd – Address Validation (ValAdd)

**Path:** `{baseUrl}/imps/reqvaladd/{txnId}` — Txn type = ValAdd

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqValAdd xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head msgId="{msgId}" orgId="{orgId}" prodType="IMPS" ts="{ts}" ver="2.0"/>
    <Txn custRef="{custRef}" id="{txnId}" initiationMode="00" note="Address Validation" refId="{refId}"
         refUrl="https://www.npci.org.in/" ts="{ts}" type="ValAdd"/>
    <Payer addr="{orgId}@psp" code="4814" name="ENQUIRER NAME" seqNum="1" type="ENTITY">
        <Info>
            <Identity id="12110100020142|{payerIfsc}" type="BANK" verifiedName="ENQUIRER NAME"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>
        <Device>
            <Tag name="cardAccpTrId" value="DNB67667"/>
            <Tag name="cardAccIdCode" value="DNB917667667667"/>
            <Tag name="MOBILE" value="{mobNum}"/>
            <Tag name="LOCATION" value="HDFC BANK MOB7667667667IN"/>
            <Tag name="TYPE" value="MOB"/>
        </Device>
        <Ac addrType="MOBILE">
            <Detail name="MMID" value="{mmid}"/>
            <Detail name="MOBNUM" value="{mobNum}"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>
    </Payer>
    <Payee code="0000" seqNum="0" type="PERSON">
        <Ac addrType="ACCOUNT">
            <Detail name="IFSC" value="{payeeIfsc}"/>
            <Detail name="ACNUM" value="{payeeAcNum}"/>
            <Detail name="ACTYPE" value="DEFAULT"/>
        </Ac>
    </Payee>
</ns2:ReqValAdd>
```

---

## Quick fill example

```
{baseUrl}     = https://192.168.1.38:8443
{orgId}       = BANK01
{msgId}       = BAN5t2Dk18UFMIMFENLBga12345678901234
{txnId}       = BAN5t2Dk18UFMIMFENLBgb12345678901234
{ts}          = 2026-01-25T10:30:00.000+05:30
{payerAcNum}  = 1234567890123456
{payerIfsc}   = HDFC0000001
{payerName}   = Rugved Kharde
{payerMobile} = 919494916511
{payeeAcNum}  = 1111222233334444
{payeeIfsc}   = ICIC0000001
{amount}      = 1000.00
{custRef}     = 023113001276
{refId}       = 001276
{orgTxnId}    = BAN5t2Dk18UFMIMG40acxy12345678901234
{orgRrn}      = 023113001279
{orgTxnDate}  = 2026-01-25T10:43:31.000+05:30
{initiationMode} = 00
{mmid}        = 4002111
{mobNum}      = 919494916511
```
