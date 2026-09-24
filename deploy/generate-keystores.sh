#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/keystores"
PASSWORD="changeit"
DAYS=3650
VEHICLE_SAN="${VEHICLE_SERVICE_SAN:-dns:localhost,ip:127.0.0.1}"
SHOP_SAN="${SHOP_SERVICE_SAN:-dns:localhost,ip:127.0.0.1}"

mkdir -p "$OUT_DIR"
cd "$OUT_DIR"

rm -f vehicle-keystore.jks shop-keystore.jks vehicle-service.cer shop-service.cer vehicle-truststore.jks vehicle-service.pem shop-service.pem

keytool -genkeypair \
  -alias vehicle-service \
  -keyalg RSA -keysize 2048 \
  -validity "$DAYS" \
  -keystore vehicle-keystore.jks \
  -storepass "$PASSWORD" -keypass "$PASSWORD" \
  -dname "CN=vehicle-service, OU=SOA, O=ITMO, L=Saint-Petersburg, C=RU" \
  -ext "SAN=$VEHICLE_SAN"

keytool -genkeypair \
  -alias shop-service \
  -keyalg RSA -keysize 2048 \
  -validity "$DAYS" \
  -keystore shop-keystore.jks \
  -storepass "$PASSWORD" -keypass "$PASSWORD" \
  -dname "CN=shop-service, OU=SOA, O=ITMO, L=Saint-Petersburg, C=RU" \
  -ext "SAN=$SHOP_SAN"

keytool -exportcert \
  -alias vehicle-service \
  -keystore vehicle-keystore.jks \
  -storepass "$PASSWORD" \
  -file vehicle-service.cer

keytool -importcert \
  -alias vehicle-service \
  -keystore vehicle-truststore.jks \
  -storepass "$PASSWORD" \
  -file vehicle-service.cer \
  -noprompt

keytool -exportcert -rfc \
  -alias vehicle-service \
  -keystore vehicle-keystore.jks \
  -storepass "$PASSWORD" \
  -file vehicle-service.pem

keytool -exportcert -rfc \
  -alias shop-service \
  -keystore shop-keystore.jks \
  -storepass "$PASSWORD" \
  -file shop-service.pem

echo "Готово: $OUT_DIR"
echo "  vehicle-keystore.jks   — устанавливается на инстанс vehicle-service"
echo "  shop-keystore.jks      — устанавливается на инстанс shop-service"
echo "  vehicle-truststore.jks — устанавливается на инстанс shop-service (доверие к сертификату vehicle-service)"
echo "  vehicle-service.pem / shop-service.pem — PEM-сертификаты для клиентского приложения"
echo "  пароль хранилищ: $PASSWORD"
