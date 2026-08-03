package kz.powersports.commerce.common.exception;

public class WooCommerceUnavailableException extends RuntimeException {

    public WooCommerceUnavailableException(Throwable cause) {
        super("Не удалось подключиться к WooCommerce", cause);
    }
}