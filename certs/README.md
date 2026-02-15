# SSL/TLS Keystores for IMPS

**Run `generate-certs.bat` from project root before first run** – keystores are gitignored and must be generated locally.

```
D:/IMPS-BACKEND/          (or E:/Hitachi_Project/)
├── certs/                ← run certs\generate-certs.bat here
├── Imps-backend/
├── mock_npci/
└── mock_switch/
```

**Output (PKCS12):** `imps-keystore.jks`, `switch-keystore.jks`, `npci-keystore.jks`, truststores.  
**Config:** Uses `${user.dir}/../certs/` – run each app from its module dir (e.g. `cd mock_switch && mvn spring-boot:run`).
