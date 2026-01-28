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
-- 3. ACCOUNT_MASTER TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS account_master (
    id SERIAL PRIMARY KEY,
    account_number VARCHAR(255),
    ifsc_code VARCHAR(255),
    account_name VARCHAR(255),
    status VARCHAR(255)
);

-- Indexes for account_master table
CREATE INDEX IF NOT EXISTS idx_account_number ON account_master(account_number);
CREATE INDEX IF NOT EXISTS idx_account_ifsc ON account_master(ifsc_code);

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
COMMENT ON TABLE message_audit_log IS 'Audit trail for raw and parsed messages';
COMMENT ON TABLE account_master IS 'Master table for account details';
COMMENT ON TABLE institution_master IS 'Master table for institution/bank details';
COMMENT ON TABLE account_type_mapping IS 'Maps account types to ISO codes';
COMMENT ON TABLE xml_path_req_pay IS 'XPath configurations for XML parsing';
COMMENT ON TABLE response_xpath IS 'XPath configurations for response parsing';

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

COMMENT ON COLUMN message_audit_log.stage IS 'Stage: NPCI_REQ_IN, ISO_OUT, ISO_IN, NPCI_RESP_OUT';
COMMENT ON COLUMN message_audit_log.raw_message IS 'Raw XML or Base64 encoded ISO bytes';
COMMENT ON COLUMN message_audit_log.parsed_message IS 'Parsed fields as text';
