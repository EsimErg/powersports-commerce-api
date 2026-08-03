package kz.powersports.commerce.common.exception;

public class OrderNotFoundException
        extends RuntimeException {

    public OrderNotFoundException() {
        super("Заказ не найден");
    }
}