# COMMON CODE TECHNICAL SPECIFICATIONS

**Appendix – Rules**

---

## Existing Rules

The table below defines the **new tags added in IMPS** along with their rules and validations.

---

### Rule 019 – Head_Version

| Field                   | Details                                        |
| ----------------------- | ---------------------------------------------- |
| **Rule ID**             | 019_Head_Version                               |
| **Tag No**              | 2.1.1                                          |
| **Condition**           | General                                        |
| **Data Type**           | Numeric                                        |
| **Description / Value** | Default value should be **'1.0'** or **'2.0'** |

---

### Rule 020 – Head_ts

| Field                   | Details                                                                        |
| ----------------------- | ------------------------------------------------------------------------------ |
| **Rule ID**             | 020_Head_ts                                                                    |
| **Tag No**              | 2.1.2, 4.1.5                                                                   |
| **Condition**           | General                                                                        |
| **Data Type**           | ISO Date-Time Format                                                           |
| **Description / Value** | Timestamp must be in the following format:<br><br>**YYYY-MM-DDTHH:mm:ss.sssZ** |

#### Timestamp Format Explanation

* **YYYY-MM-DD** → Date (Year-Month-Day)
* **T** → Delimiter between date and time
* **HH:mm:ss.sss** → Time
  * **HH** → Hours (00–23, 24-hour format)
  * **mm** → Minutes (00–59)
  * **ss** → Seconds (00–59)
  * **sss** → Milliseconds (000–999)
* **Z** → Time zone indicator
* **±hh:mm** → Mandatory time zone difference from GMT

✅ **Note:** AM/PM format is **NOT allowed**

---

### Rule 021 – Head_MsgId

| Field                   | Details                                        |
| ----------------------- | ---------------------------------------------- |
| **Rule ID**             | 021_Head_MsgId                                 |
| **Tag No**              | 2.1.4                                          |
| **Condition**           | General                                        |
| **Data Type**           | Alphanumeric                                   |
| **Description / Value** | Message ID must be **unique for each API leg** |

#### Message ID Rules

* Total length must be **35 characters**
* **First 3 characters** → Bank Participation Code assigned by **NPCI**
* **Remaining 32 characters** → Generated using **UUID logic**

---

### Rule 022 – Txn_UUID

| Field                   | Details                                                 |
| ----------------------- | ------------------------------------------------------- |
| **Rule ID**             | 022_Txn_UUID                                            |
| **Tag No**              | 4.1.1                                                   |
| **Condition**           | General                                                 |
| **Data Type**           | Alphanumeric                                            |
| **Description / Value** | Transaction ID must be **unique for every transaction** |

#### Transaction UUID Rules

* Total length must be **35 characters**
* **First 3 characters** → Bank Participation Code assigned by **NPCI**
* **Remaining 32 characters** → Generated using **UUID logic**

---

### Rule 024 – Txn_code

| Field                   | Details                         |
| ----------------------- | ------------------------------- |
| **Rule ID**             | 024_Txn_code                    |
| **Tag No**              | 5.1.5, 6.2.5                    |
| **Condition**           | General                         |
| **Data Type**           | Alphanumeric                    |
| **Description / Value** | Transaction type classification |

#### Transaction Code Values

* **PERSON** → `0000`
* **ENTITY** → `XXXX`
  * `XXXX` represents the **MCC (Merchant Category Code)** of the merchant

---

### Rule 026 – Payer/Payee_InfoRating

| Field              | Details                    |
| ------------------ | -------------------------- |
| **Rule ID**        | 026_Payer/Payee_InfoRating |
| **Tag No**         | 5.6.1, 6.5.1               |
| **Condition**      | General                    |
| **Data Type**      | Numeric                    |
| **Allowed Values** | `TRUE` \| `FALSE`          |

---

### Rule 027 – Response_ErrCode

| Field               | Details                                  |
| ------------------- | ---------------------------------------- |
| **Rule ID**         | 027_Response_ErrCode                     |
| **Tag No**          | 11.1.3                                   |
| **Condition**       | General                                  |
| **Data Type**       | Alphanumeric                             |
| **Applicable When** | **Only if transaction result = FAILURE** |

---

### Rule 029 – Payer/Payee_Type

| Field              | Details              |
| ------------------ | -------------------- |
| **Rule ID**        | 029_Payer/Payee_Type |
| **Tag No**         | 5.1.4, 6.2.4         |
| **Condition**      | General              |
| **Data Type**      | Alphanumeric         |
| **Allowed Values** | `PERSON` / `ENTITY`  |

---

### Rule 032 – RespPay_RefTag_IFSC

| Field                   | Details                                 |
| ----------------------- | --------------------------------------- |
| **Rule ID**             | 032_RespPay_RefTag_IFSC                 |
| **Tag No**              | 11.2.12                                 |
| **Condition**           | If `Response.result = SUCCESS`          |
| **Data Type**           | IFSC                                    |
| **Description / Value** | IFSC code of the respective bank branch |
| **Validation**          | Must be **11 characters**               |

---

### Rule 034 – ReqPay_DeviceDetails_Values

| Field         | Details                         |
| ------------- | ------------------------------- |
| **Rule ID**   | 034_ReqPay_DeviceDetails_Values |
| **Tag No**    | 5.8.2                           |
| **Condition** | If `DEVICE` tag occurs          |
| **Data Type** | Device Values                   |

#### Device Details Format

* **MOBILE** → `91nnnnnnnnnn`
* **LOCATION** → Area with City, State & Country Code
  * Characters **01–23** → Terminal Address
  * Characters **24–36** → Terminal City
  * Characters **37–38** → Terminal State Code
  * Characters **39–40** → Terminal Country Code
* **TYPE** → Min Length: **1**, Max Length: **20** *(Refer Rule 035)*
* **ID** → Min Length: **1**, Max Length: **35**

---

### Rule 035 – ReqPay_DeviceDetails_type

| Field           | Details                       |
| --------------- | ----------------------------- |
| **Rule ID**     | 035_ReqPay_DeviceDetails_type |
| **Tag No**      | 5.8.2                         |
| **Condition**   | If `Device.tag.name = "Type"` |
| **Data Type**   | Device Type                   |
| **Description** | Initiating Channel            |

#### Allowed Device Types

1. **MOB** – Mobile
2. **INET** – Internet
3. **BRC**
4. **ATM**
5. **MAT**
6. **SMS**

---

### Rule 042 – ReqPay_Initiation_mode

| Field                    | Details                                                                                                                |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------- |
| **Rule ID**              | 042_ReqPay_Initiation_mode                                                                                             |
| **Tag No**               | 5.20                                                                                                                   |
| **Condition**            | If `initiationMode = "12"`                                                                                             |
| **Applicable Block**     | Payers Institution Block                                                                                               |
| **Description / Action** | This institution block must contain **all mandatory fields** mentioned in the **ReqPay table** as per **Tag No: 5.20** |

#### Applicability

* This XML block is applicable to:
  * **ReqPay**
  * **ReqAuthDetails**

---

### Rule 043 – ReqPay_Institution_type

| Field                    | Details                                                         |
| ------------------------ | --------------------------------------------------------------- |
| **Rule ID**              | 043_ReqPay_Institution_type                                     |
| **Tag No**               | 5.20.1                                                          |
| **Condition**            | If `type = "MTO"` or `type = "BANK"`                            |
| **Applicable Block**     | Payers Institution Type                                         |
| **Description / Action** | Only the following **payment institution types** are admissible |

#### Allowed Institution Types

1. **MTO** – Money Transfer Operator
2. **BANK**

---

### Rule 044 – ReqPay_Institution_route

| Field                    | Details                                              |
| ------------------------ | ---------------------------------------------------- |
| **Rule ID**              | 044_ReqPay_Institution_route                         |
| **Tag No**               | 5.20.2                                               |
| **Condition**            | If `route = "MTSS"` or `route = "RDA"`               |
| **Applicable Block**     | Payers Institution Route                             |
| **Description / Action** | Only the following **payment routes** are admissible |

#### Allowed Institution Routes

1. **MTSS** – Money Transfer Service Scheme
2. **RDA** – Rupee Drawing Arrangement

---

### Rule 048 – ReqPay_Ac_name_Account

| Field                    | Details                                                                |
| ------------------------ | ---------------------------------------------------------------------- |
| **Rule ID**              | 048_ReqPay_Ac_name_Account                                             |
| **Condition**            | If `addrType = ACCOUNT`                                                |
| **Applicable Block**     | Account Values                                                         |
| **Description / Action** | If address type is **ACCOUNT**, the following fields are **mandatory** |

#### Mandatory Account Details

* **IFSC**
  * Must be **11-character alphanumeric**
* **ACTYPE**
  * Fixed allowed values:
    * `SAVINGS`
    * `DEFAULT`
    * `CURRENT`
    * `NRE`
    * `NRO`
    * `PPIWALLET`
    * `BANKWALLET`
    * `CREDIT`
    * `SOD`
    * `UOD`
    * `SEMICLOSEDPPIWALLET`
    * `SEMICLOSEDBANKWALLET`
    * `SNRR`
* **ACNUM**
  * Maximum **30 digits**

---

### Rule 049 – ReqPay_Ac_name_Mobile

| Field                    | Details                                                               |
| ------------------------ | --------------------------------------------------------------------- |
| **Rule ID**              | 049_ReqPay_Ac_name_Mobile                                             |
| **Condition**            | If `addrType = MOBILE`                                                |
| **Applicable Block**     | Mobile Values                                                         |
| **Description / Action** | If address type is **MOBILE**, the following fields are **mandatory** |

#### Mandatory Mobile Details

* **MOBNUM**
  * Must be **10-digit numeric** with `+91` prefix
  * Total length: **12 characters**
* **MMID**
  * Must be **7-digit numeric**

---

### Rule 051 – ReqPay_Amount_Value

| Field                    | Details                                                          |
| ------------------------ | ---------------------------------------------------------------- |
| **Rule ID**              | 051_ReqPay_Amount_Value                                          |
| **Condition**            | If `amount`, `orgAmount`, `settAmount` present                   |
| **Applicable Block**     | Amount Value                                                     |
| **Data Type**            | Numeric                                                          |
| **Description / Action** | Amount value must be numeric and populated in **decimal format** |

#### Amount Format Rules

* Exactly **2 digits after decimal**
* Example:

  ```
  Amount Value = "100.00"
  ```

---

### Rule 052 – ReqPay_Txn_refCategory

| Field                    | Details                                                                    |
| ------------------------ | -------------------------------------------------------------------------- |
| **Rule ID**              | 052_ReqPay_Txn_refCategory                                                 |
| **Condition**            | If `txn_type = PAY` or `CREDIT` (Applicable for Mandate transactions also) |
| **Applicable Block**     | Transaction Reference Category                                             |
| **Allowed Values**       | `00` \| `01` \| `02` \| `03` \| `04` \| `05` \| `06` \| `07` \| `08` \| `09` |
| **Description / Action** | Identifies the **category of the transaction**                             |

#### Reference Category Codes

* `00` – NULL
* `01` – Advertisement
* `02` – Invoice
* `03–09` – Reserved for future use

---

### Note

> Rules **not applicable for IMPS** have been **excluded** from the above table.

---

## New Tags – Fixed Enumerations

| Sr. No. | UPI XML Tag / Attribute / Enumeration (New) | Description                | Data Type / Fixed Enumeration Values     |
| ------- | ------------------------------------------- | -------------------------- | ---------------------------------------- |
| 1       | **Head → prodType**                         | Product Identifier         | `UPI` \| `IMPS` \| `AEPS`                 |
| 2       | **name="type"**                             | Existing enumeration with newly added values under `value` | `WAP`, `IVR`, `ATM`, `BRC`, `MAT`, `SMS` |
| 3       | **Institution → Name → ifsc**               | New attribute to populate **IFSC / SWIFT code** for FIR    | Alphanumeric                             |
