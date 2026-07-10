#!/bin/sh
CERT_DIR="/etc/nginx/certs"
CERT_FILE="$CERT_DIR/nginx-selfsigned.crt"
KEY_FILE="$CERT_DIR/nginx-selfsigned.key"

# Если переменная SSL_CN не задана, используем значение по умолчанию
CN="${SSL_CN:-localhost}"

mkdir -p "$CERT_DIR"

if [ ! -f "$CERT_FILE" ] || [ ! -f "$KEY_FILE" ]; then
    echo "Генерирую сертификат для CN=$CN ..."
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout "$KEY_FILE" \
        -out "$CERT_FILE" \
        -subj "/CN=$CN"
    echo "Сертификат создан для досена $SSL_CN."
else
    echo "Сертификат уже существует, использую имеющийся. Вы можете удалить его вручную для генерации нового при следующем запуске."
fi
