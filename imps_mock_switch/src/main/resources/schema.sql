-- ============================================
-- SWITCH_DB Schema for Mock Switch Audit
-- ============================================
-- This script creates the audit_log table for tracking
-- all incoming and outgoing ISO 8583 messages.
--
-- Database: switch_db (PostgreSQL)
-- ============================================

-- Create database if not exists (run separately in psql)
-- CREATE DATABASE switch_db;

-- Drop table if exists (for fresh start)
-- DROP TABLE IF EXISTS audit_log;

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
-- INDEXES for better query performance
-- ============================================
CREATE INDEX IF NOT EXISTS idx_audit_txn_id ON audit_log(transaction_id);
CREATE INDEX IF NOT EXISTS idx_audit_rrn ON audit_log(rrn);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_api_type ON audit_log(api_type);
CREATE INDEX IF NOT EXISTS idx_audit_direction ON audit_log(direction);
CREATE INDEX IF NOT EXISTS idx_audit_status ON audit_log(status);
CREATE INDEX IF NOT EXISTS idx_audit_response_code ON audit_log(response_code);

-- Composite index for common queries
CREATE INDEX IF NOT EXISTS idx_audit_api_direction ON audit_log(api_type, direction);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp_status ON audit_log(timestamp, status);

-- ============================================
-- COMMENTS
-- ============================================
COMMENT ON TABLE audit_log IS 'Stores all incoming and outgoing ISO 8583 messages for audit trail';
COMMENT ON COLUMN audit_log.direction IS 'INBOUND = request received, OUTBOUND = response/request sent';
COMMENT ON COLUMN audit_log.api_type IS 'Type of API: REQPAY, RESPPAY, REQCHKTXN, RESPCHKTXN, etc.';
COMMENT ON COLUMN audit_log.mti IS 'ISO 8583 Message Type Indicator';
COMMENT ON COLUMN audit_log.rrn IS 'Retrieval Reference Number for transaction tracking';
COMMENT ON COLUMN audit_log.stan IS 'System Trace Audit Number';
COMMENT ON COLUMN audit_log.status IS 'RECEIVED, PROCESSING, SUCCESS, FAILED, SENT, SEND_FAILED';
COMMENT ON COLUMN audit_log.raw_iso_hex IS 'Raw ISO message bytes in hexadecimal format';
COMMENT ON COLUMN audit_log.parsed_iso_json IS 'Parsed ISO fields stored as JSON';

-- ============================================
-- SAMPLE QUERIES
-- ============================================
-- Get all inbound requests:
-- SELECT * FROM audit_log WHERE direction = 'INBOUND' ORDER BY timestamp DESC;

-- Get all outbound responses:
-- SELECT * FROM audit_log WHERE direction = 'OUTBOUND' ORDER BY timestamp DESC;

-- Get all REQPAY transactions:
-- SELECT * FROM audit_log WHERE api_type = 'REQPAY' ORDER BY timestamp DESC;

-- Get failed transactions:
-- SELECT * FROM audit_log WHERE status IN ('FAILED', 'SEND_FAILED') ORDER BY timestamp DESC;

-- Get transactions by RRN:
-- SELECT * FROM audit_log WHERE rrn = 'YOUR_RRN' ORDER BY timestamp DESC;

-- Get statistics by API type:
-- SELECT api_type, COUNT(*) as count FROM audit_log GROUP BY api_type ORDER BY count DESC;

-- Get statistics by response code:
-- SELECT response_code, COUNT(*) as count FROM audit_log WHERE response_code IS NOT NULL GROUP BY response_code ORDER BY count DESC;

-- Clean up old logs (older than 30 days):
-- DELETE FROM audit_log WHERE timestamp < NOW() - INTERVAL '30 days';

-- ============================================
-- GRANT PERMISSIONS (if needed)
-- ============================================
-- GRANT ALL PRIVILEGES ON TABLE audit_log TO your_user;
-- GRANT USAGE, SELECT ON SEQUENCE audit_log_id_seq TO your_user;
