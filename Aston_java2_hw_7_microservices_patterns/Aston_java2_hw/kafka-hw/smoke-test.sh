#!/usr/bin/env bash
# Проверка, что сервисы в docker compose работают вместе.
# Запуск (после `docker compose up -d --build --wait`):  ./scripts/smoke-test.sh
set -u

GATEWAY=${GATEWAY:-http://localhost:8080}
EUREKA=${EUREKA:-http://localhost:8761}
CONFIG=${CONFIG:-http://localhost:8888}
MAILHOG=${MAILHOG:-http://localhost:8025}
EMAIL="smoke-$(date +%s)@test.ru"
FAILED=0

ok()   { echo "  [OK]   $*"; }
fail() { echo "  [FAIL] $*"; FAILED=1; }

# retry <секунд> <команда...> — повторяет команду раз в 3 с, пока она не вернёт 0
retry() {
  local deadline=$((SECONDS + $1)); shift
  until "$@" >/dev/null 2>&1; do
    (( SECONDS >= deadline )) && return 1
    sleep 3
  done
}

eureka_has_all() {
  local apps; apps=$(curl -sf -H 'Accept: application/json' "$EUREKA/eureka/apps") || return 1
  for a in USER-SERVICE NOTIFICATION-SERVICE API-GATEWAY; do
    grep -q "\"name\" *: *\"$a\"" <<<"$apps" || return 1
  done
}
gateway_api()  { [[ $(curl -s -o /dev/null -w '%{http_code}' "$GATEWAY/api") == 200 ]]; }
mail_arrived() { curl -sf "$MAILHOG/api/v2/search?kind=to&query=$EMAIL" | grep -q '"total":[1-9]'; }

echo "1. External configuration (config-server)"
curl -sf "$CONFIG/user-service/default" | grep -q 'spring.datasource.url' \
  && ok "config-server отдаёт конфигурацию user-service (spring.datasource.url)" \
  || fail "config-server не отдаёт ожидаемую конфигурацию user-service"

echo "2. Service discovery (Eureka)"
retry 90 eureka_has_all \
  && ok "в Eureka зарегистрированы user-service, notification-service, api-gateway" \
  || fail "не все сервисы зарегистрированы в Eureka"

echo "3. API Gateway -> user-service (маршрут lb://user-service)"
retry 90 gateway_api \
  && ok "GET $GATEWAY/api -> 200" \
  || fail "gateway не смог обратиться к user-service"

echo "4. user-service -> PostgreSQL (через gateway)"
code=$(curl -s -o /tmp/smoke-create.json -w '%{http_code}' -X POST "$GATEWAY/api/users" \
  -H 'Content-Type: application/json' -d "{\"name\":\"Smoke\",\"email\":\"$EMAIL\",\"age\":30}")
[[ $code == 201 ]] && ok "POST /api/users -> 201 ($EMAIL)" || fail "POST /api/users -> $code"
curl -sf "$GATEWAY/api/users" | grep -q "$EMAIL" \
  && ok "пользователь виден в GET /api/users (данные лежат в PostgreSQL)" \
  || fail "созданного пользователя нет в списке"

echo "5. user-service -> Kafka -> notification-service -> SMTP (MailHog)"
retry 60 mail_arrived \
  && ok "письмо для $EMAIL получено в MailHog ($MAILHOG)" \
  || fail "письмо не пришло: проверьте docker compose logs user-service notification-service kafka"

echo "6. Circuit breaker на gateway (необязательно, вручную):"
echo "     docker compose stop user-service && curl -i $GATEWAY/api/users   # 503 из fallback"

echo
(( FAILED )) && { echo "ИТОГ: есть ошибки"; exit 1; } || echo "ИТОГ: всё работает"
