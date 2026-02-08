-- ============================================
-- IMPS Full Schema - Drop All, Create All, Insert Data
-- ============================================
-- Database: imps_db (PostgreSQL)
--
-- Usage:
--   1. Create DB (if needed): psql -U postgres -c "CREATE DATABASE imps_db;"
--   2. Run this script:       psql -U postgres -d imps_db -f imps_full_schema.sql
--
-- Tables: transaction, message_audit_log, institution_master, account_type_mapping, xml_path_req_pay, response_xpath.
-- account_master is in switch_db (see switch_full_schema.sql).
-- institution_master.request_org_id (BANK01/BANK02/BANK03) and ifsc_code match IMPS_Req_API_Bodies.md test data.
-- ============================================

-- ============================================
-- PART 1: DROP ALL TABLES (reverse order of creation)
-- ============================================
DROP TABLE IF EXISTS response_xpath CASCADE;
DROP TABLE IF EXISTS xml_path_req_pay CASCADE;
DROP TABLE IF EXISTS account_type_mapping CASCADE;
DROP TABLE IF EXISTS message_audit_log CASCADE;
DROP TABLE IF EXISTS transaction CASCADE;
DROP TABLE IF EXISTS institution_master CASCADE;

-- ============================================
-- PART 2: CREATE TABLES
-- ============================================

-- 1. TRANSACTION
CREATE TABLE transaction (
    id SERIAL PRIMARY KEY,
    txn_id VARCHAR(255),
    txn_type VARCHAR(255),
    req_in_date_time VARCHAR(255),
    req_out_date_time VARCHAR(255),
    resp_in_date_time VARCHAR(255),
    resp_out_date_time TIMESTAMP,
    req_xml TEXT,
    resp_xml TEXT,
    switch_status VARCHAR(255),
    de11 VARCHAR(255),
    de37 VARCHAR(255),
    de12 VARCHAR(255),
    de13 VARCHAR(255),
    approval_number VARCHAR(255)
);
CREATE INDEX idx_transaction_txn_id ON transaction(txn_id);
CREATE INDEX idx_transaction_status ON transaction(switch_status);
CREATE INDEX idx_transaction_de11_de37_de13 ON transaction(de11, de37, de13);

-- 2. MESSAGE_AUDIT_LOG
CREATE TABLE message_audit_log (
    id SERIAL PRIMARY KEY,
    txn_id VARCHAR(255),
    stage VARCHAR(255),
    raw_message TEXT,
    parsed_message TEXT,
    created_at TIMESTAMP
);
CREATE INDEX idx_message_audit_txn_id ON message_audit_log(txn_id);
CREATE INDEX idx_message_audit_stage ON message_audit_log(stage);
CREATE INDEX idx_message_audit_created_at ON message_audit_log(created_at);

-- 3. INSTITUTION_MASTER (IMPS validation, ListAccPvd, routing)
-- url, spoc_* = optional contact/URL for RespListAccPvd AccPvd tags
-- last_modified_ts = audit timestamp; also auto-set by JPA @PrePersist/@PreUpdate on save
CREATE TABLE institution_master (
    id SERIAL PRIMARY KEY,
    aquirer_id VARCHAR(255),
    bank_code VARCHAR(255),
    name VARCHAR(255),
    switch_port VARCHAR(255),
    switch_ip VARCHAR(255),
    request_org_id VARCHAR(255),
    bin_code VARCHAR(255),
    ifsc_code VARCHAR(255),
    n_bin_code VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT true,
    url VARCHAR(500),
    spoc_name VARCHAR(100),
    spoc_email VARCHAR(100),
    spoc_phone VARCHAR(20),
    last_modified_ts TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_institution_bank_code ON institution_master(bank_code);
CREATE INDEX idx_institution_ifsc ON institution_master(ifsc_code);
CREATE INDEX idx_institution_request_org_id ON institution_master(request_org_id);
CREATE UNIQUE INDEX uk_institution_ifsc ON institution_master(ifsc_code);

-- 4. ACCOUNT_TYPE_MAPPING
CREATE TABLE account_type_mapping (
    id SERIAL PRIMARY KEY,
    acc_type VARCHAR(255),
    acc_type_iso_code VARCHAR(255)
);

-- 5. XML_PATH_REQ_PAY
CREATE TABLE xml_path_req_pay (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255),
    status VARCHAR(255),
    sub_field VARCHAR(255),
    type VARCHAR(255),
    value VARCHAR(255),
    x_path VARCHAR(255)
);

-- 6. RESPONSE_XPATH
CREATE TABLE response_xpath (
    id INTEGER PRIMARY KEY,
    status VARCHAR(255),
    type VARCHAR(255),
    value VARCHAR(255),
    xpath VARCHAR(255)
);

-- ============================================
-- PART 3: INSERT DATA (required for APIs to work)
-- ============================================

-- INSTITUTION_MASTER (IMPS validation, ListAccPvd, routing)
-- request_org_id = Head @orgId from NPCI (BANK01/BANK02/BANK03). ifsc_code must match account_master in switch_db.
-- switch_port=9084 (socket). url, spoc_* = RespListAccPvd AccPvd tags. last_modified_ts = audit (JPA @PrePersist/@PreUpdate).
INSERT INTO institution_master (aquirer_id, bank_code, name, switch_port, switch_ip, request_org_id, bin_code, ifsc_code, n_bin_code, active, url, spoc_name, spoc_email, spoc_phone, last_modified_ts)
VALUES
  ('BANK01', 'HDFC', 'HDFC Bank', '9084', 'localhost', 'BANK01', 'HDFC0001', 'HDFC0000001', 'HDFC0000001', true, 'https://www.hdfcbank.com', 'HDFC IMPS Support', 'imps.support@hdfcbank.com', '+91-1800-419-4333', CURRENT_TIMESTAMP),
  ('BANK02', 'ICIC', 'ICICI Bank', '9084', 'localhost', 'BANK02', 'ICIC0001', 'ICIC0000001', 'ICIC0000001', true, 'https://www.icicibank.com', 'ICICI IMPS Support', 'imps.support@icicibank.com', '+91-1800-1080', CURRENT_TIMESTAMP),
  ('BANK03', 'SBIN', 'State Bank of India', '9084', 'localhost', 'BANK03', 'SBIN001', 'SBIN0000001', 'SBIN0000001', true, 'https://www.onlinesbi.sbi', 'SBI IMPS Support', 'imps.support@sbi.co.in', '+91-1800-1234', CURRENT_TIMESTAMP);

-- ACCOUNT_TYPE_MAPPING (ISO code mapping for IMPS - full set per NPCI spec)
INSERT INTO account_type_mapping (acc_type, acc_type_iso_code)
VALUES
  ('SAVINGS', '10'),
  ('SB', '10'),
  ('CURRENT', '20'),
  ('CA', '20'),
  ('DEFAULT', '00'),
  ('NRE', '30'),
  ('NRO', '40'),
  ('CREDIT', '50'),
  ('PPIWALLET', '60'),
  ('BANKWALLET', '70'),
  ('SOD', '80'),
  ('UOD', '81'),
  ('SEMICLOSEDPPIWALLET', '82'),
  ('SEMICLOSEDBANKWALLET', '83'),
  ('SNRR', '90');

-- XML_PATH_REQ_PAY (complete ReqPay XPath config - aligns with XmlUtil.parseReqPay keys)
INSERT INTO xml_path_req_pay (id, name, status, sub_field, type, value, x_path)
VALUES
  (1, 'msgId', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Head"]/@msgId'),
  (2, 'orgId', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Head"]/@orgId'),
  (3, 'ts', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Head"]/@ts'),
  (4, 'txnId', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Txn"]/@id'),
  (5, 'txnType', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Txn"]/@type'),
  (6, 'custRef', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Txn"]/@custRef'),
  (7, 'note', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Txn"]/@note'),
  (8, 'amount', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Payer"]//*[local-name()="Amount"]/@value'),
  (9, 'payer_acnum', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Payer"]//*[local-name()="Detail"][@name="ACNUM"]/@value'),
  (10, 'payer_ifsc', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Payer"]//*[local-name()="Detail"][@name="IFSC"]/@value'),
  (11, 'payer_actype', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Payer"]//*[local-name()="Detail"][@name="ACTYPE"]/@value'),
  (12, 'payer_name', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Payer"]/@name'),
  (13, 'payee_acnum', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Payee"]//*[local-name()="Detail"][@name="ACNUM"]/@value'),
  (14, 'payee_ifsc', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Payee"]//*[local-name()="Detail"][@name="IFSC"]/@value'),
  (15, 'payee_actype', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Payee"]//*[local-name()="Detail"][@name="ACTYPE"]/@value'),
  (16, 'payee_name', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Payee"]/@name');

-- RESPONSE_XPATH (RespPay parsing - aligns with XmlUtil.parseRespPay keys)
INSERT INTO response_xpath (id, status, type, value, xpath)
VALUES
  (1, 'ACTIVE', 'result', NULL, '//*[local-name()="Resp"]/@result'),
  (2, 'ACTIVE', 'reqMsgId', NULL, '//*[local-name()="Resp"]/@reqMsgId'),
  (3, 'ACTIVE', 'respCode', NULL, '//*[local-name()="Ref"]/@respCode'),
  (4, 'ACTIVE', 'approvalNum', NULL, '//*[local-name()="Ref"]/@approvalNum'),
  (5, 'ACTIVE', 'settAmount', NULL, '//*[local-name()="Ref"]/@settAmount'),
  (6, 'ACTIVE', 'acNum', NULL, '//*[local-name()="Ref"]/@acNum'),
  (7, 'ACTIVE', 'IFSC', NULL, '//*[local-name()="Ref"]/@IFSC'),
  (8, 'ACTIVE', 'regName', NULL, '//*[local-name()="Ref"]/@regName'),
  (9, 'ACTIVE', 'msgId', NULL, '//*[local-name()="Head"]/@msgId');

-- ============================================
-- PART 4: COMMENTS
-- ============================================
COMMENT ON TABLE transaction IS 'Stores all IMPS transaction details';
COMMENT ON TABLE message_audit_log IS 'Audit trail (4 stages per flow: npci_xml_in, switch_iso_out, switch_iso_in, npci_xml_out)';
COMMENT ON TABLE institution_master IS 'IMPS validation (IFSC, bank routing, ListAccPvd)';
COMMENT ON TABLE account_type_mapping IS 'Maps account types to ISO codes';
COMMENT ON TABLE xml_path_req_pay IS 'XPath config for ReqPay XML parsing';
COMMENT ON TABLE response_xpath IS 'XPath config for response parsing';

COMMENT ON COLUMN transaction.txn_id IS 'Unique transaction identifier';
COMMENT ON COLUMN transaction.switch_status IS 'INIT | ISO_SENT | SUCCESS | FAILED';
COMMENT ON COLUMN message_audit_log.stage IS 'NPCI_*_XML_IN, SWITCH_*_ISO_OUT, SWITCH_*_ISO_IN, NPCI_*_XML_OUT';

-- ============================================
-- VERIFICATION
-- ============================================
SELECT 'imps_full_schema completed successfully (6 tables)' AS status;
SELECT relname AS table_name FROM pg_class WHERE relkind = 'r' AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public') AND relname NOT LIKE 'pg_%' ORDER BY relname;
