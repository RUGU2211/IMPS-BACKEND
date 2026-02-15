@echo off
REM IMPS SSL/TLS - Keystore generation for local dev
REM Run from project root: certs\generate-certs.bat
REM Creates: imps-keystore.jks, imps-truststore.jks, switch-keystore.jks, npci-keystore.jks,
REM          client-truststore.jks, switch-truststore.jks, npci-truststore.jks

cd /d "%~dp0"
if not exist . mkdir .

REM Use -storetype PKCS12 so application.yml key-store-type matches (avoids fallback to ~/.keystore)
echo Creating IMPS keystore...
keytool -genkeypair -alias imps -keyalg RSA -keysize 2048 -validity 365 -storetype PKCS12 -keystore imps-keystore.jks -storepass IMPS-Backend -keypass IMPS-Backend -dname "CN=IMPS, OU=Dev, O=Hitachi, L=Mumbai, ST=MH, C=IN"

echo Creating Switch keystore...
keytool -genkeypair -alias switch -keyalg RSA -keysize 2048 -validity 365 -storetype PKCS12 -keystore switch-keystore.jks -storepass IMPS-Backend -keypass IMPS-Backend -dname "CN=Switch, OU=Dev, O=Hitachi, L=Mumbai, ST=MH, C=IN"

echo Creating NPCI keystore...
keytool -genkeypair -alias npci -keyalg RSA -keysize 2048 -validity 365 -storetype PKCS12 -keystore npci-keystore.jks -storepass IMPS-Backend -keypass IMPS-Backend -dname "CN=NPCI, OU=Dev, O=Hitachi, L=Mumbai, ST=MH, C=IN"

echo Exporting certs...
keytool -exportcert -alias imps -keystore imps-keystore.jks -file imps-public.cer -storepass IMPS-Backend
keytool -exportcert -alias switch -keystore switch-keystore.jks -file switch-public.cer -storepass IMPS-Backend
keytool -exportcert -alias npci -keystore npci-keystore.jks -file npci-public.cer -storepass IMPS-Backend

echo Creating truststores...
keytool -importcert -alias imps -file imps-public.cer -keystore client-truststore.jks -storetype PKCS12 -storepass IMPS-Backend -noprompt
keytool -importcert -alias switch -file switch-public.cer -keystore switch-truststore.jks -storetype PKCS12 -storepass IMPS-Backend -noprompt
keytool -importcert -alias npci -file npci-public.cer -keystore npci-truststore.jks -storetype PKCS12 -storepass IMPS-Backend -noprompt
keytool -importcert -alias npci -file npci-public.cer -keystore imps-truststore.jks -storetype PKCS12 -storepass IMPS-Backend -noprompt

echo Done. Keystores created in %CD%
dir *.jks
