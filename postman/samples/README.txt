Sample testing data for IMPS API Collection

Ports (HTTPS): IMPS 8443, Switch 8444, NPCI 8445. Configured via .env in each project.
Default profile: prod,ssl.

ISO 8583 binary (Switch ↔ IMPS)


Use these binary files when calling Switch - IMPS flow (Req*) and Switch - IMPS resp flow (Resp*) in Postman.  
In Postman: Body → Binary → Select file.

Request flow (Switch → IMPS, Req*)


File  MTI  Use for
`iso_reqpay.bin`  0200  ReqPay – fund transfer
`iso_reqchktxn.bin`  0200  ReqChkTxn – check transaction
`iso_reqhbt.bin`  0800  ReqHbt – heartbeat
`iso_reqlistaccpvd.bin`  0200  ReqListAccPvd – list providers
`iso_reqvaladd.bin`  0200  ReqValAdd – name enquiry

Reverse flow (Switch → IMPS, Resp*)


File  MTI  Use for
`iso_resppay.bin`  0210  RespPay – payment response
`iso_respchktxn.bin`  0210  RespChkTxn – check status response
`iso_resphbt.bin`  0810  RespHbt – heartbeat response
`iso_resplistaccpvd.bin`  0210  RespListAccPvd – list providers response
`iso_respvaladd.bin`  0210  RespValAdd – name enquiry response

Regenerate .bin files: From project root, run:
```bash
cd mock_switch
mvn compile exec:java "-Dexec.mainClass=com.hitachi.mockswitch.util.SampleIsoGenerator" "-Dexec.args=write"
```
This writes all Req* and Resp* samples to ../postman/samples/.  
Packager matches IMPS Backend (ImpsIsoPackager) for compatibility.

---

XML samples (NPCI ↔ IMPS)


- Req* (e.g. reqpay_sample.xml): use for NPCI - IMPS req flow in Postman. Replace {{txnId}} and {{msgId}} with collection variables or values (35 chars each).
- Resp* (resppay_sample.xml, etc.): reference only. IMPS sends responses to NPCI automatically; no separate Postman flow needed.

File  Use for
`reqpay_sample.xml`  ReqPay – NPCI → IMPS (P2A/P2P sample)
`resppay_sample.xml`  RespPay – reference (IMPS→NPCI is automatic)
`respchktxn_sample.xml`  RespChkTxn – reference
`resphbt_sample.xml`  RespHbt – reference
`resplistaccpvd_sample.xml`  RespListAccPvd – reference
`respvaladd_sample.xml`  RespValAdd – reference
