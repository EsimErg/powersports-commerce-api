package kz.powersports.commerce.torgsoft.catalog.woocommerce;

public record WooCommerceProductSyncResult(
        Long productId
) {

    public WooCommerceProductSyncResult {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException(
                    "WooCommerce product ID должен быть положительным"
            );
        }
    }
}