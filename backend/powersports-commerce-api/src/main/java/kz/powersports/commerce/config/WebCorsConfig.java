package kz.powersports.commerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebCorsConfig
        implements WebMvcConfigurer {

    private final String[] allowedOriginPatterns;

    public WebCorsConfig(
            @Value("${app.cors.allowed-origin-patterns}")
            String allowedOriginPatterns
    ) {
        this.allowedOriginPatterns =
                Arrays.stream(
                                allowedOriginPatterns.split(",")
                        )
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toArray(String[]::new);

        if (this.allowedOriginPatterns.length == 0) {
            throw new IllegalStateException(
                    "Не настроены разрешённые frontend origins"
            );
        }
    }

    @Override
    public void addCorsMappings(
            CorsRegistry registry
    ) {
        registry.addMapping("/api/**")
                /*
                 * Разрешаем только настроенные
                 * frontend-домены.
                 */
                .allowedOriginPatterns(
                        allowedOriginPatterns
                )

                /*
                 * Методы, которые использует
                 * каталог, корзина и заказы.
                 */
                .allowedMethods(
                        "GET",
                        "POST",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )

                /*
                 * Заголовки, которые frontend
                 * может отправлять в Spring Boot.
                 */
                .allowedHeaders(
                        "Accept",
                        "Content-Type",
                        "Idempotency-Key",
                        "X-Requested-With"
                )

                /*
                 * Обязательно для передачи
                 * POWERSPORTS_SESSION cookie.
                 */
                .allowCredentials(true)

                /*
                 * Браузер может кешировать
                 * preflight-ответ один час.
                 */
                .maxAge(3600);
    }
}
