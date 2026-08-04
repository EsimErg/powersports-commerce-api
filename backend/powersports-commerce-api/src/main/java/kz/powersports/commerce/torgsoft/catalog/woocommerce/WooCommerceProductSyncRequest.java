package kz.powersports.commerce.torgsoft.catalog.woocommerce;

import kz.powersports.commerce.torgsoft.catalog.model.TorgsoftProduct;

import java.math.BigDecimal;
import java.util.Objects;

public record WooCommerceProductSyncRequest(
        String goodId,
        String sku,
        String name,
        BigDecimal price,
        BigDecimal stockQuantity
) {

    public WooCommerceProductSyncRequest {
        Objects.requireNonNull(goodId, "goodId не должен быть null");
        Objects.requireNonNull(name, "name не должен быть null");
        Objects.requireNonNull(price, "price не должен быть null");
        Objects.requireNonNull(
                stockQuantity,
                "stockQuantity не должен быть null"
        );
    }

    public static WooCommerceProductSyncRequest from(
            TorgsoftProduct product
    ) {
        Objects.requireNonNull(
                product,
                "product не должен быть null"
        );

        return new WooCommerceProductSyncRequest(
                product.goodId(),
                product.sku(),
                product.name(),
                product.price(),
                product.stockQuantity()
        );
    }
}