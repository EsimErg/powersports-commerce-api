package kz.powersports.commerce.order.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WooCommerceOrderLineItemRequest(

        @JsonProperty("product_id")
        Long productId,

        int quantity

) {
}