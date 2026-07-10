FROM nginx:alpine
RUN apk add --no-cache openssl
COPY generate-cert.sh /docker-entrypoint.d/40-generate-cert.sh
RUN chmod +x /docker-entrypoint.d/40-generate-cert.sh

