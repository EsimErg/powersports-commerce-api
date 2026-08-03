package kz.powersports.commerce.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Configuration
public class WooCommerceClientConfig {

    /**
     * Публичный Store API:
     * каталог, категории и корзина.
     */
    @Bean
    @Primary
    public RestClient wooCommerceRestClient(
            @Value("${woocommerce.base-url}")
            String baseUrl
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Административный REST API:
     *
     * HTTP  -> OAuth 1.0a
     * HTTPS -> Basic Auth
     */
    @Bean
    @Qualifier("wooCommerceAdminRestClient")
    public RestClient wooCommerceAdminRestClient(
            @Value("${woocommerce.base-url}")
            String baseUrl,

            @Value("${woocommerce.consumer-key}")
            String consumerKey,

            @Value("${woocommerce.consumer-secret}")
            String consumerSecret
    ) {
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .defaultHeaders(headers -> {
                            headers.setContentType(
                                    MediaType.APPLICATION_JSON
                            );

                            headers.setAccept(
                                    java.util.List.of(
                                            MediaType.APPLICATION_JSON
                                    )
                            );
                        });

        if (baseUrl.startsWith("https://")) {
            /*
             * Production:
             * защищённое HTTPS-соединение.
             */
            builder.defaultHeaders(headers ->
                    headers.setBasicAuth(
                            consumerKey,
                            consumerSecret,
                            StandardCharsets.UTF_8
                    )
            );

        } else {
            /*
             * Локальная разработка:
             * WooCommerce запущен по HTTP.
             */
            builder.requestInterceptor(
                    new WooCommerceOAuth1Interceptor(
                            consumerKey,
                            consumerSecret
                    )
            );
        }

        return builder.build();
    }
}