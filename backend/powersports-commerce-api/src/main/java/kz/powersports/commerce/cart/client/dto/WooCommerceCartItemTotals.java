package kz.powersports.commerce.cart.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WooCommerceCartItemTotals(

        @JsonProperty("line_subtotal")
        String lineSubtotal,

        @JsonProperty("line_total")
        String lineTotal,

        @JsonProperty("currency_code")
        String currencyCode,

        @JsonProperty("currency_minor_unit")
        int currencyMinorUnit

) {
}
