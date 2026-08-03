package kz.powersports.commerce.order.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WooCommerceOrderResponse(

        Long id,

        String number,

        @JsonProperty("order_key")
        String orderKey,

        String status,

        String total,

        String currency,

        WooCommerceBillingAddress billing

) {
}