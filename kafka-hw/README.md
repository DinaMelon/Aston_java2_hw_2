# Aston Java 2 — HW: Kafka (user-service + notification-service)

Два Spring Boot микросервиса:

| Сервис | Порт | Что делает |
|---|---|---|
| `user-service` | 8080 | CRUD пользователей (PostgreSQL). При создании/удалении пользователя отправляет событие в Kafka |
| `notification-service` | 8081 | Читает события из Kafka и отправляет письмо на email пользователя. Также есть REST API для ручной отправки |

```
user-service --(топик user-events, JSON {"operation":"CREATED|DELETED","email":"..."})--> Kafka --> notification-service --SMTP--> почта
```

## Тексты писем

* создание: `Здравствуйте! Ваш аккаунт на сайте ваш сайт был успешно создан.`
  (название сайта задаётся переменной `SITE_NAME`, по умолчанию `ваш сайт`)
* удаление: `Здравствуйте! Ваш аккаунт был удалён.`

## Запуск

```bash
# 1. PostgreSQL + Kafka + MailHog (тестовый SMTP, веб-интерфейс http://localhost:8025)
docker compose up -d

# 2. user-service
cd user-service && mvn spring-boot:run

# 3. notification-service (в другом терминале)
cd notification-service && mvn spring-boot:run
```

Проверка:

```bash
# создать пользователя -> в MailHog придёт письмо о создании
curl -X POST localhost:8080/api/users -H 'Content-Type: application/json' \
     -d '{"name":"Иван","email":"ivan@test.ru","age":20}'

# удалить пользователя (id из ответа) -> письмо об удалении
curl -X DELETE localhost:8080/api/users/1

# отдельный API notification-service (без Kafka)
curl -X POST localhost:8081/api/notifications -H 'Content-Type: application/json' \
     -d '{"email":"ivan@test.ru","operation":"CREATED"}'
```

`operation`: `CREATED` или `DELETED`. Ответ `200` с телом `{email, operation, subject, message}`;
`400` — невалидный запрос; `502` — почтовый сервер не принял письмо.

## Реальная почта (вместо MailHog)

Переменные окружения `notification-service`:

| Переменная | По умолчанию | Пример для Gmail |
|---|---|---|
| `MAIL_HOST` | `localhost` | `smtp.gmail.com` |
| `MAIL_PORT` | `1025` | `587` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | пусто | адрес / пароль приложения |
| `MAIL_SMTP_AUTH` | `false` | `true` |
| `MAIL_SMTP_STARTTLS` | `false` | `true` |
| `MAIL_FROM` | `no-reply@example.com` | тот же адрес, что `MAIL_USERNAME` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | |

## Тесты

```bash
mvn verify          # из корня: оба сервиса
```

* `user-service`: unit-тесты (в т.ч. проверка публикации событий), `@WebMvcTest`, интеграционный тест
  (PostgreSQL в Testcontainers — нужен Docker, + `@EmbeddedKafka`): проверяет, что при создании/удалении
  в топик уходят `CREATED`/`DELETED` с email, а при ошибке (409) события нет.
* `notification-service`: unit-тесты текстов, `@WebMvcTest` для API и **интеграционный тест**
  `NotificationIntegrationTest` — `@EmbeddedKafka` + GreenMail (SMTP-сервер в JVM, Docker не нужен).
  Проверяется, что письма реально приходят адресату с нужной темой и текстом — и после сообщения из Kafka,
  и после вызова REST API; «битое» сообщение в Kafka пропускается.

## Заметки по реализации

* Событие в Kafka отправляется **после коммита** транзакции (`@TransactionalEventListener(AFTER_COMMIT)`),
  поэтому откатённая операция не приведёт к письму. Недоступность Kafka не ломает CRUD API.
* Ключ сообщения — email, значение — JSON. Типы Java-классов в заголовки не пишутся, у каждого сервиса свой DTO.
* Ошибка отправки письма в consumer'е: 3 повтора с паузой 1 с, затем сообщение пропускается (`DefaultErrorHandler`).
