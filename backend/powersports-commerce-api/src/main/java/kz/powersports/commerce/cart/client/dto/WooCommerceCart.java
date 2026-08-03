package kz.powersports.commerce.cart.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WooCommerceCart(

        List<WooCommerceCartItem> items,

        WooCommerceCartTotals totals,

        @JsonProperty("items_count")
        int itemsCount,

        @JsonProperty("needs_payment")
        boolean needsPayment,

        @JsonProperty("needs_shipping")
        boolean needsShipping

) {
}
