package kz.powersports.commerce.config;

import kz.powersports.commerce.category.dto.CategoryResponse;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
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
                defaultJsonSerializer =
                RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(
                                RedisSerializer.json()
                        );

        RedisCacheConfiguration defaultConfiguration =
                RedisCacheConfiguration
                        .defaultCacheConfig()
                        .disableCachingNullValues()
                        .serializeValuesWith(
                                defaultJsonSerializer
                        )
                        .entryTtl(
                                Duration.ofMinutes(5)
                        );

        JsonMapper jsonMapper =
                JsonMapper.builder()
                        .build();

        JavaType categoriesType =
                jsonMapper
                        .getTypeFactory()
                        .constructCollectionType(
                                List.class,
                                CategoryResponse.class
                        );

        JacksonJsonRedisSerializer<List<CategoryResponse>>
                categoriesSerializer =
                new JacksonJsonRedisSerializer<>(
                        jsonMapper,
                        categoriesType
                );

        RedisSerializationContext.SerializationPair<
                List<CategoryResponse>>
                categoriesSerializationPair =
                RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(
                                categoriesSerializer
                        );

        RedisCacheConfiguration
                categoriesConfiguration =
                defaultConfiguration
                        .serializeValuesWith(
                                categoriesSerializationPair
                        )
                        .entryTtl(
                                Duration.ofMinutes(30)
                        );

        Map<String, RedisCacheConfiguration>
                cacheConfigurations =
                Map.of(
                        PRODUCTS_CACHE,
                        defaultConfiguration,

                        PRODUCT_BY_SLUG_CACHE,
                        defaultConfiguration,

                        CATEGORIES_CACHE,
                        categoriesConfiguration
                );

        return RedisCacheManager
                .builder(connectionFactory)
                .cacheDefaults(
                        defaultConfiguration
                )
                .withInitialCacheConfigurations(
                        cacheConfigurations
                )
                .build();
    }
}