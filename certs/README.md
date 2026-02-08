# SSL/TLS Keystores for IMPS

Run `generate-certs.bat` (Windows) or `generate-certs.sh` (Unix) to create self-signed keystores for local dev.

**Output:** `imps-keystore.jks`, `imps-truststore.jks`, `switch-keystore.jks`, `npci-keystore.jks`, `client-truststore.jks`, `switch-truststore.jks`, `npci-truststore.jks`

**Config paths:** Use `file:certs/...` when running from project root, or `file:../certs/...` when running from a module (Imps-backend, mock_switch, mock_npci).
