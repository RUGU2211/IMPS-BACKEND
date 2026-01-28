# IMPS System – Final Flow Chart

## 1. High-Level Components

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐     ┌──────────────────┐
│  Postman /      │     │  IMPS Backend    │     │  Mock Switch    │     │  NPCI Mock       │
│  NPCI (XML)     │────▶│  Port 8081       │────▶│  Port 8082      │     │  Client 8083     │
│                 │     │  (XML ↔ ISO)     │◀────│  (Auto-reply)   │     │  (optional)      │
└─────────────────┘     └──────────────────┘     └─────────────────┘     └──────────────────┘
```

---

## 2. Request Flow (NPCI → Backend → Switch)

```
                    POST /npci/reqpay/2.1 (XML)
                    POST /npci/reqchktxn/2.1
                    POST /npci/reqhbt/2.1
                    POST /npci/reqvaladd/2.1
                    POST /npci/reqlistaccpvd/2.1
                                    │
                                    ▼
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│  IMPS BACKEND                                                                              │
│  1. Receive XML → Audit (message_audit_log)                                                │
│  2. Create/update transaction (txn_type, switch_status = INIT)                              │
│  3. Convert XML → ISO (XmlToIsoConverter)                                                  │
│  4. markIsoSent(txn) → switch_status = ISO_SENT, req_out_date_time                         │
│  5. Send ISO to Mock Switch (SwitchClient)                                                 │
└───────────────────────────────────────────────────────────────────────────────────────────┘
                                    │
                    POST /switch/reqpay/2.1 (ISO)
                    POST /switch/reqchktxn/2.1
                    POST /switch/reqhbt/2.1
                    POST /switch/reqvaladd/2.1
                    POST /switch/reqlistaccpvd/2.1
                                    │
                                    ▼
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│  MOCK SWITCH (Auto-reply ON, Trigger OFF)                                                 │
│  1. Receive ISO → Log, validate                                                           │
│  2. After delay (500ms) → Build response ISO (copy DE120, DE39, etc.)                      │
│  3. POST response ISO to IMPS Backend                                                      │
└───────────────────────────────────────────────────────────────────────────────────────────┘
                                    │
                    POST /switch/resppay/2.1 (ISO)
                    POST /switch/respchktxn/2.1
                    ...
                                    │
                                    ▼
```

---

## 3. Response Flow (Switch → Backend → NPCI)

```
                    POST /switch/resppay/2.1 (ISO)
                    POST /switch/respchktxn/2.1
                    POST /switch/resphbt/2.1
                    POST /switch/respvaladd/2.1
                    POST /switch/resplistaccpvd/2.1
                                    │
                                    ▼
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│  IMPS BACKEND                                                                              │
│  1. Unpack ISO → DE120 (txnId), DE39 (resp code), DE38 (approval)                          │
│  2. Convert ISO → XML (IsoToXmlConverter)                                                  │
│  3. findOptionalByTxnId(DE120) → markSuccess(txn, xml, ...) or markFailure(txn, xml)     │
│     → switch_status = SUCCESS | FAILED                                                     │
│     → resp_xml, resp_in_date_time, resp_out_date_time set                                 │
│  4. (Optional) POST XML to NPCI Mock Client http://localhost:8083/npci/resppay/2.1        │
│     → If 8083 not running: log "Connection refused", continue                              │
└───────────────────────────────────────────────────────────────────────────────────────────┘
                                    │
                    Optional: POST /npci/resppay/2.1 (XML) to 8083
                                    │
                                    ▼
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│  NPCI MOCK CLIENT (Port 8083) – optional                                                   │
│  Receives XML response; returns ACK.                                                       │
└───────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Transaction Table – Status Flow

```
  ┌─────────┐       markIsoSent()        ┌─────────────┐      Response received
  │  INIT   │ ────────────────────────▶ │  ISO_SENT   │ ──────────────────────▶
  └─────────┘   (req sent to switch)     └─────────────┘   (DE120 found, update txn)
        │                                        │
        │ createRequest(txnId, xml, txnType)    │
        │ PAY | CHKTXN | HBT | VALADD            │    markSuccess()     ┌───────────┐
        │                                        └─────────────────────▶│  SUCCESS  │
        │                                        │                      └───────────┘
        │                                        │    markFailure()     ┌───────────┐
        │                                        └─────────────────────▶│  FAILED   │
        │                                                               └───────────┘
```

| Column              | When set |
|---------------------|----------|
| req_in_date_time    | createRequest() |
| req_out_date_time   | markIsoSent() |
| resp_in_date_time   | markSuccess() / markFailure() |
| resp_out_date_time  | markSuccess() / markFailure() |
| resp_xml            | markSuccess(txn, xml, ...) / markFailure(txn, xml) |
| switch_status       | INIT → ISO_SENT → SUCCESS \| FAILED |

---

## 5. API Types – End-to-End Flow

| API          | NPCI/Postman → Backend      | Backend → Switch        | Switch auto-reply     | Backend → NPCI Mock (8083) |
|--------------|-----------------------------|--------------------------|------------------------|----------------------------|
| ReqPay       | POST /npci/reqpay/2.1 (XML) | POST /switch/reqpay/2.1  | RespPay 0210 (DE120)   | POST /npci/resppay/2.1     |
| ReqChkTxn    | POST /npci/reqchktxn/2.1    | POST /switch/reqchktxn/2.1 | RespChkTxn 0210      | POST /npci/respchktxn/2.1  |
| ReqHbt       | POST /npci/reqhbt/2.1       | POST /switch/reqhbt/2.1  | RespHbt 0810           | POST /npci/resphbt/2.1     |
| ReqValAdd    | POST /npci/reqvaladd/2.1    | POST /switch/reqvaladd/2.1 | RespValAdd 0210 (DE120) | POST /npci/respvaladd/2.1 |
| ReqListAccPvd| POST /npci/reqlistaccpvd/2.1 | POST /switch/reqlistaccpvd/2.1 | RespListAccPvd 0210 | POST /npci/resplistaccpvd/2.1 |

---

## 6. Ports Summary

| Service           | Port | Role |
|-------------------|------|------|
| IMPS Backend      | 8081 | Receives NPCI XML / Switch ISO; converts; DB; calls Switch & NPCI Mock |
| Mock Switch       | 8082 | Receives request ISO; auto-replies with response ISO to Backend |
| NPCI Mock Client  | 8083 | Optional; receives response XML from Backend |

---

## 7. One-Line Flow (ReqPay Example)

```
Postman (ReqPay XML) → Backend 8081 → [create txn INIT, XML→ISO, markIsoSent] → Switch 8082
→ Switch auto-reply (RespPay ISO, DE120) → Backend 8081 → [find txn, markSuccess, resp_xml, resp_out_date_time]
→ (optional) NPCI Mock 8083
```

This is the final flow for the IMPS system with Mock Switch auto-reply and transaction table updates.
