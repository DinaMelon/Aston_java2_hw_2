package db.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Паттерн Service Discovery: реестр, в котором сервисы регистрируются и ищут друг друга по имени.
 * Дашборд: http://localhost:8761
 */
@EnableEurekaServer
@SpringBootApplication
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
