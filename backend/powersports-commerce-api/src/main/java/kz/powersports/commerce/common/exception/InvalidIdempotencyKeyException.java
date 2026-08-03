package kz.powersports.commerce.common.exception;

public class InvalidIdempotencyKeyException
        extends RuntimeException {

    public InvalidIdempotencyKeyException() {
        super(
                "Заголовок Idempotency-Key отсутствует или имеет некорректный формат"
        );
    }
}