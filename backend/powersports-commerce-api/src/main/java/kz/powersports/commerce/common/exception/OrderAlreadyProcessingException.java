package kz.powersports.commerce.common.exception;

public class OrderAlreadyProcessingException
        extends RuntimeException {

    public OrderAlreadyProcessingException() {
        super(
                "Заказ с таким Idempotency-Key уже создаётся"
        );
    }
}