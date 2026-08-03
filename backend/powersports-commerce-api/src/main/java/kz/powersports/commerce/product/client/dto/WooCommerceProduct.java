package kz.powersports.commerce.product.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WooCommerceProduct(

        Long id,

        String name,

        String slug,

        String sku,

        @JsonProperty("short_description")
        String shortDescription,

        @JsonProperty("on_sale")
        boolean onSale,

        @JsonProperty("is_in_stock")
        boolean inStock,

        WooCommercePrices prices,

        List<WooCommerceImage> images

) {
}