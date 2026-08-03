package kz.powersports.commerce.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(
            ProductNotFoundException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle("Товар не найден");
        problem.setProperty(
                "code",
                "PRODUCT_NOT_FOUND"
        );

        return problem;
    }

    @ExceptionHandler(
            WooCommerceUnavailableException.class
    )
    public ProblemDetail handleWooCommerceUnavailable(
            WooCommerceUnavailableException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Каталог временно недоступен"
                );

        problem.setTitle(
                "WooCommerce недоступен"
        );

        problem.setProperty(
                "code",
                "WOOCOMMERCE_UNAVAILABLE"
        );

        return problem;
    }

    @ExceptionHandler(WooCommerceApiException.class)
    public ProblemDetail handleWooCommerceApi(
            WooCommerceApiException exception
    ) {
        HttpStatus responseStatus;

        String title;

        switch (exception.getUpstreamStatus()) {
            case 400 -> {
                responseStatus = HttpStatus.BAD_REQUEST;
                title = "Некорректная операция";
            }

            case 404 -> {
                responseStatus = HttpStatus.NOT_FOUND;
                title = "Ресурс не найден";
            }

            case 409 -> {
                responseStatus = HttpStatus.CONFLICT;
                title = "Конфликт состояния корзины";
            }

            default -> {
                responseStatus = HttpStatus.BAD_GATEWAY;
                title = "Ошибка внешнего сервиса";
            }
        }

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        responseStatus,
                        "WooCommerce не смог выполнить операцию"
                );

        problem.setTitle(title);

        problem.setProperty(
                "code",
                "WOOCOMMERCE_API_ERROR"
        );

        problem.setProperty(
                "upstreamStatus",
                exception.getUpstreamStatus()
        );

        return problem;
    }

    @ExceptionHandler(
            ConstraintViolationException.class
    )
    public ProblemDetail handleValidation(
            ConstraintViolationException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        exception.getMessage()
                );

        problem.setTitle(
                "Некорректные параметры запроса"
        );

        problem.setProperty(
                "code",
                "VALIDATION_ERROR"
        );

        return problem;
    }
    @ExceptionHandler(
            CartInitializationException.class
    )
    public ProblemDetail handleCartInitialization(
            CartInitializationException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_GATEWAY,
                        "Не удалось создать корзину"
                );

        problem.setTitle(
                "Ошибка инициализации корзины"
        );

        problem.setProperty(
                "code",
                "CART_INITIALIZATION_ERROR"
        );

        return problem;
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ProblemDetail handleRequestValidation(
            MethodArgumentNotValidException exception
    ) {
        String detail =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error ->
                                error.getField()
                                        + ": "
                                        + error.getDefaultMessage()
                        )
                        .findFirst()
                        .orElse(
                                "Некорректное тело запроса"
                        );

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        detail
                );

        problem.setTitle(
                "Ошибка валидации"
        );

        problem.setProperty(
                "code",
                "REQUEST_VALIDATION_ERROR"
        );

        return problem;
    }
    @ExceptionHandler(EmptyCartException.class)
    public ProblemDetail handleEmptyCart(
            EmptyCartException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        exception.getMessage()
                );

        problem.setTitle("Корзина пуста");
        problem.setProperty(
                "code",
                "EMPTY_CART"
        );

        return problem;
    }

    @ExceptionHandler(OrderCreationException.class)
    public ProblemDetail handleOrderCreation(
            OrderCreationException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_GATEWAY,
                        "Не удалось создать заказ"
                );

        problem.setTitle(
                "Ошибка создания заказа"
        );

        problem.setProperty(
                "code",
                "ORDER_CREATION_ERROR"
        );

        return problem;
    }
    @ExceptionHandler(
            InvalidIdempotencyKeyException.class
    )
    public ProblemDetail handleInvalidIdempotencyKey(
            InvalidIdempotencyKeyException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        exception.getMessage()
                );

        problem.setTitle(
                "Некорректный Idempotency-Key"
        );

        problem.setProperty(
                "code",
                "INVALID_IDEMPOTENCY_KEY"
        );

        return problem;
    }

    @ExceptionHandler(
            OrderAlreadyProcessingException.class
    )
    public ProblemDetail handleOrderAlreadyProcessing(
            OrderAlreadyProcessingException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problem.setTitle(
                "Заказ уже обрабатывается"
        );

        problem.setProperty(
                "code",
                "ORDER_ALREADY_PROCESSING"
        );

        return problem;
    }
    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(
            OrderNotFoundException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle(
                "Заказ не найден"
        );

        problem.setProperty(
                "code",
                "ORDER_NOT_FOUND"
        );

        return problem;
    }
}
