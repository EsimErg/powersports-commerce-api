package kz.powersports.commerce.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCTS_CACHE =
            "products";

    public static final String PRODUCT_BY_SLUG_CACHE =
            "productBySlug";

    public static final String CATEGORIES_CACHE =
            "categories";

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory
    ) {
        RedisSerializationContext.SerializationPair<Object>
                jsonSerializer =
                RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(
                                RedisSerializer.json()
                        );

        RedisCacheConfiguration defaultConfiguration =
                RedisCacheConfiguration
                        .defaultCacheConfig()
                        .disableCachingNullValues()
                        .serializeValuesWith(jsonSerializer)
                        .entryTtl(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration>
                cacheConfigurations =
                Map.of(
                        PRODUCTS_CACHE,
                        defaultConfiguration.entryTtl(
                                Duration.ofMinutes(5)
                        ),

                        PRODUCT_BY_SLUG_CACHE,
                        defaultConfiguration.entryTtl(
                                Duration.ofMinutes(5)
                        ),

                        CATEGORIES_CACHE,
                        defaultConfiguration.entryTtl(
                                Duration.ofMinutes(30)
                        )
                );

        return RedisCacheManager
                .builder(connectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(
                        cacheConfigurations
                )
                .build();
    }
}