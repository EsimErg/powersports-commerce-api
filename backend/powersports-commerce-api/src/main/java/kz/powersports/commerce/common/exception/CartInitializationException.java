package kz.powersports.commerce.common.exception;

public class CartInitializationException
        extends RuntimeException {

    public CartInitializationException(
            String message
    ) {
        super(message);
    }
}