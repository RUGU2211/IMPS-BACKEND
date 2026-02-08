#!/bin/bash
# IMPS SSL/TLS - Keystore generation for local dev
# Run: ./certs/generate-certs.sh
# Creates: imps-keystore.jks, imps-truststore.jks, switch-keystore.jks, npci-keystore.jks,
#          client-truststore.jks, switch-truststore.jks, npci-truststore.jks

set -e
cd "$(dirname "$0")"
mkdir -p .

echo "Creating IMPS keystore..."
keytool -genkeypair -alias imps -keyalg RSA -keysize 2048 -validity 365 \
  -keystore imps-keystore.jks -storepass IMPS-Backend -keypass IMPS-Backend \
  -dname "CN=IMPS, OU=Dev, O=Hitachi, L=Mumbai, ST=MH, C=IN"

echo "Creating Switch keystore..."
keytool -genkeypair -alias switch -keyalg RSA -keysize 2048 -validity 365 \
  -keystore switch-keystore.jks -storepass IMPS-Backend -keypass IMPS-Backend \
  -dname "CN=Switch, OU=Dev, O=Hitachi, L=Mumbai, ST=MH, C=IN"

echo "Creating NPCI keystore..."
keytool -genkeypair -alias npci -keyalg RSA -keysize 2048 -validity 365 \
  -keystore npci-keystore.jks -storepass IMPS-Backend -keypass IMPS-Backend \
  -dname "CN=NPCI, OU=Dev, O=Hitachi, L=Mumbai, ST=MH, C=IN"

echo "Exporting certs..."
keytool -exportcert -alias imps -keystore imps-keystore.jks -file imps-public.cer -storepass IMPS-Backend
keytool -exportcert -alias switch -keystore switch-keystore.jks -file switch-public.cer -storepass IMPS-Backend
keytool -exportcert -alias npci -keystore npci-keystore.jks -file npci-public.cer -storepass IMPS-Backend

echo "Creating truststores..."
keytool -importcert -alias imps -file imps-public.cer -keystore client-truststore.jks -storepass IMPS-Backend -noprompt
keytool -importcert -alias switch -file switch-public.cer -keystore switch-truststore.jks -storepass IMPS-Backend -noprompt
keytool -importcert -alias npci -file npci-public.cer -keystore npci-truststore.jks -storepass IMPS-Backend -noprompt
keytool -importcert -alias npci -file npci-public.cer -keystore imps-truststore.jks -storepass IMPS-Backend -noprompt

echo "Done. Keystores created in $(pwd)"
ls -la *.jks
