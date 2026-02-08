-- ============================================
-- Switch Full Schema - Drop All, Create All, Insert Data
-- ============================================
-- Database: switch_db (PostgreSQL)
--
-- Usage:
--   1. Create DB (if needed): psql -U postgres -c "CREATE DATABASE switch_db;"
--   2. Run this script:       psql -U postgres -d switch_db -f switch_full_schema.sql
--
-- Tables (1 only - required for Switch):
--   - account_master : ValAdd name enquiry, ReqPay debit/credit, balance updates
--
-- Test data matches IMPS_Req_API_Bodies.md: Rugved/Chetan (HDFC), Sajid (ICIC), Madhav (SBIN).
-- IFSC must match institution_master.ifsc_code in imps_db.
-- ============================================

-- ============================================
-- PART 1: DROP ALL TABLES (reverse order of creation)
-- ============================================
DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS iso_field_mapping CASCADE;
DROP TABLE IF EXISTS account_master CASCADE;

-- ============================================
-- PART 2: CREATE TABLES
-- ============================================

-- 1. ACCOUNT_MASTER (Switch: ValAdd name enquiry, ReqPay debit/credit, balance)
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

-- ============================================
-- PART 3: INSERT DATA (required for Switch APIs / testing)
-- ============================================

-- ACCOUNT_MASTER (Switch: ValAdd name enquiry, ReqPay debit/credit)
-- Matches IMPS_Req_API_Bodies.md: payer 1234567890123456/9876543210987654@HDFC0000001, payee 1111222233334444@ICIC0000001, 5555666677778888@SBIN0000001
-- IFSC must match institution_master.ifsc_code in imps_db (HDFC0000001, ICIC0000001, SBIN0000001)
INSERT INTO account_master (account_number, ifsc_code, account_holder_name, account_type, available_balance, account_status, imps_enabled, upi_enabled, daily_txn_limit, last_updated_time)
VALUES
  ('1234567890123456', 'HDFC0000001', 'Rugved Kharde', 'SB', 50000.00, 'ACTIVE', 'Y', 'Y', 100000.00, CURRENT_TIMESTAMP),
  ('9876543210987654', 'HDFC0000001', 'Chetan Mokashi', 'CA', 100000.00, 'ACTIVE', 'Y', 'Y', 500000.00, CURRENT_TIMESTAMP),
  ('1111222233334444', 'ICIC0000001', 'Sajid Mulla', 'SB', 25000.00, 'ACTIVE', 'Y', 'Y', 50000.00, CURRENT_TIMESTAMP),
  ('5555666677778888', 'SBIN0000001', 'Madhav Shipure', 'SB', 75000.00, 'ACTIVE', 'Y', 'Y', 200000.00, CURRENT_TIMESTAMP);

-- ============================================
-- PART 4: COMMENTS
-- ============================================
COMMENT ON TABLE account_master IS 'Switch: ValAdd name enquiry, ReqPay debit/credit, balance updates. Required.';

-- ============================================
-- VERIFICATION
-- ============================================
SELECT 'switch_full_schema completed successfully (1 table)' AS status;
SELECT relname AS table_name FROM pg_class WHERE relkind = 'r' AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public') AND relname NOT LIKE 'pg_%' ORDER BY relname;
