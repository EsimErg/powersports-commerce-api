package kz.powersports.commerce.torgsoft.mapping;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = "enabled",
        havingValue = "true"
)
public class RedisTorgsoftProductMappingRepository
        implements TorgsoftProductMappingRepository {

    private static final String KEY_PREFIX =
            "powersports:torgsoft:product-mapping:";

    private final StringRedisTemplate redisTemplate;

    public RedisTorgsoftProductMappingRepository(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = Objects.requireNonNull(
                redisTemplate,
                "redisTemplate не должен быть null"
        );
    }

    @Override
    public Optional<Long> findWooCommerceProductId(String goodId) {
        String normalizedGoodId = normalizeGoodId(goodId);

        String value = redisTemplate
                .opsForValue()
                .get(buildKey(normalizedGoodId));

        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Повреждено сопоставление товара Torgsoft. GoodID: "
                            + normalizedGoodId,
                    exception
            );
        }
    }

    @Override
    public void save(
            String goodId,
            Long wooCommerceProductId
    ) {
        String normalizedGoodId = normalizeGoodId(goodId);

        if (wooCommerceProductId == null
                || wooCommerceProductId <= 0) {
            throw new IllegalArgumentException(
                    "WooCommerce product ID должен быть положительным"
            );
        }

        redisTemplate
                .opsForValue()
                .set(
                        buildKey(normalizedGoodId),
                        wooCommerceProductId.toString()
                );
    }

    @Override
    public void delete(String goodId) {
        redisTemplate.delete(
                buildKey(normalizeGoodId(goodId))
        );
    }

    private String buildKey(String goodId) {
        return KEY_PREFIX + goodId;
    }

    private String normalizeGoodId(String goodId) {
        if (goodId == null || goodId.isBlank()) {
            throw new IllegalArgumentException(
                    "Torgsoft GoodID не должен быть пустым"
            );
        }

        return goodId.trim();
    }
}