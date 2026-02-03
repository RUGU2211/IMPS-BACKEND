-- ============================================
-- IMPS_DB Schema for IMPS Backend
-- ============================================
-- This script creates all required tables for the IMPS Backend.
-- Database: imps_db (PostgreSQL)
-- ============================================

-- ============================================
-- 1. TRANSACTION TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS transaction (
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

-- Indexes for transaction table
CREATE INDEX IF NOT EXISTS idx_transaction_txn_id ON transaction(txn_id);
CREATE INDEX IF NOT EXISTS idx_transaction_status ON transaction(switch_status);
CREATE INDEX IF NOT EXISTS idx_transaction_de11_de37_de13 ON transaction(de11, de37, de13);

-- ============================================
-- 2. MESSAGE_AUDIT_LOG TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS message_audit_log (
    id SERIAL PRIMARY KEY,
    txn_id VARCHAR(255),
    stage VARCHAR(255),
    raw_message TEXT,
    parsed_message TEXT,
    created_at TIMESTAMP
);

-- Indexes for message_audit_log table
CREATE INDEX IF NOT EXISTS idx_audit_txn_id ON message_audit_log(txn_id);
CREATE INDEX IF NOT EXISTS idx_audit_stage ON message_audit_log(stage);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON message_audit_log(created_at);

-- ============================================
-- 3. ACCOUNT_MASTER TABLE (shared for Switch/NPCI validation only)
-- IMPS uses institution_master for validation; account_master for account-level checks
-- ============================================
CREATE TABLE IF NOT EXISTS account_master (
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

-- Indexes for account_master table
CREATE INDEX IF NOT EXISTS idx_account_number ON account_master(account_number);
CREATE INDEX IF NOT EXISTS idx_account_ifsc ON account_master(ifsc_code);
CREATE INDEX IF NOT EXISTS idx_account_status_imps ON account_master(account_status, imps_enabled);
CREATE UNIQUE INDEX IF NOT EXISTS uk_account_ifsc ON account_master(account_number, ifsc_code);

-- ============================================
-- 4. INSTITUTION_MASTER TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS institution_master (
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

-- Indexes for institution_master table
CREATE INDEX IF NOT EXISTS idx_institution_bank_code ON institution_master(bank_code);
CREATE INDEX IF NOT EXISTS idx_institution_ifsc ON institution_master(ifsc_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_institution_ifsc ON institution_master(ifsc_code);

-- ============================================
-- 5. ACCOUNT_TYPE_MAPPING TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS account_type_mapping (
    id SERIAL PRIMARY KEY,
    acc_type VARCHAR(255),
    acc_type_iso_code VARCHAR(255)
);

-- ============================================
-- 6. XML_PATH_REQ_PAY TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS xml_path_req_pay (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255),
    status VARCHAR(255),
    sub_field VARCHAR(255),
    type VARCHAR(255),
    value VARCHAR(255),
    x_path VARCHAR(255)
);

-- ============================================
-- 7. RESPONSE_XPATH TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS response_xpath (
    id INTEGER PRIMARY KEY,
    status VARCHAR(255),
    type VARCHAR(255),
    value VARCHAR(255),
    xpath VARCHAR(255)
);

-- ============================================
-- TABLE COMMENTS
-- ============================================
COMMENT ON TABLE transaction IS 'Stores all IMPS transaction details';
COMMENT ON TABLE message_audit_log IS 'IMPS-only: Audit trail (exactly 4 stages per flow: npci_xml_in, switch_iso_out, switch_iso_in, npci_xml_out)';
COMMENT ON TABLE account_master IS 'Shared for Switch/NPCI validation only; IMPS uses institution_master';
COMMENT ON TABLE institution_master IS 'IMPS-only: used by IMPS for validation (IFSC, bank routing)';
COMMENT ON TABLE account_type_mapping IS 'Maps account types to ISO codes';
COMMENT ON TABLE xml_path_req_pay IS 'XPath configurations for XML parsing';
COMMENT ON TABLE response_xpath IS 'XPath configurations for response parsing';

-- ============================================
-- SAMPLE DATA (IMPS-only: institution_master, account_master for validation)
-- ============================================
-- institution_master: used by IMPS for validation (IFSC, bank routing)
INSERT INTO institution_master (aquirer_id, bank_code, name, switch_port, switch_ip, request_org_id, bin_code, ifsc_code, n_bin_code, active)
VALUES
  ('BANK01', 'HDFC', 'HDFC Bank', '8082', 'localhost', 'BANK01', 'HDFC0001', 'HDFC0000001', 'HDFC0000001', true),
  ('BANK02', 'ICIC', 'ICICI Bank', '8082', 'localhost', 'BANK02', 'ICIC0001', 'ICIC0000001', 'ICIC0000001', true)
ON CONFLICT (ifsc_code) DO NOTHING;

-- account_master: shared for Switch/NPCI validation (account-level checks)
INSERT INTO account_master (account_number, ifsc_code, account_holder_name, account_type, available_balance, account_status, imps_enabled, upi_enabled, daily_txn_limit, last_updated_time)
VALUES
  ('1234567890123456', 'HDFC0000001', 'John Doe', 'SB', 50000.00, 'ACTIVE', 'Y', 'Y', 100000.00, CURRENT_TIMESTAMP),
  ('9876543210987654', 'HDFC0000001', 'Jane Smith', 'CA', 100000.00, 'ACTIVE', 'Y', 'Y', 500000.00, CURRENT_TIMESTAMP),
  ('1111222233334444', 'ICIC0000001', 'Alice Kumar', 'SB', 25000.00, 'ACTIVE', 'Y', 'Y', 50000.00, CURRENT_TIMESTAMP)
ON CONFLICT (account_number, ifsc_code) DO NOTHING;

-- account_type_mapping: full set per NPCI spec (for ISO code lookup)
INSERT INTO account_type_mapping (id, acc_type, acc_type_iso_code)
VALUES
  (1, 'SAVINGS', '10'), (2, 'SB', '10'), (3, 'CURRENT', '20'), (4, 'CA', '20'), (5, 'DEFAULT', '00'),
  (6, 'NRE', '30'), (7, 'NRO', '40'), (8, 'CREDIT', '50'), (9, 'PPIWALLET', '60'), (10, 'BANKWALLET', '70'),
  (11, 'SOD', '80'), (12, 'UOD', '81'), (13, 'SEMICLOSEDPPIWALLET', '82'), (14, 'SEMICLOSEDBANKWALLET', '83'), (15, 'SNRR', '90')
ON CONFLICT (id) DO NOTHING;

-- xml_path_req_pay: complete ReqPay XPath config (aligns with XmlUtil.parseReqPay)
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
  (16, 'payee_name', 'ACTIVE', NULL, 'attr', NULL, '//*[local-name()="Payee"]/@name')
ON CONFLICT (id) DO NOTHING;

-- response_xpath: RespPay parsing (aligns with XmlUtil.parseRespPay)
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
  (9, 'ACTIVE', 'msgId', NULL, '//*[local-name()="Head"]/@msgId')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- COLUMN COMMENTS
-- ============================================
COMMENT ON COLUMN transaction.txn_id IS 'Unique transaction identifier';
COMMENT ON COLUMN transaction.txn_type IS 'Type: PAY, CREDIT, etc.';
COMMENT ON COLUMN transaction.switch_status IS 'Status: INIT → ISO_SENT → SUCCESS | FAILED (INIT=created, ISO_SENT=request sent to switch, SUCCESS/FAILED=response received)';
COMMENT ON COLUMN transaction.de11 IS 'ISO DE11 - STAN';
COMMENT ON COLUMN transaction.de37 IS 'ISO DE37 - RRN';
COMMENT ON COLUMN transaction.de12 IS 'ISO DE12 - Local Transaction Time';
COMMENT ON COLUMN transaction.de13 IS 'ISO DE13 - Local Transaction Date';

COMMENT ON COLUMN message_audit_log.stage IS 'Stage: only 4 per flow - NPCI_*_XML_IN, SWITCH_*_ISO_OUT, SWITCH_*_ISO_IN, NPCI_*_XML_OUT';
COMMENT ON COLUMN message_audit_log.raw_message IS 'Raw XML or Base64 encoded ISO bytes';
COMMENT ON COLUMN message_audit_log.parsed_message IS 'Parsed fields as text';
