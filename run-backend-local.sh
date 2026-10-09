#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-/tmp/gardle-jdk11}"
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "Defina JAVA_HOME para um JDK 8 a 13 (recomendado: JDK 11)." >&2
  exit 1
fi

if ! docker inspect gardle-refactor-db >/dev/null 2>&1; then
  docker run -d --rm --name gardle-refactor-db \
    -e POSTGRES_DB=gardle -e POSTGRES_USER=gardle \
    -e POSTGRES_PASSWORD=gardle_local -p 15433:5432 postgres:17-alpine >/dev/null
elif [[ "$(docker inspect -f '{{.State.Running}}' gardle-refactor-db)" != true ]]; then
  docker start gardle-refactor-db >/dev/null
fi

until docker exec gardle-refactor-db pg_isready -U gardle -d gardle >/dev/null 2>&1; do
  sleep 1
done

export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:15433/gardle
export SPRING_DATASOURCE_USERNAME=gardle
export SPRING_DATASOURCE_PASSWORD=gardle_local
export SPRING_MAIL_HOST=localhost
export SPRING_MAIL_PORT=1025
export MANAGEMENT_HEALTH_MAIL_ENABLED=false
export JASYPT_ENCRYPTOR_PASSWORD=local-dev
export JHIPSTER_SECURITY_AUTHENTICATION_JWT_BASE64_SECRET=MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=
export STRIPE_PUBLIC_KEY=local
export STRIPE_PRIVATE_KEY=local
export STRIPE_LEASING_WEBHOOK_SECRET=local
export STRIPE_ACCOUNT_WEBHOOK_SECRET=local
export STRIPE_PAYMENT_SUCCESS_URL=http://localhost:8080/payment/success
export STRIPE_PAYMENT_CANCEL_URL=http://localhost:8080/payment/error
export STRIPE_ACCOUNT_SUCCESS_URL=http://localhost:8080/account/verification/success
export STRIPE_ACCOUNT_FAILURE_URL=http://localhost:8080/account/verification/error

exec ./mvnw spring-boot:run
