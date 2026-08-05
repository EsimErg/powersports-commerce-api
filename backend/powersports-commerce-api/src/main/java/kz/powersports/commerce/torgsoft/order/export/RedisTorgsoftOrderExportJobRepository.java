package kz.powersports.commerce.torgsoft.order.export;

import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Repository
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = "enabled",
        havingValue = "true"
)
public class RedisTorgsoftOrderExportJobRepository
        implements TorgsoftOrderExportJobRepository {

    private static final String JOB_KEY_PREFIX =
            "powersports:torgsoft:order-export:job:";

    private static final String PENDING_KEY =
            "powersports:torgsoft:order-export:pending";

    /*
     * Атомарно:
     * 1. Проверяет, не поставлен ли заказ в очередь.
     * 2. Сохраняет JSON задания.
     * 3. Добавляет orderId в очередь по времени запуска.
     */
    private static final DefaultRedisScript<Long> ENQUEUE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('EXISTS', KEYS[1]) == 1 then
                        return 0
                    end

                    redis.call('SET', KEYS[1], ARGV[1])
                    redis.call('ZADD', KEYS[2], ARGV[2], ARGV[3])

                    return 1
                    """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    public RedisTorgsoftOrderExportJobRepository(
            StringRedisTemplate redisTemplate,
            JsonMapper jsonMapper
    ) {
        this.redisTemplate = Objects.requireNonNull(
                redisTemplate,
                "redisTemplate не должен быть null"
        );

        this.jsonMapper = Objects.requireNonNull(
                jsonMapper,
                "jsonMapper не должен быть null"
        );
    }
    @Override
    public void update(TorgsoftOrderExportJob job) {
        Objects.requireNonNull(
                job,
                "job не должен быть null"
        );

        try {
            String json =
                    jsonMapper.writeValueAsString(job);

            redisTemplate
                    .opsForValue()
                    .set(
                            buildJobKey(
                                    job.wooCommerceOrderId()
                            ),
                            json
                    );

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Не удалось обновить задание "
                            + "экспорта заказа Torgsoft",
                    exception
            );
        }
    }

    @Override
    public void schedule(
            Long wooCommerceOrderId,
            Instant nextAttemptAt
    ) {
        validateOrderId(wooCommerceOrderId);

        Objects.requireNonNull(
                nextAttemptAt,
                "nextAttemptAt не должен быть null"
        );

        redisTemplate
                .opsForZSet()
                .add(
                        PENDING_KEY,
                        wooCommerceOrderId.toString(),
                        nextAttemptAt.toEpochMilli()
                );
    }

    @Override
    public void removeFromPending(
            Long wooCommerceOrderId
    ) {
        validateOrderId(wooCommerceOrderId);

        redisTemplate
                .opsForZSet()
                .remove(
                        PENDING_KEY,
                        wooCommerceOrderId.toString()
                );
    }

    @Override
    public boolean enqueue(TorgsoftOrderExportJob job) {
        Objects.requireNonNull(
                job,
                "job не должен быть null"
        );

        try {
            String json =
                    jsonMapper.writeValueAsString(job);

            Long result = redisTemplate.execute(
                    ENQUEUE_SCRIPT,
                    List.of(
                            buildJobKey(job.wooCommerceOrderId()),
                            PENDING_KEY
                    ),
                    json,
                    Long.toString(
                            job.nextAttemptAt().toEpochMilli()
                    ),
                    job.wooCommerceOrderId().toString()
            );

            return Long.valueOf(1L).equals(result);

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Не удалось сериализовать задание "
                            + "экспорта заказа Torgsoft",
                    exception
            );
        }
    }

    @Override
    public Optional<TorgsoftOrderExportJob> findByOrderId(
            Long wooCommerceOrderId
    ) {
        validateOrderId(wooCommerceOrderId);

        String json = redisTemplate
                .opsForValue()
                .get(buildJobKey(wooCommerceOrderId));

        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    jsonMapper.readValue(
                            json,
                            TorgsoftOrderExportJob.class
                    )
            );

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Повреждено задание экспорта заказа Torgsoft. "
                            + "Order ID: " + wooCommerceOrderId,
                    exception
            );
        }
    }

    @Override
    public List<Long> findDueOrderIds(
            Instant now,
            int limit
    ) {
        Objects.requireNonNull(
                now,
                "now не должен быть null"
        );

        int safeLimit = Math.max(
                1,
                Math.min(limit, 100)
        );

        Set<String> values = redisTemplate
                .opsForZSet()
                .rangeByScore(
                        PENDING_KEY,
                        0,
                        now.toEpochMilli(),
                        0,
                        safeLimit
                );

        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .map(this::parseOrderId)
                .toList();
    }

    private String buildJobKey(Long orderId) {
        validateOrderId(orderId);

        return JOB_KEY_PREFIX + orderId;
    }

    private Long parseOrderId(String value) {
        try {
            return Long.parseLong(value);

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Повреждён order ID в очереди Torgsoft: "
                            + value,
                    exception
            );
        }
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException(
                    "WooCommerce order ID должен быть положительным"
            );
        }
    }
}