# IMPS Request APIs – Names, Paths, and Bodies

Source: Imps-backend/IMPS_API_Collection.postman_collection.json (REQ APIs only).

---

## ReqPay - P2A Fund Transfer (Remitter to NPCI)

**Path:** http://localhost:8081/npci/reqpay/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqPay xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0"
          ts="2026-01-25T10:30:00.000+05:30"
          orgId="{{orgId}}"
          msgId="5t2Dk18UFMIMFENLBga"
          prodType="IMPS"/>

    <Txn note="P2A Fund Transfer"
         custRef="023113001276"
         refId="001276"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T10:30:00.000+05:30"
         refCategory="00"
         type="PAY"
         purpose="00"
         initiationMode="00"
         id="NPCI000000005t2Dk18UFMIMFENLBgb"/>

    <Payer addr="{{orgId}}@psp"
           name="Rugved Kharde"
           seqNum="1"
           type="ENTITY"
           code="4814">

        <Info>
            <Identity id="1234567890123456|HDFC0000001"
                      type="BANK"
                      verifiedName="Rugved Kharde"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>

        <Device>
            <Tag name="MOBILE" value="919494916511"/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value="MOB"/>
            <Tag name="cardAccpTrId" value="NPC16511"/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>

        <Ac addrType="ACCOUNT">
            <Detail name="ACNUM" value="1234567890123456"/>
            <Detail name="IFSC" value="HDFC0000001"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>

        <Creds>
            <Cred subType="NA" type="PreApproved">
                <Data>MDB8QVBQUk9WRUQ</Data>
            </Cred>
        </Creds>

        <Amount value="1000.00" curr="INR"/>
    </Payer>

    <Payees>
        <Payee seqNum="0" type="PERSON" code="0000">
            <Amount value="1000.00" curr="INR"/>
            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value="ICIC0000001"/>
                <Detail name="ACTYPE" value="SAVINGS"/>
                <Detail name="ACNUM" value="1111222233334444"/>
            </Ac>
        </Payee>
    </Payees>
</ns2:ReqPay>
```

---

## ReqPay - P2P Fund Transfer (Chetan to Madhav)

**Path:** http://localhost:8081/npci/reqpay/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqPay xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0"
          ts="2026-01-25T10:30:00.000+05:30"
          orgId="{{orgId}}"
          msgId="5t2Dk18UFMIMGzOucfb"
          prodType="IMPS"/>

    <Txn note="P2P Fund Transfer"
         custRef="023113001284"
         refId="001284"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T10:30:00.000+05:30"
         refCategory="00"
         type="PAY"
         purpose="00"
         initiationMode="00"
         id="NPCI000000005t2Dk18UFMIMGzOucfc"/>

    <Payer addr="{{orgId}}@psp"
           name="Chetan Mokashi"
           seqNum="1"
           type="ENTITY"
           code="4814">

        <Info>
            <Identity id="9876543210987654|HDFC0000001"
                      type="BANK"
                      verifiedName="Chetan Mokashi"/>
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
            <Cred subType="NA" type="PreApproved">
                <Data>MDB8QVBQUk9WRUQ</Data>
            </Cred>
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

## ReqPay - FIR Transaction (Foreign Inward Remittance)

**Path:** http://localhost:8081/npci/reqpay/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqPay xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0"
          ts="2026-01-25T13:18:37.263+05:30"
          orgId="{{orgId}}"
          msgId="5t2Dk05P35IW2iSy2Pq"
          prodType="IMPS"/>

    <Txn note="Foreign Inward Remittance"
         custRef="112013002995"
         refId="002995"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T13:18:37.000+05:30"
         refCategory="00"
         type="PAY"
         purpose="00"
         initiationMode="12"
         id="rZzpW5Cnb"/>

    <Payer addr="{{orgId}}@psp"
           name="Rugved Kharde"
           seqNum="1"
           type="ENTITY"
           code="4814">

        <Info>
            <Identity id="1234567890123456|HDFC0000001"
                      type="BANK"
                      verifiedName="Rugved Kharde"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>

        <Device>
            <Tag name="MOBILE" value="919494916511"/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value="MOB"/>
            <Tag name="cardAccpTrId" value="NPC16511"/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>

        <Ac addrType="ACCOUNT">
            <Detail name="ACNUM" value="1234567890123456"/>
            <Detail name="IFSC" value="HDFC0000001"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>

        <Creds>
            <Cred subType="NA" type="PreApproved">
                <Data>MDB8QVBQUk9WRUQ</Data>
            </Cred>
        </Creds>

        <Amount value="10000.00" curr="INR"/>

        <Institution route="RDA" type="BANK">
            <Name value="TRANSWISE"
                  acNum="123456789456"
                  ifsc="PCIN0234123"/>
            <Purpose code="Credit to Beneficiary in INR"
                     note="Foreign Inward Remittance"/>
            <Originator name="John William"
                        refNo="1234567891"
                        type="INDIVIDUAL">
                <Address location="Marlin Apartment Limehouse London"/>
            </Originator>
            <Beneficiary name="Sajid Mulla"/>
        </Institution>
    </Payer>

    <Payees>
        <Payee seqNum="0" type="PERSON" code="0000">
            <Amount value="10000.00" curr="INR"/>
            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value="ICIC0000001"/>
                <Detail name="ACTYPE" value="SAVINGS"/>
                <Detail name="ACNUM" value="1111222233334444"/>
            </Ac>
        </Payee>
    </Payees>
</ns2:ReqPay>
```

---

## ReqPay - CREDIT (NPCI to Beneficiary Bank)

**Path:** http://localhost:8081/npci/reqpay/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqPay xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0"
          ts="2026-01-25T10:30:00+05:30"
          orgId="NPCI"
          msgId="5t2Dk05P35yQrXk0eCv"
          prodType="IMPS"/>

    <Txn note="Credit to Beneficiary"
         custRef="023113001276"
         refId="001276"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T10:30:00.000+05:30"
         refCategory="00"
         type="CREDIT"
         purpose="00"
         initiationMode="00"
         id="NPCI000000005t2Dk18UFMIMFENLBgb"/>

    <Payer addr="{{orgId}}@psp"
           name="Rugved Kharde"
           seqNum="1"
           type="ENTITY"
           code="4814">

        <Info>
            <Identity id="1234567890123456|HDFC0000001"
                      type="BANK"
                      verifiedName="Rugved Kharde"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>

        <Device>
            <Tag name="MOBILE" value="919494916511"/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value="MOB"/>
            <Tag name="cardAccpTrId" value="NPC16511"/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>

        <Ac addrType="ACCOUNT">
            <Detail name="ACNUM" value="1234567890123456"/>
            <Detail name="IFSC" value="HDFC0000001"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>

        <Creds>
            <Cred subType="NA" type="PreApproved">
                <Data>MDB8QVBQUk9WRUQ</Data>
            </Cred>
        </Creds>

        <Amount value="1000.00" curr="INR"/>
    </Payer>

    <Payees>
        <Payee seqNum="0" type="PERSON" code="0000">
            <Amount value="1000.00" curr="INR"/>
            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value="ICIC0000001"/>
                <Detail name="ACTYPE" value="SAVINGS"/>
                <Detail name="ACNUM" value="1111222233334444"/>
            </Ac>
        </Payee>
    </Payees>
</ns2:ReqPay>
```

---

## ReqChkTxn - Check Status Request

**Path:** http://localhost:8081/npci/reqchktxn/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqChkTxn xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0"
          ts="2026-01-25T10:44:03.976+05:30"
          orgId="{{orgId}}"
          msgId="5t2Dk18UFMIMG7zXPZV"
          prodType="IMPS"/>

    <Txn custRef="023113001279"
         id="NPCI000000005t2Dk18UFMIMG7zXPZX"
         initiationMode="00"
         note="Check Transaction Status"
         orgRrn="023113001279"
         orgTxnDate="2026-01-25T10:43:31.000+05:30"
         orgTxnId="NPCI000000005t2Dk18UFMIMG40acxy"
         purpose="00"
         refCategory="00"
         refId="001279"
         refUrl="https://www.npci.org.in/"
         subType="PAY"
         ts="2026-01-25T10:43:31.000+05:30"
         type="VR"/>

    <Payer addr="{{orgId}}@psp"
           name="Rugved Kharde"
           seqNum="1"
           type="ENTITY"
           code="4814">

        <Info>
            <Identity id="1234567890123456|HDFC0000001"
                      type="ACCOUNT"
                      verifiedName="Rugved Kharde"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>

        <Device>
            <Tag name="MOBILE" value="919494916511"/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value="MOB"/>
            <Tag name="cardAccpTrId" value="NPC16511"/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>

        <Ac addrType="ACCOUNT">
            <Detail name="ACNUM" value="1234567890123456"/>
            <Detail name="IFSC" value="HDFC0000001"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>

        <Amount value="1000.00" curr="INR"/>
    </Payer>

    <Payees>
        <Payee seqNum="0" type="PERSON" code="0000">
            <Amount value="1000.00" curr="INR"/>
            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value="ICIC0000001"/>
                <Detail name="ACTYPE" value="SAVINGS"/>
                <Detail name="ACNUM" value="1111222233334444"/>
            </Ac>
        </Payee>
    </Payees>
</ns2:ReqChkTxn>
```

---

## ReqChkTxn - NPCI to Beneficiary Bank

**Path:** http://localhost:8081/npci/reqchktxn/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqChkTxn xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0"
          ts="2026-01-25T10:44:04+05:30"
          orgId="NPCI"
          msgId="5t2Dk05P35yQsq6cTn6"
          prodType="IMPS"/>

    <Txn custRef="023113001279"
         id="NPCI000000005t2Dk18UFMIMG7zXPZX"
         initiationMode="00"
         note="Check Transaction Status"
         orgRrn="023113001279"
         orgTxnDate="2026-01-25T10:43:31.000+05:30"
         orgTxnId="NPCI000000005t2Dk18UFMIMG40acxy"
         purpose="00"
         refCategory="00"
         refId="001279"
         refUrl="https://www.npci.org.in/"
         subType="PAY"
         ts="2026-01-25T10:43:31.000+05:30"
         type="VR"/>

    <Payer addr="{{orgId}}@psp"
           name="Rugved Kharde"
           seqNum="1"
           type="ENTITY"
           code="4814">

        <Info>
            <Identity id="1234567890123456|HDFC0000001"
                      type="ACCOUNT"
                      verifiedName="Rugved Kharde"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>

        <Device>
            <Tag name="MOBILE" value="919494916511"/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value="MOB"/>
            <Tag name="cardAccpTrId" value="NPC16511"/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>

        <Ac addrType="ACCOUNT">
            <Detail name="ACNUM" value="1234567890123456"/>
            <Detail name="IFSC" value="HDFC0000001"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>

        <Amount value="1000.00" curr="INR"/>
    </Payer>

    <Payees>
        <Payee seqNum="0" type="PERSON" code="0000">
            <Amount value="1000.00" curr="INR"/>
            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value="ICIC0000001"/>
                <Detail name="ACTYPE" value="SAVINGS"/>
                <Detail name="ACNUM" value="1111222233334444"/>
            </Ac>
        </Payee>
    </Payees>
</ns2:ReqChkTxn>
```

---

## ReqHbt - Heartbeat ALIVE Request

**Path:** http://localhost:8081/npci/reqhbt/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<upi:ReqHbt xmlns:upi="http://npci.org/upi/schema/">
    <Head ver="1.0"
          ts="2026-01-25T10:30:00.000+05:30"
          orgId="{{orgId}}"
          msgId="HBT123456789012345678901234567890"/>
    <Txn id="HBT123456789012345678901234567890"
         note="Heartbeat Check"
         refId="123456"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T10:30:00.000+05:30"
         type="Hbt"/>
    <HbtMsg type="ALIVE" value="NA"/>
</upi:ReqHbt>
```

---

## ReqHbt - Heartbeat EOD Request

**Path:** http://localhost:8081/npci/reqhbt/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<upi:ReqHbt xmlns:upi="http://npci.org/upi/schema/">
    <Head ver="1.0"
          ts="2026-01-25T23:59:00.000+05:30"
          orgId="NPCI"
          msgId="EOD123456789012345678901234567890"/>
    <Txn id="EOD123456789012345678901234567890"
         note="End of Day Signal"
         refId="123456"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T23:59:00.000+05:30"
         type="Hbt"/>
    <HbtMsg type="EOD" value="2026-01-25"/>
</upi:ReqHbt>
```

---

## ReqListAccPvd - List Providers Request

**Path:** http://localhost:8081/npci/reqlistaccpvd/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqListAccPvd xmlns:ns2="http://npci.org/upi/schema/">
    <Head ver="2.0"
          ts="2026-01-25T23:40:15.000+05:30"
          orgId="{{orgId}}"
          msgId="PNB4a69d250abe6433899c2f5a08fc0qw12"/>
    <Txn id="PNB4a69d250abe6433899c2f5e7dad71c12"
         note="List Account Provider Fetch"
         refId="123456"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T23:40:10.000+05:30"
         type="ListAccPvd"/>
</ns2:ReqListAccPvd>
```

---

## ReqValAdd - Name Enquiry Request

**Path:** http://localhost:8081/npci/reqvaladd/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqValAdd xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head msgId="5t2Dk18UFMIRH8K84fV"
          orgId="{{orgId}}"
          prodType="IMPS"
          ts="2026-01-25T14:48:23.373+05:30"
          ver="2.0"/>

    <Txn custRef="023314480540"
         id="NPCI000000005t2Dk18UFMIRH8K84fW"
         initiationMode="00"
         note="Name Enquiry Request"
         refId="860454"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T14:48:01.000+05:30"
         type="NameEnq"/>

    <Payer addr="{{orgId}}@psp"
           code="4814"
           name="ENQUIRER NAME"
           seqNum="1"
           type="ENTITY">

        <Info>
            <Identity id="12110100020142|HDFC0000001"
                      type="BANK"
                      verifiedName="ENQUIRER NAME"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>

        <Device>
            <Tag name="cardAccpTrId" value="DNB67667"/>
            <Tag name="cardAccIdCode" value="DNB917667667667"/>
            <Tag name="MOBILE" value="917667667667"/>
            <Tag name="LOCATION" value="HDFC BANK MOB7667667667IN"/>
            <Tag name="TYPE" value="MOB"/>
        </Device>

        <Ac addrType="MOBILE">
            <Detail name="MMID" value="4002111"/>
            <Detail name="MOBNUM" value="919494916511"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>
    </Payer>

    <Payee code="0000" seqNum="0" type="PERSON">
        <Ac addrType="ACCOUNT">
            <Detail name="IFSC" value="ICIC0000001"/>
            <Detail name="ACNUM" value="1111222233334444"/>
            <Detail name="ACTYPE" value="DEFAULT"/>
        </Ac>
    </Payee>
</ns2:ReqValAdd>
```

---

## ReqValAdd - ValAdd Type Request

**Path:** http://localhost:8081/npci/reqvaladd/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqValAdd xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head msgId="5t2Dk18UFMIRH8K84fV"
          orgId="{{orgId}}"
          prodType="IMPS"
          ts="2026-01-25T14:48:23.373+05:30"
          ver="2.0"/>

    <Txn custRef="023314480540"
         id="NPCI000000005t2Dk18UFMIRH8K84fW"
         initiationMode="00"
         note="Address Validation"
         refId="860454"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T14:48:01.000+05:30"
         type="ValAdd"/>

    <Payer addr="{{orgId}}@psp"
           code="4814"
           name="ENQUIRER NAME"
           seqNum="1"
           type="ENTITY">

        <Info>
            <Identity id="12110100020142|HDFC0000001"
                      type="BANK"
                      verifiedName="ENQUIRER NAME"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>

        <Device>
            <Tag name="cardAccpTrId" value="DNB67667"/>
            <Tag name="cardAccIdCode" value="DNB917667667667"/>
            <Tag name="MOBILE" value="917667667667"/>
            <Tag name="LOCATION" value="HDFC BANK MOB7667667667IN"/>
            <Tag name="TYPE" value="MOB"/>
        </Device>

        <Ac addrType="MOBILE">
            <Detail name="MMID" value="4002111"/>
            <Detail name="MOBNUM" value="919494916511"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>
    </Payer>

    <Payee code="0000" seqNum="0" type="PERSON">
        <Ac addrType="ACCOUNT">
            <Detail name="IFSC" value="ICIC0000001"/>
            <Detail name="ACNUM" value="1111222233334444"/>
            <Detail name="ACTYPE" value="DEFAULT"/>
        </Ac>
    </Payee>
</ns2:ReqValAdd>
```

---

## ReqPay - Fund Transfer Request (to Switch)

**Path:** http://localhost:8082/switch/reqpay/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqPay xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0"
          ts="2026-01-25T10:30:00.000+05:30"
          orgId="{{orgId}}"
          msgId="5t2Dk18UFMIMFENLBga"
          prodType="IMPS"/>

    <Txn note="P2A Fund Transfer"
         custRef="023113001276"
         refId="001276"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T10:30:00.000+05:30"
         refCategory="00"
         type="PAY"
         purpose="00"
         initiationMode="00"
         id="NPCI000000005t2Dk18UFMIMFENLBgb"/>

    <Payer addr="{{orgId}}@psp"
           name="Rugved Kharde"
           seqNum="1"
           type="ENTITY"
           code="4814">

        <Info>
            <Identity id="1234567890123456|HDFC0000001"
                      type="BANK"
                      verifiedName="Rugved Kharde"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>

        <Device>
            <Tag name="MOBILE" value="919494916511"/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value="MOB"/>
            <Tag name="cardAccpTrId" value="NPC16511"/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>

        <Ac addrType="ACCOUNT">
            <Detail name="ACNUM" value="1234567890123456"/>
            <Detail name="IFSC" value="HDFC0000001"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>

        <Creds>
            <Cred subType="NA" type="PreApproved">
                <Data>MDB8QVBQUk9WRUQ</Data>
            </Cred>
        </Creds>

        <Amount value="1000.00" curr="INR"/>
    </Payer>

    <Payees>
        <Payee seqNum="0" type="PERSON" code="0000">
            <Amount value="1000.00" curr="INR"/>
            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value="ICIC0000001"/>
                <Detail name="ACTYPE" value="SAVINGS"/>
                <Detail name="ACNUM" value="1111222233334444"/>
            </Ac>
        </Payee>
    </Payees>
</ns2:ReqPay>
```

---

## ReqPay - CREDIT Request (to Switch)

**Path:** http://localhost:8082/switch/reqpay/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqPay xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0"
          ts="2026-01-25T10:30:00+05:30"
          orgId="NPCI"
          msgId="5t2Dk05P35yQrXk0eCv"
          prodType="IMPS"/>

    <Txn note="Credit to Beneficiary"
         custRef="023113001276"
         refId="001276"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T10:30:00.000+05:30"
         refCategory="00"
         type="CREDIT"
         purpose="00"
         initiationMode="00"
         id="NPCI000000005t2Dk18UFMIMFENLBgb"/>

    <Payer addr="{{orgId}}@psp"
           name="Rugved Kharde"
           seqNum="1"
           type="ENTITY"
           code="4814">

        <Info>
            <Identity id="1234567890123456|HDFC0000001"
                      type="BANK"
                      verifiedName="Rugved Kharde"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>

        <Device>
            <Tag name="MOBILE" value="919494916511"/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value="MOB"/>
            <Tag name="cardAccpTrId" value="NPC16511"/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>

        <Ac addrType="ACCOUNT">
            <Detail name="ACNUM" value="1234567890123456"/>
            <Detail name="IFSC" value="HDFC0000001"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>

        <Creds>
            <Cred subType="NA" type="PreApproved">
                <Data>MDB8QVBQUk9WRUQ</Data>
            </Cred>
        </Creds>

        <Amount value="1000.00" curr="INR"/>
    </Payer>

    <Payees>
        <Payee seqNum="0" type="PERSON" code="0000">
            <Amount value="1000.00" curr="INR"/>
            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value="ICIC0000001"/>
                <Detail name="ACTYPE" value="SAVINGS"/>
                <Detail name="ACNUM" value="1111222233334444"/>
            </Ac>
        </Payee>
    </Payees>
</ns2:ReqPay>
```

---

## ReqChkTxn - Check Status Request (to Switch)

**Path:** http://localhost:8082/switch/reqchktxn/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqChkTxn xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head ver="2.0"
          ts="2026-01-25T10:44:03.976+05:30"
          orgId="{{orgId}}"
          msgId="5t2Dk18UFMIMG7zXPZV"
          prodType="IMPS"/>

    <Txn custRef="023113001279"
         id="NPCI000000005t2Dk18UFMIMG7zXPZX"
         initiationMode="00"
         note="Check Transaction Status"
         orgRrn="023113001279"
         orgTxnDate="2026-01-25T10:43:31.000+05:30"
         orgTxnId="NPCI000000005t2Dk18UFMIMG40acxy"
         purpose="00"
         refCategory="00"
         refId="001279"
         refUrl="https://www.npci.org.in/"
         subType="PAY"
         ts="2026-01-25T10:43:31.000+05:30"
         type="VR"/>

    <Payer addr="{{orgId}}@psp"
           name="Rugved Kharde"
           seqNum="1"
           type="ENTITY"
           code="4814">

        <Info>
            <Identity id="1234567890123456|HDFC0000001"
                      type="ACCOUNT"
                      verifiedName="Rugved Kharde"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>

        <Device>
            <Tag name="MOBILE" value="919494916511"/>
            <Tag name="LOCATION" value=""/>
            <Tag name="TYPE" value="MOB"/>
            <Tag name="cardAccpTrId" value="NPC16511"/>
            <Tag name="cardAccIdCode" value=""/>
        </Device>

        <Ac addrType="ACCOUNT">
            <Detail name="ACNUM" value="1234567890123456"/>
            <Detail name="IFSC" value="HDFC0000001"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>

        <Amount value="1000.00" curr="INR"/>
    </Payer>

    <Payees>
        <Payee seqNum="0" type="PERSON" code="0000">
            <Amount value="1000.00" curr="INR"/>
            <Ac addrType="ACCOUNT">
                <Detail name="IFSC" value="ICIC0000001"/>
                <Detail name="ACTYPE" value="SAVINGS"/>
                <Detail name="ACNUM" value="1111222233334444"/>
            </Ac>
        </Payee>
    </Payees>
</ns2:ReqChkTxn>
```

---

## ReqHbt - Heartbeat Request (to Switch)

**Path:** http://localhost:8082/switch/reqhbt/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<upi:ReqHbt xmlns:upi="http://npci.org/upi/schema/">
    <Head ver="1.0"
          ts="2026-01-25T10:30:00.000+05:30"
          orgId="{{orgId}}"
          msgId="HBT123456789012345678901234567890"/>
    <Txn id="HBT123456789012345678901234567890"
         note="Heartbeat Check"
         refId="123456"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T10:30:00.000+05:30"
         type="Hbt"/>
    <HbtMsg type="ALIVE" value="NA"/>
</upi:ReqHbt>
```

---

## ReqListAccPvd - List Providers Request (to Switch)

**Path:** http://localhost:8082/switch/reqlistaccpvd/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqListAccPvd xmlns:ns2="http://npci.org/upi/schema/">
    <Head ver="2.0"
          ts="2026-01-25T23:40:15.000+05:30"
          orgId="{{orgId}}"
          msgId="PNB4a69d250abe6433899c2f5a08fc0qw12"/>
    <Txn id="PNB4a69d250abe6433899c2f5e7dad71c12"
         note="List Account Provider Fetch"
         refId="123456"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T23:40:10.000+05:30"
         type="ListAccPvd"/>
</ns2:ReqListAccPvd>
```

---

## ReqValAdd - Name Enquiry Request (to Switch)

**Path:** http://localhost:8082/switch/reqvaladd/2.1

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:ReqValAdd xmlns:ns2="http://npci.org/upi/schema/" xmlns:ns3="http://npci.org/cm/schema/">
    <Head msgId="5t2Dk18UFMIRH8K84fV"
          orgId="{{orgId}}"
          prodType="IMPS"
          ts="2026-01-25T14:48:23.373+05:30"
          ver="2.0"/>

    <Txn custRef="023314480540"
         id="NPCI000000005t2Dk18UFMIRH8K84fW"
         initiationMode="00"
         note="Name Enquiry Request"
         refId="860454"
         refUrl="https://www.npci.org.in/"
         ts="2026-01-25T14:48:01.000+05:30"
         type="NameEnq"/>

    <Payer addr="{{orgId}}@psp"
           code="4814"
           name="ENQUIRER NAME"
           seqNum="1"
           type="ENTITY">

        <Info>
            <Identity id="12110100020142|HDFC0000001"
                      type="BANK"
                      verifiedName="ENQUIRER NAME"/>
            <Rating verifiedAddress="TRUE"/>
        </Info>

        <Device>
            <Tag name="cardAccpTrId" value="DNB67667"/>
            <Tag name="cardAccIdCode" value="DNB917667667667"/>
            <Tag name="MOBILE" value="917667667667"/>
            <Tag name="LOCATION" value="HDFC BANK MOB7667667667IN"/>
            <Tag name="TYPE" value="MOB"/>
        </Device>

        <Ac addrType="MOBILE">
            <Detail name="MMID" value="4002111"/>
            <Detail name="MOBNUM" value="919494916511"/>
            <Detail name="ACTYPE" value="SAVINGS"/>
        </Ac>
    </Payer>

    <Payee code="0000" seqNum="0" type="PERSON">
        <Ac addrType="ACCOUNT">
            <Detail name="IFSC" value="ICIC0000001"/>
            <Detail name="ACNUM" value="1111222233334444"/>
            <Detail name="ACTYPE" value="DEFAULT"/>
        </Ac>
    </Payee>
</ns2:ReqValAdd>
```
