package db.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ответы, которые gateway отдаёт, когда circuit breaker разомкнут или сервис не ответил вовремя.
 * @RequestMapping без метода — fallback вызывается с тем же HTTP-методом, что и исходный запрос.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        return fallback("user-service");
    }

    @RequestMapping("/notification-service")
    public ResponseEntity<Map<String, Object>> notificationServiceFallback() {
        return fallback("notification-service");
    }

    private ResponseEntity<Map<String, Object>> fallback(String service) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Service Unavailable");
        body.put("message", "Сервис " + service + " временно недоступен, попробуйте позже");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
