package kz.powersports.commerce.order.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kz.powersports.commerce.order.dto.CreateOrderRequest;
import kz.powersports.commerce.order.dto.OrderResponse;
import kz.powersports.commerce.order.dto.OrderStatusResponse;
import kz.powersports.commerce.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(
            OrderService orderService
    ) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(

            @RequestHeader(
                    name = "Idempotency-Key",
                    required = false
            )
            String idempotencyKey,

            @Valid
            @RequestBody
            CreateOrderRequest request,

            HttpSession session
    ) {
        return orderService.createOrder(
                session,
                idempotencyKey,
                request
        );
    }

    @GetMapping("/{orderId}/status")
    public OrderStatusResponse getOrderStatus(

            @PathVariable
            Long orderId,

            HttpSession session
    ) {
        return orderService.getOrderStatus(
                session,
                orderId
        );
    }
}