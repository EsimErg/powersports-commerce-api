package kz.powersports.commerce.common.exception;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Нельзя создать заказ из пустой корзины");
    }
}