package kz.powersports.commerce.product.client.dto;

import java.util.List;

public record WooCommerceProductPage(
        List<WooCommerceProduct> content,
        long totalElements,
        int totalPages
) {
}