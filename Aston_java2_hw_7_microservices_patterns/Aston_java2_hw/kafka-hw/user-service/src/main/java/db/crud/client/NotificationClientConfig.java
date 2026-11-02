package db.crud.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class NotificationClientConfig {

    /**
     * @LoadBalanced: имя хоста в URL (http://notification-service/...) заменяется адресом из Eureka.
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2_000);
        factory.setReadTimeout(3_000);
        return RestClient.builder().requestFactory(factory);
    }

    @Bean
    public RestClient notificationRestClient(RestClient.Builder builder) {
        return builder.build();
    }

    /**
     * Параметры circuit breaker «notification-service»:
     * окно из 10 вызовов, при ≥50% ошибок цепь размыкается на 10 секунд.
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> notificationCircuitBreakerCustomizer() {
        return factory -> factory.configure(builder -> builder
                        .circuitBreakerConfig(CircuitBreakerConfig.custom()
                                .slidingWindowSize(10)
                                .minimumNumberOfCalls(5)
                                .failureRateThreshold(50)
                                .waitDurationInOpenState(Duration.ofSeconds(10))
                                .build())
                        .timeLimiterConfig(TimeLimiterConfig.custom()
                                .timeoutDuration(Duration.ofSeconds(4))
                                .build()),
                NotificationClient.CIRCUIT_BREAKER_NAME);
    }
}
