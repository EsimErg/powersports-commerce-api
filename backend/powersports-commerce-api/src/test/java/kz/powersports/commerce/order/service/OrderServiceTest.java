package kz.powersports.commerce.order.service;

import jakarta.servlet.http.HttpSession;
import kz.powersports.commerce.cart.dto.CartItemResponse;
import kz.powersports.commerce.cart.dto.CartResponse;
import kz.powersports.commerce.cart.service.CartService;
import kz.powersports.commerce.common.exception.EmptyCartException;
import kz.powersports.commerce.common.exception.InvalidIdempotencyKeyException;
import kz.powersports.commerce.common.exception.OrderAlreadyProcessingException;
import kz.powersports.commerce.common.exception.OrderCreationException;
import kz.powersports.commerce.common.exception.OrderNotFoundException;
import kz.powersports.commerce.order.client.WooCommerceOrderClient;
import kz.powersports.commerce.order.client.dto.WooCommerceOrderCreateRequest;
import kz.powersports.commerce.order.client.dto.WooCommerceOrderResponse;
import kz.powersports.commerce.order.dto.CreateOrderRequest;
import kz.powersports.commerce.order.dto.OrderResponse;
import kz.powersports.commerce.order.dto.OrderStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String SESSION_ID =
            "test-session-123";

    private static final String IDEMPOTENCY_KEY =
            "550e8400-e29b-41d4-a716-446655440000";

    private static final Long ORDER_ID =
            15L;

    private static final String ORDER_KEY =
            "wc_order_test_secret_key";

    @Mock
    private CartService cartService;

    @Mock
    private WooCommerceOrderClient orderClient;

    @Mock
    private OrderIdempotencyService idempotencyService;

    @Mock
    private HttpSession session;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService =
                new OrderService(
                        cartService,
                        orderClient,
                        idempotencyService
                );
    }

    @Test
    void createOrderShouldCreateWooCommerceOrder() {
        CreateOrderRequest request =
                createCustomerRequest();

        CartResponse cart =
                createCartWithOneItem();

        WooCommerceOrderResponse wooCommerceResponse =
                createWooCommerceResponse();

        when(session.getId())
                .thenReturn(SESSION_ID);

        when(
                idempotencyService.findCompleted(
                        SESSION_ID,
                        IDEMPOTENCY_KEY
                )
        ).thenReturn(Optional.empty());

        when(
                idempotencyService.tryStart(
                        SESSION_ID,
                        IDEMPOTENCY_KEY
                )
        ).thenReturn(true);

        when(cartService.getCart(session))
                .thenReturn(cart);

        when(
                orderClient.createOrder(
                        any(WooCommerceOrderCreateRequest.class)
                )
        ).thenReturn(wooCommerceResponse);

        OrderResponse result =
                orderService.createOrder(
                        session,
                        IDEMPOTENCY_KEY,
                        request
                );

        assertThat(result.id())
                .isEqualTo(ORDER_ID);

        assertThat(result.number())
                .isEqualTo("15");

        assertThat(result.status())
                .isEqualTo("on-hold");

        assertThat(result.total())
                .isEqualByComparingTo(
                        new BigDecimal("340000.00")
                );

        assertThat(result.currency())
                .isEqualTo("KZT");

        assertThat(result.customerName())
                .isEqualTo("Есым Ергобек");

        assertThat(result.phone())
                .isEqualTo("+77001234567");

        ArgumentCaptor<WooCommerceOrderCreateRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        WooCommerceOrderCreateRequest.class
                );

        verify(orderClient)
                .createOrder(
                        requestCaptor.capture()
                );

        WooCommerceOrderCreateRequest wooRequest =
                requestCaptor.getValue();

        assertThat(wooRequest.status())
                .isEqualTo("on-hold");

        assertThat(wooRequest.setPaid())
                .isFalse();

        assertThat(wooRequest.billing().firstName())
                .isEqualTo("Есым");

        assertThat(wooRequest.billing().lastName())
                .isEqualTo("Ергобек");

        assertThat(wooRequest.billing().phone())
                .isEqualTo("+77001234567");

        assertThat(wooRequest.billing().country())
                .isEqualTo("KZ");

        assertThat(wooRequest.lineItems())
                .hasSize(1);

        assertThat(
                wooRequest.lineItems()
                        .get(0)
                        .productId()
        ).isEqualTo(12L);

        assertThat(
                wooRequest.lineItems()
                        .get(0)
                        .quantity()
        ).isEqualTo(1);

        assertThat(wooRequest.metaData())
                .anySatisfy(metaData -> {
                    assertThat(metaData.key())
                            .isEqualTo(
                                    "_powersports_idempotency_key"
                            );

                    assertThat(metaData.value())
                            .isEqualTo(
                                    IDEMPOTENCY_KEY
                            );
                });

        verify(session)
                .setAttribute(
                        "powersports:order-access:" + ORDER_ID,
                        ORDER_KEY
                );

        verify(idempotencyService)
                .complete(
                        SESSION_ID,
                        IDEMPOTENCY_KEY,
                        result
                );

        verify(cartService)
                .detachCart(session);
    }

    @Test
    void repeatedRequestShouldReturnCompletedOrder() {
        OrderResponse completedOrder =
                createOrderResponse();

        when(session.getId())
                .thenReturn(SESSION_ID);

        when(
                idempotencyService.findCompleted(
                        SESSION_ID,
                        IDEMPOTENCY_KEY
                )
        ).thenReturn(
                Optional.of(completedOrder)
        );

        OrderResponse result =
                orderService.createOrder(
                        session,
                        IDEMPOTENCY_KEY,
                        createCustomerRequest()
                );

        assertThat(result)
                .isSameAs(completedOrder);

        verify(
                idempotencyService,
                never()
        ).tryStart(
                SESSION_ID,
                IDEMPOTENCY_KEY
        );

        verifyNoInteractions(
                cartService,
                orderClient
        );
    }

    @Test
    void requestBeingProcessedShouldThrowConflictException() {
        when(session.getId())
                .thenReturn(SESSION_ID);

        when(
                idempotencyService.findCompleted(
                        SESSION_ID,
                        IDEMPOTENCY_KEY
                )
        ).thenReturn(
                Optional.empty(),
                Optional.empty()
        );

        when(
                idempotencyService.tryStart(
                        SESSION_ID,
                        IDEMPOTENCY_KEY
                )
        ).thenReturn(false);

        assertThatThrownBy(() ->
                orderService.createOrder(
                        session,
                        IDEMPOTENCY_KEY,
                        createCustomerRequest()
                )
        ).isInstanceOf(
                OrderAlreadyProcessingException.class
        );

        verifyNoInteractions(
                cartService,
                orderClient
        );
    }

    @Test
    void emptyCartShouldReleaseIdempotencyKey() {
        when(session.getId())
                .thenReturn(SESSION_ID);

        when(
                idempotencyService.findCompleted(
                        SESSION_ID,
                        IDEMPOTENCY_KEY
                )
        ).thenReturn(Optional.empty());

        when(
                idempotencyService.tryStart(
                        SESSION_ID,
                        IDEMPOTENCY_KEY
                )
        ).thenReturn(true);

        when(cartService.getCart(session))
                .thenReturn(createEmptyCart());

        assertThatThrownBy(() ->
                orderService.createOrder(
                        session,
                        IDEMPOTENCY_KEY,
                        createCustomerRequest()
                )
        ).isInstanceOf(
                EmptyCartException.class
        );

        verify(idempotencyService)
                .release(
                        SESSION_ID,
                        IDEMPOTENCY_KEY
                );

        verify(
                orderClient,
                never()
        ).createOrder(any());

        verify(
                idempotencyService,
                never()
        ).complete(
                any(),
                any(),
                any()
        );

        verify(
                cartService,
                never()
        ).detachCart(session);
    }

    @Test
    void wooCommerceFailureShouldReleaseIdempotencyKey() {
        when(session.getId())
                .thenReturn(SESSION_ID);

        when(
                idempotencyService.findCompleted(
                        SESSION_ID,
                        IDEMPOTENCY_KEY
                )
        ).thenReturn(Optional.empty());

        when(
                idempotencyService.tryStart(
                        SESSION_ID,
                        IDEMPOTENCY_KEY
                )
        ).thenReturn(true);

        when(cartService.getCart(session))
                .thenReturn(
                        createCartWithOneItem()
                );

        when(
                orderClient.createOrder(
                        any(WooCommerceOrderCreateRequest.class)
                )
        ).thenThrow(
                new OrderCreationException(
                        "Тестовая ошибка WooCommerce"
                )
        );

        assertThatThrownBy(() ->
                orderService.createOrder(
                        session,
                        IDEMPOTENCY_KEY,
                        createCustomerRequest()
                )
        ).isInstanceOf(
                OrderCreationException.class
        );

        verify(idempotencyService)
                .release(
                        SESSION_ID,
                        IDEMPOTENCY_KEY
                );

        verify(
                idempotencyService,
                never()
        ).complete(
                any(),
                any(),
                any()
        );

        verify(
                cartService,
                never()
        ).detachCart(session);
    }

    @Test
    void getOrderStatusShouldReturnOrderForOwnerSession() {
        when(
                session.getAttribute(
                        "powersports:order-access:" + ORDER_ID
                )
        ).thenReturn(ORDER_KEY);

        when(orderClient.getOrder(ORDER_ID))
                .thenReturn(
                        createWooCommerceResponse()
                );

        OrderStatusResponse result =
                orderService.getOrderStatus(
                        session,
                        ORDER_ID
                );

        assertThat(result.id())
                .isEqualTo(ORDER_ID);

        assertThat(result.number())
                .isEqualTo("15");

        assertThat(result.status())
                .isEqualTo("on-hold");

        assertThat(result.total())
                .isEqualByComparingTo(
                        new BigDecimal("340000.00")
                );

        assertThat(result.currency())
                .isEqualTo("KZT");
    }

    @Test
    void getOrderStatusShouldRejectForeignSession() {
        when(
                session.getAttribute(
                        "powersports:order-access:" + ORDER_ID
                )
        ).thenReturn(null);

        assertThatThrownBy(() ->
                orderService.getOrderStatus(
                        session,
                        ORDER_ID
                )
        ).isInstanceOf(
                OrderNotFoundException.class
        );

        verifyNoInteractions(orderClient);
    }

    @Test
    void invalidIdempotencyKeyShouldBeRejected() {
        assertThatThrownBy(() ->
                orderService.createOrder(
                        session,
                        "abc",
                        createCustomerRequest()
                )
        ).isInstanceOf(
                InvalidIdempotencyKeyException.class
        );

        verifyNoInteractions(
                cartService,
                orderClient,
                idempotencyService
        );
    }

    private CreateOrderRequest createCustomerRequest() {
        return new CreateOrderRequest(
                "Есым",
                "Ергобек",
                "+77001234567",
                "esym@example.com",
                "Туркестан",
                "Адрес согласовать по телефону",
                "Тестовый заказ"
        );
    }

    private CartResponse createCartWithOneItem() {
        CartItemResponse item =
                new CartItemResponse(
                        "cart-item-key",
                        12L,
                        "Беговая дорожка PowerRun X1",
                        "POWERRUN-X1",
                        1,
                        new BigDecimal("340000.00"),
                        new BigDecimal("340000.00"),
                        new BigDecimal("340000.00"),
                        "KZT",
                        "http://localhost/test-image.jpg"
                );

        return new CartResponse(
                List.of(item),
                1,
                new BigDecimal("340000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("340000.00"),
                BigDecimal.ZERO,
                "KZT",
                true,
                true
        );
    }

    private CartResponse createEmptyCart() {
        return new CartResponse(
                List.of(),
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "KZT",
                false,
                false
        );
    }

    private WooCommerceOrderResponse createWooCommerceResponse() {
        return new WooCommerceOrderResponse(
                ORDER_ID,
                "15",
                ORDER_KEY,
                "on-hold",
                "340000.00",
                "KZT",
                null
        );
    }

    private OrderResponse createOrderResponse() {
        return new OrderResponse(
                ORDER_ID,
                "15",
                "on-hold",
                new BigDecimal("340000.00"),
                "KZT",
                "Есым Ергобек",
                "+77001234567"
        );
    }
}