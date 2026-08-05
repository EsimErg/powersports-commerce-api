package kz.powersports.commerce.torgsoft.catalog.woocommerce;

import java.util.Optional;

public interface TorgsoftWooCommerceProductGateway {

    Optional<Long> findProductIdBySku(String sku);

    WooCommerceProductSyncResult create(
            WooCommerceProductSyncRequest request
    );

    void update(
            Long productId,
            WooCommerceProductSyncRequest request
    );
}