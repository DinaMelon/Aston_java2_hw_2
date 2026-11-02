package db.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Паттерн API Gateway: единая точка входа http://localhost:8080.
 * Маршруты описаны во внешней конфигурации (config-repo/api-gateway.yml).
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
