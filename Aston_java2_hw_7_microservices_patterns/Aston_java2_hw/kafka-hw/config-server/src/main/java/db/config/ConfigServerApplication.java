package db.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Паттерн External Configuration: все сервисы забирают настройки отсюда.
 * Файлы лежат в resources/config-repo (native-профиль); в продакшене обычно подключают git-репозиторий.
 */
@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
