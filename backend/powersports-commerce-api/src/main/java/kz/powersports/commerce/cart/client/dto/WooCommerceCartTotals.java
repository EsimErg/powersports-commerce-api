package kz.powersports.commerce.cart.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WooCommerceCartTotals(

        @JsonProperty("total_items")
        String totalItems,

        @JsonProperty("total_discount")
        String totalDiscount,

        @JsonProperty("total_shipping")
        String totalShipping,

        @JsonProperty("total_price")
        String totalPrice,

        @JsonProperty("total_tax")
        String totalTax,

        @JsonProperty("currency_code")
        String currencyCode,

        @JsonProperty("currency_symbol")
        String currencySymbol,

        @JsonProperty("currency_minor_unit")
        int currencyMinorUnit

) {
}