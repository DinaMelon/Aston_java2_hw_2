package db.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Общее описание API для Swagger UI (http://localhost:8081/swagger-ui.html).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .version("1.0")
                        .description("""
                                REST-вариант отправки писем о создании/удалении аккаунта. \
                                То же самое сервис делает автоматически по сообщениям из Kafka (топик `user-events`).""")
                        .contact(new Contact().name("Aston Java 2")));
    }
}
