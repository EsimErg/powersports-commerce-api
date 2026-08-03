package kz.powersports.commerce.order.controller;

import jakarta.servlet.http.HttpSession;
import kz.powersports.commerce.common.exception.EmptyCartException;
import kz.powersports.commerce.common.exception.GlobalExceptionHandler;
import kz.powersports.commerce.common.exception.InvalidIdempotencyKeyException;
import kz.powersports.commerce.common.exception.OrderAlreadyProcessingException;
import kz.powersports.commerce.common.exception.OrderCreationException;
import kz.powersports.commerce.common.exception.OrderNotFoundException;
import kz.powersports.commerce.order.dto.CreateOrderRequest;
import kz.powersports.commerce.order.dto.OrderResponse;
import kz.powersports.commerce.order.dto.OrderStatusResponse;
import kz.powersports.commerce.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    private static final String IDEMPOTENCY_KEY =
            "550e8400-e29b-41d4-a716-446655440000";

    private static final Long ORDER_ID =
            16L;

    private static final String VALID_ORDER_JSON =
            """
            {
              "firstName": "Есым",
              "lastName": "Ергобек",
              "phone": "+77001234567",
              "email": "esym@example.com",
              "city": "Туркестан",
              "address": "Адрес согласовать по телефону",
              "comment": "Тестовый заказ"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    /*
     * Настоящий OrderService не запускается.
     * Spring подставит Mockito mock.
     */
    @MockitoBean
    private OrderService orderService;

    @Test
    void createOrderShouldReturn201() throws Exception {
        OrderResponse response =
                new OrderResponse(
                        ORDER_ID,
                        "16",
                        "on-hold",
                        new BigDecimal("340000.00"),
                        "KZT",
                        "Есым Ергобек",
                        "+77001234567"
                );

        when(
                orderService.createOrder(
                        any(HttpSession.class),
                        eq(IDEMPOTENCY_KEY),
                        any(CreateOrderRequest.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/orders")
                                .header(
                                        "Idempotency-Key",
                                        IDEMPOTENCY_KEY
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(VALID_ORDER_JSON)
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(16)
                )
                .andExpect(
                        jsonPath("$.number")
                                .value("16")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("on-hold")
                )
                .andExpect(
                        jsonPath("$.total")
                                .value(340000.00)
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("KZT")
                )
                .andExpect(
                        jsonPath("$.customerName")
                                .value("Есым Ергобек")
                )
                .andExpect(
                        jsonPath("$.phone")
                                .value("+77001234567")
                );
    }

    @Test
    void getOrderStatusShouldReturn200() throws Exception {
        OrderStatusResponse response =
                new OrderStatusResponse(
                        ORDER_ID,
                        "16",
                        "on-hold",
                        new BigDecimal("340000.00"),
                        "KZT"
                );

        when(
                orderService.getOrderStatus(
                        any(HttpSession.class),
                        eq(ORDER_ID)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get(
                                "/api/v1/orders/{orderId}/status",
                                ORDER_ID
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(16)
                )
                .andExpect(
                        jsonPath("$.number")
                                .value("16")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("on-hold")
                )
                .andExpect(
                        jsonPath("$.total")
                                .value(340000.00)
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("KZT")
                );
    }

    @Test
    void missingIdempotencyKeyShouldReturn400()
            throws Exception {

        when(
                orderService.createOrder(
                        any(HttpSession.class),
                        isNull(),
                        any(CreateOrderRequest.class)
                )
        ).thenThrow(
                new InvalidIdempotencyKeyException()
        );

        mockMvc.perform(
                        post("/api/v1/orders")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(VALID_ORDER_JSON)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Некорректный Idempotency-Key"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "INVALID_IDEMPOTENCY_KEY"
                                )
                );
    }

    @Test
    void invalidRequestBodyShouldReturn400()
            throws Exception {

        String invalidBody =
                """
                {
                  "firstName": "",
                  "lastName": "Ергобек",
                  "phone": "+77001234567",
                  "email": "esym@example.com",
                  "city": "Туркестан",
                  "address": "Адрес",
                  "comment": "Тест"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/orders")
                                .header(
                                        "Idempotency-Key",
                                        IDEMPOTENCY_KEY
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(invalidBody)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Ошибка валидации")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "REQUEST_VALIDATION_ERROR"
                                )
                );
    }

    @Test
    void emptyCartShouldReturn400() throws Exception {
        when(
                orderService.createOrder(
                        any(HttpSession.class),
                        eq(IDEMPOTENCY_KEY),
                        any(CreateOrderRequest.class)
                )
        ).thenThrow(
                new EmptyCartException()
        );

        mockMvc.perform(
                        post("/api/v1/orders")
                                .header(
                                        "Idempotency-Key",
                                        IDEMPOTENCY_KEY
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(VALID_ORDER_JSON)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Корзина пуста")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("EMPTY_CART")
                );
    }

    @Test
    void processingOrderShouldReturn409()
            throws Exception {

        when(
                orderService.createOrder(
                        any(HttpSession.class),
                        eq(IDEMPOTENCY_KEY),
                        any(CreateOrderRequest.class)
                )
        ).thenThrow(
                new OrderAlreadyProcessingException()
        );

        mockMvc.perform(
                        post("/api/v1/orders")
                                .header(
                                        "Idempotency-Key",
                                        IDEMPOTENCY_KEY
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(VALID_ORDER_JSON)
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Заказ уже обрабатывается"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "ORDER_ALREADY_PROCESSING"
                                )
                );
    }

    @Test
    void unknownOrderShouldReturn404()
            throws Exception {

        when(
                orderService.getOrderStatus(
                        any(HttpSession.class),
                        eq(999999L)
                )
        ).thenThrow(
                new OrderNotFoundException()
        );

        mockMvc.perform(
                        get(
                                "/api/v1/orders/{orderId}/status",
                                999999L
                        )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Заказ не найден")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("ORDER_NOT_FOUND")
                );
    }

    @Test
    void orderCreationFailureShouldReturn502()
            throws Exception {

        when(
                orderService.createOrder(
                        any(HttpSession.class),
                        eq(IDEMPOTENCY_KEY),
                        any(CreateOrderRequest.class)
                )
        ).thenThrow(
                new OrderCreationException(
                        "Тестовая ошибка WooCommerce"
                )
        );

        mockMvc.perform(
                        post("/api/v1/orders")
                                .header(
                                        "Idempotency-Key",
                                        IDEMPOTENCY_KEY
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(VALID_ORDER_JSON)
                )
                .andExpect(
                        status().isBadGateway()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Ошибка создания заказа"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(502)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "ORDER_CREATION_ERROR"
                                )
                );
    }
}