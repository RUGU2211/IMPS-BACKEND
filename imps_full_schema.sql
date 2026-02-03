-- ============================================
-- IMPS Full Schema - Drop All, Create All, Insert Data
-- ============================================
-- Database: imps_db (PostgreSQL)
--
-- Usage:
--   1. Create DB (if needed): psql -U postgres -c "CREATE DATABASE imps_db;"
--   2. Run this script:       psql -U postgres -d imps_db -f imps_full_schema.sql
--
-- This script: drops all tables, recreates them, and inserts data required
-- for IMPS Backend and Mock Switch (Pay, ChkTxn, Hbt, ValAdd, ListAccPvd, audit_log).
-- ============================================

-- ============================================
-- PART 1: DROP ALL TABLES (reverse order of creation)
-- ============================================
DROP TABLE IF EXISTS response_xpath CASCADE;
DROP TABLE IF EXISTS xml_path_req_pay CASCADE;
DROP TABLE IF EXISTS account_type_mapping CASCADE;
DROP TABLE IF EXISTS message_audit_log CASCADE;
DROP TABLE IF EXISTS transaction CASCADE;
DROP TABLE IF EXISTS account_master CASCADE;
DROP TABLE IF EXISTS institution_master CASCADE;
DROP TABLE IF EXISTS audit_log CASCADE;

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

-- 3. ACCOUNT_MASTER (shared for Switch/NPCI validation)
CREATE TABLE account_master (
    account_id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL,
    ifsc_code VARCHAR(11) NOT NULL,
    account_holder_name VARCHAR(100),
    account_type VARCHAR(10),
    available_balance DECIMAL(18,2) DEFAULT 0,
    account_status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    imps_enabled CHAR(1) NOT NULL DEFAULT 'Y',
    upi_enabled CHAR(1) NOT NULL DEFAULT 'Y',
    daily_txn_limit DECIMAL(18,2),
    last_txn_rrn VARCHAR(20),
    last_updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_account_number ON account_master(account_number);
CREATE INDEX idx_account_ifsc ON account_master(ifsc_code);
CREATE INDEX idx_account_status_imps ON account_master(account_status, imps_enabled);
CREATE UNIQUE INDEX uk_account_ifsc ON account_master(account_number, ifsc_code);

-- 4. INSTITUTION_MASTER (IMPS validation)
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
    active BOOLEAN NOT NULL DEFAULT true
);
CREATE INDEX idx_institution_bank_code ON institution_master(bank_code);
CREATE INDEX idx_institution_ifsc ON institution_master(ifsc_code);
CREATE UNIQUE INDEX uk_institution_ifsc ON institution_master(ifsc_code);

-- 5. ACCOUNT_TYPE_MAPPING
CREATE TABLE account_type_mapping (
    id SERIAL PRIMARY KEY,
    acc_type VARCHAR(255),
    acc_type_iso_code VARCHAR(255)
);

-- 6. XML_PATH_REQ_PAY
CREATE TABLE xml_path_req_pay (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255),
    status VARCHAR(255),
    sub_field VARCHAR(255),
    type VARCHAR(255),
    value VARCHAR(255),
    x_path VARCHAR(255)
);

-- 7. RESPONSE_XPATH
CREATE TABLE response_xpath (
    id INTEGER PRIMARY KEY,
    status VARCHAR(255),
    type VARCHAR(255),
    value VARCHAR(255),
    xpath VARCHAR(255)
);

-- 8. AUDIT_LOG (Mock Switch - ISO message audit in imps_db)
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    direction VARCHAR(20) NOT NULL,
    api_type VARCHAR(50) NOT NULL,
    mti VARCHAR(10),
    transaction_id VARCHAR(100),
    rrn VARCHAR(50),
    stan VARCHAR(20),
    processing_code VARCHAR(10),
    amount VARCHAR(20),
    currency VARCHAR(5),
    response_code VARCHAR(10),
    approval_number VARCHAR(20),
    payer_account VARCHAR(50),
    payee_account VARCHAR(50),
    payer_ifsc VARCHAR(20),
    payee_ifsc VARCHAR(20),
    source_endpoint VARCHAR(255),
    destination_endpoint VARCHAR(255),
    raw_iso_hex TEXT,
    parsed_iso_json TEXT,
    http_status INTEGER,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    processing_time_ms BIGINT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);
CREATE INDEX idx_audit_log_txn_id ON audit_log(transaction_id);
CREATE INDEX idx_audit_log_rrn ON audit_log(rrn);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_log_api_type ON audit_log(api_type);
CREATE INDEX idx_audit_log_direction ON audit_log(direction);

-- ============================================
-- PART 3: INSERT DATA (required for APIs to work)
-- ============================================

-- INSTITUTION_MASTER (IMPS validation, ListAccPvd, routing)
INSERT INTO institution_master (aquirer_id, bank_code, name, switch_port, switch_ip, request_org_id, bin_code, ifsc_code, n_bin_code, active)
VALUES
  ('BANK01', 'HDFC', 'HDFC Bank', '8082', 'localhost', 'BANK01', 'HDFC0001', 'HDFC0000001', 'HDFC0000001', true),
  ('BANK02', 'ICIC', 'ICICI Bank', '8082', 'localhost', 'BANK02', 'ICIC0001', 'ICIC0000001', 'ICIC0000001', true),
  ('BANK03', 'SBIN', 'State Bank of India', '8082', 'localhost', 'BANK03', 'SBIN001', 'SBIN0000001', 'SBIN0000001', true);

-- ACCOUNT_MASTER (Switch/NPCI validation, ValAdd name enquiry)
-- API testing: use Sajid Mulla (1111222233334444@ICIC) and Madhav Shipure (5555666677778888@SBIN) as payee/payer pair.
INSERT INTO account_master (account_number, ifsc_code, account_holder_name, account_type, available_balance, account_status, imps_enabled, upi_enabled, daily_txn_limit, last_updated_time)
VALUES
  ('1234567890123456', 'HDFC0000001', 'Rugved Kharde', 'SB', 50000.00, 'ACTIVE', 'Y', 'Y', 100000.00, CURRENT_TIMESTAMP),
  ('9876543210987654', 'HDFC0000001', 'Chetan Mokashi', 'CA', 100000.00, 'ACTIVE', 'Y', 'Y', 500000.00, CURRENT_TIMESTAMP),
  ('1111222233334444', 'ICIC0000001', 'Sajid Mulla', 'SB', 25000.00, 'ACTIVE', 'Y', 'Y', 50000.00, CURRENT_TIMESTAMP),
  ('5555666677778888', 'SBIN0000001', 'Madhav Shipure', 'SB', 75000.00, 'ACTIVE', 'Y', 'Y', 200000.00, CURRENT_TIMESTAMP);

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
COMMENT ON TABLE account_master IS 'Shared for Switch/NPCI validation; IMPS uses institution_master';
COMMENT ON TABLE institution_master IS 'IMPS validation (IFSC, bank routing, ListAccPvd)';
COMMENT ON TABLE account_type_mapping IS 'Maps account types to ISO codes';
COMMENT ON TABLE xml_path_req_pay IS 'XPath config for ReqPay XML parsing';
COMMENT ON TABLE response_xpath IS 'XPath config for response parsing';
COMMENT ON TABLE audit_log IS 'Mock Switch: ISO 8583 message audit trail';

COMMENT ON COLUMN transaction.txn_id IS 'Unique transaction identifier';
COMMENT ON COLUMN transaction.switch_status IS 'INIT | ISO_SENT | SUCCESS | FAILED';
COMMENT ON COLUMN message_audit_log.stage IS 'NPCI_*_XML_IN, SWITCH_*_ISO_OUT, SWITCH_*_ISO_IN, NPCI_*_XML_OUT';

-- ============================================
-- VERIFICATION
-- ============================================
SELECT 'imps_full_schema completed successfully (8 tables)' AS status;
SELECT relname AS table_name FROM pg_class WHERE relkind = 'r' AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public') AND relname NOT LIKE 'pg_%' ORDER BY relname;
