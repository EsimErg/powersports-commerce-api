package kz.powersports.commerce.torgsoft.order.woocommerce;

public class TorgsoftOrderLoadException
        extends RuntimeException {

    public TorgsoftOrderLoadException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}