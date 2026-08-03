package kz.powersports.commerce.cart.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.powersports.commerce.cart.dto.AddCartItemRequest;
import kz.powersports.commerce.cart.dto.CartResponse;
import kz.powersports.commerce.cart.dto.UpdateCartItemRequest;
import kz.powersports.commerce.cart.service.CartService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@Validated
public class CartController {

    private final CartService cartService;

    public CartController(
            CartService cartService
    ) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(
            HttpSession session
    ) {
        return cartService.getCart(session);
    }

    @PostMapping("/items")
    public CartResponse addItem(

            @Valid
            @RequestBody
            AddCartItemRequest request,

            HttpSession session
    ) {
        return cartService.addItem(
                session,
                request
        );
    }

    @PatchMapping("/items/{key}")
    public CartResponse updateItem(

            @PathVariable
            @NotBlank(message = "Ключ позиции обязателен")
            @Size(
                    max = 200,
                    message = "Некорректный ключ позиции"
            )
            String key,

            @Valid
            @RequestBody
            UpdateCartItemRequest request,

            HttpSession session
    ) {
        return cartService.updateItem(
                session,
                key,
                request
        );
    }

    @DeleteMapping("/items/{key}")
    public CartResponse removeItem(

            @PathVariable
            @NotBlank(message = "Ключ позиции обязателен")
            @Size(
                    max = 200,
                    message = "Некорректный ключ позиции"
            )
            String key,

            HttpSession session
    ) {
        return cartService.removeItem(
                session,
                key
        );
    }

    @DeleteMapping("/items")
    public CartResponse clearCart(
            HttpSession session
    ) {
        return cartService.clearCart(session);
    }
}