package kz.powersports.commerce.torgsoft.order.woocommerce;

public final class TorgsoftOrderStatusUpdateException
        extends RuntimeException {

    public TorgsoftOrderStatusUpdateException(
            Long orderId,
            Throwable cause
    ) {
        super(
                "Не удалось обновить metadata Torgsoft "
                        + "для заказа WooCommerce: "
                        + orderId,
                cause
        );
    }
}