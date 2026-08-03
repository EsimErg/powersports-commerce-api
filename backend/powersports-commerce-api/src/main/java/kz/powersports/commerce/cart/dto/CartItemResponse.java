package kz.powersports.commerce.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(

        String key,

        Long productId,

        String name,

        String sku,

        int quantity,

        BigDecimal unitPrice,

        BigDecimal lineSubtotal,

        BigDecimal lineTotal,

        String currency,

        String imageUrl

) {
}