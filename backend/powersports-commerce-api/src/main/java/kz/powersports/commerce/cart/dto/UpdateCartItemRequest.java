package kz.powersports.commerce.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(

        @NotNull(message = "Количество обязательно")
        @Min(
                value = 1,
                message = "Количество должно быть не меньше 1"
        )
        Integer quantity

) {
}