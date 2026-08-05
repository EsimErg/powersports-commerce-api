package kz.powersports.commerce.order.service;

import jakarta.servlet.http.HttpSession;
import kz.powersports.commerce.cart.dto.CartItemResponse;
import kz.powersports.commerce.cart.dto.CartResponse;
import kz.powersports.commerce.cart.service.CartService;
import kz.powersports.commerce.common.exception.EmptyCartException;
import kz.powersports.commerce.common.exception.InvalidIdempotencyKeyException;
import kz.powersports.commerce.common.exception.OrderAlreadyProcessingException;
import kz.powersports.commerce.order.client.WooCommerceOrderClient;
import kz.powersports.commerce.order.client.dto.WooCommerceBillingAddress;
import kz.powersports.commerce.order.client.dto.WooCommerceOrderCreateRequest;
import kz.powersports.commerce.order.client.dto.WooCommerceOrderLineItemRequest;
import kz.powersports.commerce.order.client.dto.WooCommerceOrderMetaData;
import kz.powersports.commerce.order.client.dto.WooCommerceOrderResponse;
import kz.powersports.commerce.order.client.dto.WooCommerceShippingAddress;
import kz.powersports.commerce.order.dto.CreateOrderRequest;
import kz.powersports.commerce.order.dto.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import kz.powersports.commerce.common.exception.OrderNotFoundException;
import kz.powersports.commerce.order.dto.OrderStatusResponse;
import kz.powersports.commerce.torgsoft.order.export.TorgsoftOrderExportQueueService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class OrderService {

    private static final Logger log =
            LoggerFactory.getLogger(OrderService.class);

    /*
     * Разрешаем UUID и обычные безопасные ключи.
     *
     * Минимум: 8 символов.
     * Максимум: 100 символов.
     */
    public OrderStatusResponse getOrderStatus(
            HttpSession session,
            Long orderId
    ) {
        if (orderId == null || orderId <= 0) {
            throw new OrderNotFoundException();
        }

        String sessionAttributeName =
                buildOrderAccessSessionKey(
                        orderId
                );

        Object storedValue =
                session.getAttribute(
                        sessionAttributeName
                );

        if (!(storedValue instanceof String storedOrderKey)
                || storedOrderKey.isBlank()) {
            /*
             * В текущей сессии этот заказ
             * никогда не создавался.
             */
            throw new OrderNotFoundException();
        }

        WooCommerceOrderResponse order =
                orderClient.getOrder(
                        orderId
                );

        if (order.orderKey() == null
                || !secureEquals(
                storedOrderKey,
                order.orderKey()
        )) {
            /*
             * Не сообщаем, что заказ существует,
             * если проверка доступа не прошла.
             */
            throw new OrderNotFoundException();
        }

        return new OrderStatusResponse(
                order.id(),
                order.number(),
                order.status(),
                convertTotal(order.total()),
                order.currency()
        );
    }
    private static final Pattern IDEMPOTENCY_KEY_PATTERN =
            Pattern.compile(
                    "^[A-Za-z0-9._:-]{8,100}$"
            );
    private static final String ORDER_ACCESS_SESSION_PREFIX =
            "powersports:order-access:";
    private final CartService cartService;
    private final WooCommerceOrderClient orderClient;
    private final OrderIdempotencyService idempotencyService;

    private final Optional<TorgsoftOrderExportQueueService>
            torgsoftOrderExportQueueService;

    public OrderService(
            CartService cartService,
            WooCommerceOrderClient orderClient,
            OrderIdempotencyService idempotencyService,
            Optional<TorgsoftOrderExportQueueService>
                    torgsoftOrderExportQueueService
    ) {
        this.cartService = cartService;
        this.orderClient = orderClient;
        this.idempotencyService = idempotencyService;
        this.torgsoftOrderExportQueueService =
                torgsoftOrderExportQueueService;
    }

    public OrderResponse createOrder(
            HttpSession session,
            String idempotencyKey,
            CreateOrderRequest request
    ) {
        String normalizedIdempotencyKey =
                validateIdempotencyKey(
                        idempotencyKey
                );

        String sessionId =
                session.getId();

        /*
         * Если этот запрос уже успешно выполнялся,
         * возвращаем старый результат.
         *
         * Корзину при этом не проверяем, потому что
         * после первого заказа она уже была отвязана.
         */
        OrderResponse completedResponse =
                idempotencyService
                        .findCompleted(
                                sessionId,
                                normalizedIdempotencyKey
                        )
                        .orElse(null);

        if (completedResponse != null) {
            log.info(
                    "Возвращаем ранее созданный заказ. Idempotency-Key: {}",
                    normalizedIdempotencyKey
            );

            return completedResponse;
        }

        /*
         * Атомарно резервируем ключ в Redis.
         */
        boolean started =
                idempotencyService.tryStart(
                        sessionId,
                        normalizedIdempotencyKey
                );

        if (!started) {
            /*
             * Между первой проверкой и этой строкой
             * другой запрос мог успеть завершиться.
             */
            return idempotencyService
                    .findCompleted(
                            sessionId,
                            normalizedIdempotencyKey
                    )
                    .orElseThrow(
                            OrderAlreadyProcessingException::new
                    );
        }

        boolean orderCreatedInWooCommerce =
                false;

        try {
            CartResponse cart =
                    cartService.getCart(session);

            if (cart.items() == null
                    || cart.items().isEmpty()) {
                throw new EmptyCartException();
            }

            List<WooCommerceOrderLineItemRequest> lineItems =
                    cart.items()
                            .stream()
                            .map(this::toLineItem)
                            .toList();

            String lastName =
                    normalize(request.lastName());

            String city =
                    normalize(request.city());

            String address =
                    normalize(request.address());

            WooCommerceBillingAddress billing =
                    new WooCommerceBillingAddress(
                            request.firstName().trim(),
                            lastName,
                            address,
                            city,
                            "KZ",
                            normalize(request.email()),
                            request.phone().trim()
                    );

            WooCommerceShippingAddress shipping =
                    new WooCommerceShippingAddress(
                            request.firstName().trim(),
                            lastName,
                            address,
                            city,
                            "KZ"
                    );

            List<WooCommerceOrderMetaData> metaData =
                    List.of(
                            new WooCommerceOrderMetaData(
                                    "_powersports_source",
                                    "framer"
                            ),
                            new WooCommerceOrderMetaData(
                                    "_delivery_calculation",
                                    "not_required"
                            ),
                            new WooCommerceOrderMetaData(
                                    "_torgsoft_export_status",
                                    "pending"
                            ),
                            new WooCommerceOrderMetaData(
                                    "_powersports_idempotency_key",
                                    normalizedIdempotencyKey
                            ),
                            new WooCommerceOrderMetaData(
                                    "_powersports_session_id",
                                    sessionId
                            )
                    );

            WooCommerceOrderCreateRequest wooRequest =
                    new WooCommerceOrderCreateRequest(
                            "on-hold",
                            false,
                            billing,
                            shipping,
                            normalize(request.comment()),
                            lineItems,
                            metaData
                    );

            WooCommerceOrderResponse createdOrder =
                    orderClient.createOrder(
                            wooRequest
                    );

            orderCreatedInWooCommerce = true;

            rememberOrderAccess(
                    session,
                    createdOrder
            );
            OrderResponse response =
                    new OrderResponse(
                            createdOrder.id(),
                            createdOrder.number(),
                            createdOrder.status(),
                            convertTotal(
                                    createdOrder.total()
                            ),
                            createdOrder.currency(),
                            buildCustomerName(
                                    request.firstName(),
                                    request.lastName()
                            ),
                            request.phone()
                    );

            /*
             * Сохраняем ответ для повторных запросов
             * с тем же Idempotency-Key.
             */
            idempotencyService.complete(
                    sessionId,
                    normalizedIdempotencyKey,
                    response
            );

            /*
             * Ошибка очистки корзины уже не должна
             * превращать успешный заказ в ошибочный.
             */
            try {
                cartService.detachCart(session);
            } catch (RuntimeException exception) {
                log.warn(
                        "Заказ создан, но корзину не удалось отвязать. Order ID: {}",
                        createdOrder.id(),
                        exception
                );
            }
            enqueueTorgsoftExportSafely(
                    response.id(),
                    response.number()
            );
            return response;

        } catch (RuntimeException exception) {
            /*
             * Освобождаем ключ только тогда,
             * когда заказ ещё не был создан
             * в WooCommerce.
             */
            if (!orderCreatedInWooCommerce) {
                releaseIdempotencyKeySafely(
                        sessionId,
                        normalizedIdempotencyKey
                );
            } else {
                log.error(
                        """
                        Заказ уже создан в WooCommerce,
                        но завершение локальной обработки завершилось ошибкой.
                        Idempotency-Key: {}
                        """,
                        normalizedIdempotencyKey,
                        exception
                );
            }

            throw exception;
        }
    }

    private void releaseIdempotencyKeySafely(
            String sessionId,
            String idempotencyKey
    ) {
        try {
            idempotencyService.release(
                    sessionId,
                    idempotencyKey
            );
        } catch (RuntimeException redisException) {
            log.error(
                    "Не удалось освободить Idempotency-Key: {}",
                    idempotencyKey,
                    redisException
            );
        }
    }

    private String validateIdempotencyKey(
            String value
    ) {
        if (value == null) {
            throw new InvalidIdempotencyKeyException();
        }

        String normalized =
                value.trim();

        if (!IDEMPOTENCY_KEY_PATTERN
                .matcher(normalized)
                .matches()) {
            throw new InvalidIdempotencyKeyException();
        }

        return normalized;
    }

    private WooCommerceOrderLineItemRequest toLineItem(
            CartItemResponse item
    ) {
        return new WooCommerceOrderLineItemRequest(
                item.productId(),
                item.quantity()
        );
    }

    private BigDecimal convertTotal(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String buildCustomerName(
            String firstName,
            String lastName
    ) {
        String normalizedLastName =
                normalize(lastName);

        if (normalizedLastName.isBlank()) {
            return firstName.trim();
        }

        return firstName.trim()
                + " "
                + normalizedLastName;
    }

    private String normalize(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }
    private void rememberOrderAccess(
            HttpSession session,
            WooCommerceOrderResponse order
    ) {
        if (order.id() == null
                || order.orderKey() == null
                || order.orderKey().isBlank()) {
            log.error(
                    """
                    Заказ создан, но WooCommerce не вернул
                    id или order_key. Статус заказа будет недоступен.
                    """
            );

            return;
        }

        try {
            session.setAttribute(
                    buildOrderAccessSessionKey(
                            order.id()
                    ),
                    order.orderKey()
            );
        } catch (RuntimeException exception) {
            /*
             * Ошибка сохранения доступа не должна
             * создавать второй заказ.
             */
            log.error(
                    """
                    Заказ создан, но не удалось сохранить
                    доступ к нему в пользовательской сессии.
                    Order ID: {}
                    """,
                    order.id(),
                    exception
            );
        }

    }

    private String buildOrderAccessSessionKey(
            Long orderId
    ) {
        return ORDER_ACCESS_SESSION_PREFIX
                + orderId;
    }

    private boolean secureEquals(
            String expected,
            String actual
    ) {
        byte[] expectedBytes =
                expected.getBytes(
                        StandardCharsets.UTF_8
                );

        byte[] actualBytes =
                actual.getBytes(
                        StandardCharsets.UTF_8
                );

        return MessageDigest.isEqual(
                expectedBytes,
                actualBytes
        );
    }
    private void enqueueTorgsoftExportSafely(
            Long orderId,
            String orderNumber
    ) {
        torgsoftOrderExportQueueService.ifPresent(
                queueService -> {
                    try {
                        queueService.enqueue(
                                orderId,
                                orderNumber
                        );

                    } catch (RuntimeException exception) {
                        /*
                         * Заказ WooCommerce уже создан.
                         * Ошибка Redis/Torgsoft не должна отменять
                         * оформление заказа покупателя.
                         */
                        log.error(
                                "Заказ создан, но не поставлен "
                                        + "в очередь экспорта Torgsoft. "
                                        + "Order ID: {}, номер: {}",
                                orderId,
                                orderNumber,
                                exception
                        );
                    }
                }
        );
    }
}