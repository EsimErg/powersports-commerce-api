package kz.powersports.commerce.torgsoft.catalog.woocommerce;

public class TorgsoftProductSyncException extends RuntimeException {

    public TorgsoftProductSyncException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}