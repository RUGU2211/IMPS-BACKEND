# NPCI IMPS Message Formats (Reference)

This document is the **authoritative format** for IMPS API request/response bodies. The Postman collection and project use these structures only.

---

## 6.1 ReqPay – Funds Transfer Request

```xml
<ns2:ReqPay xmlns:ns2="http://npci.org/upi/schema/"
            xmlns:ns3="http://npci.org/cm/schema/">

    <Head ver="2.0"
          ts=""
          orgId=""
          msgId=""
          prodType="IMPS"/>

    <Txn note=""
         custRef=""
         refId=""
         refUrl=""
         ts=""
         refCategory=""
         type="PAY|CREDIT"
         purpose=""
         initiationMode=""
         id="">
    </Txn>

    <Payer addr=""
           name=""
           seqNum=""
           type="PERSON|ENTITY"
           code="">

        <Info>
            <Identity id=""
                      type=""
                      verifiedName=""/>
            <Rating verifiedAddress="TRUE"/>
        </Info>

        <Device>
            <Tag name="MOBILE" value=""/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value=""/>
            <Tag name="cardAccpTrId" value=""/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>

        <Ac addrType="MOBILE">
            <Detail name="MMID" value=""/>
            <Detail name="MOBNUM" value=""/>
            <Detail name="ACTYPE"
                    value="SAVINGS|CURRENT|DEFAULT|NRE|NRO|CREDIT|PPIWALLET|
                           BANKWALLET|SOD|UOD|SEMICLOSEDPPIWALLET|
                           SEMICLOSEDBANKWALLET|SNRR"/>
        </Ac>

        <Creds>
            <Cred subType="NA" type="PreApproved">
                <Data>MDB8QVBQUk9WRUQ</Data>
            </Cred>
        </Creds>

        <Amount value=""
                curr="INR"/>
        
        <Institution route=""
                     type="">
            <Name acNum=""
                  ifsc=""
                  value=""/>
            <Purpose code=""
                     note=""/>
            <Originator name=""
                        refNo=""
                        type="">
                <Address location=""/>
            </Originator>
            <Beneficiary name=""/>
        </Institution>

    </Payer>

    <Payees>
        <Payee seqNum=""
               type=""
               code="">

            <Amount value=""
                    curr="INR"/>

            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value=""/>
                <Detail name="ACTYPE"
                        value="SAVINGS|CURRENT|DEFAULT|NRE|NRO|CREDIT|
                               PPIWALLET|BANKWALLET|SOD|UOD|
                               SEMICLOSEDPPIWALLET|SEMICLOSEDBANKWALLET|SNRR"/>
                <Detail name="ACNUM" value=""/>
            </Ac>

        </Payee>
    </Payees>

</ns2:ReqPay>
```

---

## ReqPay – Acknowledgement

```xml
<ns2:Ack xmlns:ns2="http://npci.org/upi/schema/"
         xmlns:ns3="http://npci.org/cm/schema/"
         api="ReqPay"
         reqMsgId=""
         ts="">
</ns2:Ack>
```

---

## 6.2 RespPay – Credit Response

```xml
<ns2:RespPay xmlns:upi="http://npci.org/upi/schema/">

    <Head ver="2.0"
          ts=""
          orgId=""
          msgId=""
          prodType="IMPS"/>

    <Txn id=""
         note="Test"
         refId=""
         custRef=""
         refUrl=""
         ts=""
         purpose=""
         type="PAY|CREDIT"
         subType="PAY"
         initiationMode=""
         refCategory=""/>

    <Resp reqMsgId=""
          result="">
        <Ref type="PAYEE"
             seqNum="1"
             addr=""
             regName=""
             acNum=""
             IFSC=""
             code=""
             accType=""
             settAmount=""
             orgAmount=""
             settCurrency="INR"
             approvalNum=""
             respCode=""/>
    </Resp>

</ns2:RespPay>
```

---

## RespPay – Acknowledgement

```xml
<ns2:Ack xmlns:ns2="http://npci.org/upi/schema/"
         xmlns:ns3="http://npci.org/cm/schema/"
         api="RespPay"
         reqMsgId=""
         ts="">
</ns2:Ack>
```

---

## 7.1 ReqChkTxn – Check Status Request

```xml
<ns2:ReqChkTxn xmlns:upi="http://npci.org/upi/schema/">

    <Head ver="2.0"
          ts=""
          orgId=""
          msgId=""
          prodType="IMPS"/>

    <Txn custRef=""
         id=""
         initiationMode=""
         note=""
         orgRrn=""
         orgTxnDate=""
         orgTxnId=""
         purpose=""
         refCategory=""
         refId=""
         refUrl=""
         subType=""
         ts=""
         type=""/>

    <Payer addr=""
           name=""
           seqNum=""
           type="PERSON|ENTITY"
           code="">

        <Info>
            <Identity id=""
                      type=""
                      verifiedName=""/>
            <Rating verifiedAddress="TRUE"/>
        </Info>

        <Device>
            <Tag name="MOBILE" value=""/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value=""/>
            <Tag name="cardAccpTrId" value=""/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>

        <Ac addrType="MOBILE">
            <Detail name="MMID" value=""/>
            <Detail name="MOBNUM" value=""/>
            <Detail name="ACTYPE"
                    value="SAVINGS|CURRENT|DEFAULT|NRE|NRO|CREDIT|
                           PPIWALLET|BANKWALLET|SOD|UOD|
                           SEMICLOSEDPPIWALLET|SEMICLOSEDBANKWALLET|SNRR"/>
        </Ac>

        <Amount value=""
                curr="INR"/>

    </Payer>

    <Payees>
        <Payee seqNum=""
               type=""
               code="">

            <Amount value=""
                    curr="INR"/>

            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value=""/>
                <Detail name="ACTYPE"
                        value="SAVINGS|CURRENT|DEFAULT|NRE|NRO|CREDIT|
                               PPIWALLET|BANKWALLET|SOD|UOD"/>
                <Detail name="ACNUM" value=""/>
            </Ac>

        </Payee>
    </Payees>

</ns2:ReqChkTxn>
```

---

## ReqChkTxn – Acknowledgement

```xml
<ns2:Ack xmlns:ns2="http://npci.org/upi/schema/"
         api="ReqChkTxn"
         reqMsgId=""
         ts=""/>
```

---

## RespChkTxn – Check Status Response

```xml
<ns2:RespChkTxn xmlns:upi="http://npci.org/upi/schema/">

    <Head ver="2.0"
          ts=""
          orgId=""
          msgId=""
          prodType="IMPS"/>

    <Txn id=""
         note=""
         refId=""
         refUrl=""
         refCategory=""
         ts=""
         type="ChkBankStatus"
         orgMsgId=""
         orgTxnId=""
         orgTxnDate=""
         initiationMode=""
         purpose=""
         subType=""
         custRef=""/>

    <Resp reqMsgId=""
          result="">
        <Ref type="PAYEE"
             seqNum=""
             addr=""
             settAmount=""
             orgAmount=""
             settCurrency=""
             acNum=""
             regName=""
             IFSC=""
             code=""
             accType=""
             approvalNum=""
             respCode=""/>
    </Resp>

</ns2:RespChkTxn>
```

---

*Source: NPCI IMPS specification. Use these formats only for API bodies in this project.*
