package kz.powersports.commerce.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(

        @NotBlank(message = "Имя обязательно")
        @Size(max = 100, message = "Имя слишком длинное")
        String firstName,

        @Size(max = 100, message = "Фамилия слишком длинная")
        String lastName,

        @NotBlank(message = "Телефон обязателен")
        @Pattern(
                regexp = "^[0-9+()\\-\\s]{7,25}$",
                message = "Некорректный номер телефона"
        )
        String phone,

        @Email(message = "Некорректный email")
        @Size(max = 150, message = "Email слишком длинный")
        String email,

        @Size(max = 100, message = "Название города слишком длинное")
        String city,

        @Size(max = 250, message = "Адрес слишком длинный")
        String address,

        @Size(max = 1000, message = "Комментарий слишком длинный")
        String comment

) {
}
