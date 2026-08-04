package kz.powersports.commerce.torgsoft.catalog.model;

import java.math.BigDecimal;

public record TorgsoftProduct(
        String goodId,
        String sku,
        String name,
        BigDecimal price,
        BigDecimal stockQuantity
) {

    public TorgsoftProduct {
        if (goodId == null || goodId.isBlank()) {
            throw new IllegalArgumentException(
                    "Torgsoft GoodID не должен быть пустым"
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Название товара не должно быть пустым"
            );
        }

        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException(
                    "Цена товара не может быть пустой или отрицательной"
            );
        }

        goodId = goodId.trim();
        sku = normalizeNullable(sku);
        name = name.trim();

        if (stockQuantity == null) {
            stockQuantity = BigDecimal.ZERO;
        }
    }

    public boolean inStock() {
        return stockQuantity.signum() > 0;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}