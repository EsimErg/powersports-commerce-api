package kz.powersports.commerce.order.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WooCommerceOrderCreateRequest(

        String status,

        @JsonProperty("set_paid")
        boolean setPaid,

        WooCommerceBillingAddress billing,

        WooCommerceShippingAddress shipping,

        @JsonProperty("customer_note")
        String customerNote,

        @JsonProperty("line_items")
        List<WooCommerceOrderLineItemRequest> lineItems,

        @JsonProperty("meta_data")
        List<WooCommerceOrderMetaData> metaData

) {
}