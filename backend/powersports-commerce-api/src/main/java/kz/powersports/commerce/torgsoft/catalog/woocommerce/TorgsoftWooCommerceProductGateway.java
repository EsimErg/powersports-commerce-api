package kz.powersports.commerce.torgsoft.catalog.woocommerce;

public interface TorgsoftWooCommerceProductGateway {

    WooCommerceProductSyncResult create(
            WooCommerceProductSyncRequest request
    );

    void update(
            Long productId,
            WooCommerceProductSyncRequest request
    );
}