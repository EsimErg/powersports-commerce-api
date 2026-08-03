package kz.powersports.commerce.order.service;

import kz.powersports.commerce.order.dto.OrderResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

@Service
public class OrderIdempotencyService {

    private static final String KEY_PREFIX =
            "powersports:order:idempotency:";

    /*
     * Если создание заказа зависло, блокировка
     * автоматически исчезнет через 10 минут.
     */
    private static final Duration PROCESSING_TTL =
            Duration.ofMinutes(10);

    /*
     * Успешный результат сохраняем на сутки,
     * чтобы повторный запрос вернул тот же заказ.
     */
    private static final Duration COMPLETED_TTL =
            Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    public OrderIdempotencyService(
            StringRedisTemplate redisTemplate,
            JsonMapper jsonMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Пытается атомарно зарезервировать ключ.
     *
     * true  — этот запрос первый;
     * false — такой запрос уже выполняется
     *         или был выполнен раньше.
     */
    public boolean tryStart(
            String sessionId,
            String idempotencyKey
    ) {
        String redisKey =
                buildRedisKey(
                        sessionId,
                        idempotencyKey
                );

        StoredOrder processing =
                new StoredOrder(
                        State.PROCESSING,
                        null
                );

        Boolean created =
                redisTemplate
                        .opsForValue()
                        .setIfAbsent(
                                redisKey,
                                serialize(processing),
                                PROCESSING_TTL
                        );

        return Boolean.TRUE.equals(created);
    }

    /**
     * Возвращает сохранённый результат,
     * если заказ уже был успешно создан.
     */
    public Optional<OrderResponse> findCompleted(
            String sessionId,
            String idempotencyKey
    ) {
        StoredOrder storedOrder =
                findStoredOrder(
                        sessionId,
                        idempotencyKey
                );

        if (storedOrder == null
                || storedOrder.state()
                != State.COMPLETED
                || storedOrder.response() == null) {
            return Optional.empty();
        }

        return Optional.of(
                storedOrder.response()
        );
    }

    /**
     * Проверяет, создаётся ли заказ прямо сейчас.
     */
    public boolean isProcessing(
            String sessionId,
            String idempotencyKey
    ) {
        StoredOrder storedOrder =
                findStoredOrder(
                        sessionId,
                        idempotencyKey
                );

        return storedOrder != null
                && storedOrder.state()
                == State.PROCESSING;
    }

    /**
     * Сохраняет результат успешного создания заказа.
     */
    public void complete(
            String sessionId,
            String idempotencyKey,
            OrderResponse response
    ) {
        String redisKey =
                buildRedisKey(
                        sessionId,
                        idempotencyKey
                );

        StoredOrder completed =
                new StoredOrder(
                        State.COMPLETED,
                        response
                );

        redisTemplate
                .opsForValue()
                .set(
                        redisKey,
                        serialize(completed),
                        COMPLETED_TTL
                );
    }

    /**
     * При ошибке освобождает ключ,
     * чтобы пользователь мог повторить заказ.
     */
    public void release(
            String sessionId,
            String idempotencyKey
    ) {
        redisTemplate.delete(
                buildRedisKey(
                        sessionId,
                        idempotencyKey
                )
        );
    }

    private StoredOrder findStoredOrder(
            String sessionId,
            String idempotencyKey
    ) {
        String value =
                redisTemplate
                        .opsForValue()
                        .get(
                                buildRedisKey(
                                        sessionId,
                                        idempotencyKey
                                )
                        );

        if (value == null || value.isBlank()) {
            return null;
        }

        return deserialize(value);
    }

    private String buildRedisKey(
            String sessionId,
            String idempotencyKey
    ) {
        if (sessionId == null
                || sessionId.isBlank()) {
            throw new IllegalArgumentException(
                    "Session ID не должен быть пустым"
            );
        }

        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key не должен быть пустым"
            );
        }

        return KEY_PREFIX
                + sessionId
                + ":"
                + idempotencyKey.trim();
    }

    private String serialize(
            StoredOrder storedOrder
    ) {
        try {
            return jsonMapper.writeValueAsString(
                    storedOrder
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Не удалось сохранить idempotency-результат",
                    exception
            );
        }
    }

    private StoredOrder deserialize(
            String value
    ) {
        try {
            return jsonMapper.readValue(
                    value,
                    StoredOrder.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Не удалось прочитать idempotency-результат",
                    exception
            );
        }
    }

    private enum State {
        PROCESSING,
        COMPLETED
    }

    private record StoredOrder(
            State state,
            OrderResponse response
    ) {
    }
}