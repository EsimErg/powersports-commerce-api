package kz.powersports.commerce.product.dto;

import java.math.BigDecimal;

public record ProductResponse(

        Long id,

        String name,

        String slug,

        String sku,

        String shortDescription,

        BigDecimal price,

        BigDecimal regularPrice,

        BigDecimal salePrice,

        String currency,

        boolean inStock,

        boolean onSale,

        String imageUrl

) {
}