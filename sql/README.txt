IMPS Database Schema

Files:
  imps_full_schema.sql   - IMPS database (transaction, message_audit_log, institution_master, etc.)
  switch_full_schema.sql - Switch database (account_master, etc.)

Usage:
  1. Create databases:
     psql -U postgres -c "CREATE DATABASE imps_db;"
     psql -U postgres -c "CREATE DATABASE switch_db;"

  2. Run schema:
     psql -U postgres -d imps_db -f imps_full_schema.sql
     psql -U postgres -d switch_db -f switch_full_schema.sql

Connection: Configure SPRING_DATASOURCE_URL, USERNAME, PASSWORD in each project .env.
Imps-backend: imps_db
mock_switch: switch_db
