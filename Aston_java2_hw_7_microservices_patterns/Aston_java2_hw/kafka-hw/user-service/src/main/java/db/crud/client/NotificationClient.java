package db.crud.client;

import db.crud.event.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Резервный канал доставки: прямой REST-вызов notification-service (POST /api/notifications).
 * Адрес сервиса определяется через Service Discovery (Eureka), вызов защищён Circuit Breaker'ом:
 * если notification-service недоступен, после серии ошибок цепь размыкается и вызовы
 * сразу завершаются fallback'ом, не создавая лишних таймаутов.
 */
@Component
public class NotificationClient {

    public static final String CIRCUIT_BREAKER_NAME = "notification-service";

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
    private static final String URL = "http://notification-service/api/notifications";

    private final RestClient restClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public NotificationClient(RestClient notificationRestClient,
                              CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.restClient = notificationRestClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    /**
     * @return true, если уведомление принято notification-service; false — сработал fallback
     */
    public boolean send(UserEvent event) {
        return circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME).run(
                () -> {
                    restClient.post()
                            .uri(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("operation", event.operation().name(), "email", event.email()))
                            .retrieve()
                            .toBodilessEntity();
                    return true;
                },
                throwable -> {
                    log.warn("notification-service недоступен, уведомление {} не отправлено: {}",
                            event, throwable.toString());
                    return false;
                });
    }
}
