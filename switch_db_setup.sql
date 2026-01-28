-- ============================================
-- SWITCH_DB Complete Database Setup
-- ============================================
-- Run this script in PostgreSQL to set up the
-- switch_db database for Mock Switch audit logging.
--
-- Usage: psql -U postgres -f switch_db_setup.sql
-- ============================================

-- Create database
CREATE DATABASE switch_db;

-- Connect to the database
\c switch_db

-- ============================================
-- AUDIT_LOG TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    
    -- Direction: INBOUND (request received) or OUTBOUND (response sent)
    direction VARCHAR(20) NOT NULL,
    
    -- API Type: REQPAY, RESPPAY, REQCHKTXN, etc.
    api_type VARCHAR(50) NOT NULL,
    
    -- Message Type Indicator (MTI) from ISO message
    mti VARCHAR(10),
    
    -- Transaction ID
    transaction_id VARCHAR(100),
    
    -- Retrieval Reference Number (RRN)
    rrn VARCHAR(50),
    
    -- STAN (System Trace Audit Number)
    stan VARCHAR(20),
    
    -- Processing Code (DE3)
    processing_code VARCHAR(10),
    
    -- Amount
    amount VARCHAR(20),
    
    -- Currency Code
    currency VARCHAR(5),
    
    -- Response Code (DE39)
    response_code VARCHAR(10),
    
    -- Approval Number (DE38)
    approval_number VARCHAR(20),
    
    -- Payer Account / Source Account
    payer_account VARCHAR(50),
    
    -- Payee Account / Destination Account
    payee_account VARCHAR(50),
    
    -- Payer IFSC
    payer_ifsc VARCHAR(20),
    
    -- Payee IFSC
    payee_ifsc VARCHAR(20),
    
    -- Source endpoint (where request came from)
    source_endpoint VARCHAR(255),
    
    -- Destination endpoint (where response was sent)
    destination_endpoint VARCHAR(255),
    
    -- Raw ISO message (hex encoded)
    raw_iso_hex TEXT,
    
    -- Parsed ISO fields as JSON
    parsed_iso_json TEXT,
    
    -- HTTP Status code
    http_status INTEGER,
    
    -- Processing status: RECEIVED, PROCESSING, SUCCESS, FAILED, SENT, SEND_FAILED
    status VARCHAR(20) NOT NULL,
    
    -- Error message if any
    error_message TEXT,
    
    -- Processing time in milliseconds
    processing_time_ms BIGINT,
    
    -- Timestamp when the record was created
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Additional notes or metadata
    notes TEXT
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX idx_audit_txn_id ON audit_log(transaction_id);
CREATE INDEX idx_audit_rrn ON audit_log(rrn);
CREATE INDEX idx_audit_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_api_type ON audit_log(api_type);
CREATE INDEX idx_audit_direction ON audit_log(direction);
CREATE INDEX idx_audit_status ON audit_log(status);
CREATE INDEX idx_audit_response_code ON audit_log(response_code);
CREATE INDEX idx_audit_api_direction ON audit_log(api_type, direction);
CREATE INDEX idx_audit_timestamp_status ON audit_log(timestamp, status);

-- ============================================
-- TRANSACTION_SUMMARY VIEW
-- ============================================
CREATE OR REPLACE VIEW transaction_summary AS
SELECT 
    transaction_id,
    rrn,
    MAX(CASE WHEN direction = 'INBOUND' THEN api_type END) as request_type,
    MAX(CASE WHEN direction = 'OUTBOUND' THEN api_type END) as response_type,
    MAX(CASE WHEN direction = 'INBOUND' THEN timestamp END) as request_time,
    MAX(CASE WHEN direction = 'OUTBOUND' THEN timestamp END) as response_time,
    MAX(amount) as amount,
    MAX(payer_account) as payer_account,
    MAX(payee_account) as payee_account,
    MAX(response_code) as response_code,
    MAX(CASE WHEN direction = 'OUTBOUND' THEN status END) as final_status
FROM audit_log
WHERE transaction_id IS NOT NULL
GROUP BY transaction_id, rrn
ORDER BY request_time DESC;

-- ============================================
-- DAILY_STATS VIEW
-- ============================================
CREATE OR REPLACE VIEW daily_stats AS
SELECT 
    DATE(timestamp) as date,
    api_type,
    direction,
    COUNT(*) as total_count,
    COUNT(CASE WHEN status = 'SUCCESS' OR status = 'SENT' THEN 1 END) as success_count,
    COUNT(CASE WHEN status = 'FAILED' OR status = 'SEND_FAILED' THEN 1 END) as failed_count,
    AVG(processing_time_ms) as avg_processing_time_ms
FROM audit_log
GROUP BY DATE(timestamp), api_type, direction
ORDER BY date DESC, api_type;

-- ============================================
-- RESPONSE_CODE_STATS VIEW
-- ============================================
CREATE OR REPLACE VIEW response_code_stats AS
SELECT 
    response_code,
    CASE response_code
        WHEN '00' THEN 'SUCCESS'
        WHEN '01' THEN 'REFER TO CARD ISSUER'
        WHEN '05' THEN 'DO NOT HONOR'
        WHEN '12' THEN 'INVALID TRANSACTION'
        WHEN '13' THEN 'INVALID AMOUNT'
        WHEN '14' THEN 'INVALID CARD NUMBER'
        WHEN '30' THEN 'FORMAT ERROR'
        WHEN '51' THEN 'INSUFFICIENT FUNDS'
        WHEN '54' THEN 'EXPIRED CARD'
        WHEN '91' THEN 'ISSUER/SWITCH INOPERATIVE'
        WHEN '96' THEN 'SYSTEM MALFUNCTION'
        ELSE 'OTHER'
    END as description,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM audit_log
WHERE response_code IS NOT NULL
GROUP BY response_code
ORDER BY count DESC;

-- ============================================
-- COMMENTS
-- ============================================
COMMENT ON TABLE audit_log IS 'Stores all incoming and outgoing ISO 8583 messages for audit trail';
COMMENT ON VIEW transaction_summary IS 'Summary view of transactions with request and response details';
COMMENT ON VIEW daily_stats IS 'Daily statistics by API type and direction';
COMMENT ON VIEW response_code_stats IS 'Statistics by response code with descriptions';

-- ============================================
-- VERIFICATION
-- ============================================
SELECT 'switch_db setup complete!' as status;
SELECT 'Tables created:' as info;
SELECT tablename FROM pg_tables WHERE schemaname = 'public';
SELECT 'Views created:' as info;
SELECT viewname FROM pg_views WHERE schemaname = 'public';
