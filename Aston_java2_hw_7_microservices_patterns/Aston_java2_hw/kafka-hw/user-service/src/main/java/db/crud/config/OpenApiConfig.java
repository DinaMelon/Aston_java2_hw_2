package db.crud.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Общее описание API для Swagger UI (http://localhost:8080/swagger-ui.html).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .version("1.0")
                        .description("""
                                CRUD-API пользователей (PostgreSQL). Ответы оформлены по принципам HATEOAS \
                                в формате HAL: каждый ресурс содержит блок `_links` со ссылками для навигации \
                                (`self`, `users`, `update`, `delete`).
                                Точка входа в API: `GET /api`.
                                При создании и удалении пользователя сервис отправляет событие в Kafka.""")
                        .contact(new Contact().name("Aston Java 2")))
                .servers(List.of(new Server().url("/").description("Текущий сервер")));
    }
}
