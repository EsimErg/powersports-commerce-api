package kz.powersports.commerce.cart.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kz.powersports.commerce.product.client.dto.WooCommerceImage;
import kz.powersports.commerce.product.client.dto.WooCommercePrices;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WooCommerceCartItem(

        String key,

        Long id,

        int quantity,

        String name,

        String sku,

        List<WooCommerceImage> images,

        WooCommercePrices prices,

        WooCommerceCartItemTotals totals

) {
}