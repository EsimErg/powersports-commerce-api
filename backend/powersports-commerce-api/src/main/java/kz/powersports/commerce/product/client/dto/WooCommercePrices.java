package kz.powersports.commerce.product.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WooCommercePrices(

        String price,

        @JsonProperty("regular_price")
        String regularPrice,

        @JsonProperty("sale_price")
        String salePrice,

        @JsonProperty("currency_code")
        String currencyCode,

        @JsonProperty("currency_symbol")
        String currencySymbol,

        @JsonProperty("currency_minor_unit")
        int currencyMinorUnit

) {
}