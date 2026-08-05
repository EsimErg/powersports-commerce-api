package kz.powersports.commerce.torgsoft.order.woocommerce;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WooCommerceOrderExportResponse(
        Long id,
        String number,
        String status,
        String currency,
        String total,

        @JsonProperty("date_created")
        String dateCreated,

        @JsonProperty("customer_note")
        String customerNote,

        Address billing,
        Address shipping,

        @JsonProperty("line_items")
        List<LineItem> lineItems
) {

    public record Address(
            @JsonProperty("first_name")
            String firstName,

            @JsonProperty("last_name")
            String lastName,

            @JsonProperty("address_1")
            String address1,

            @JsonProperty("address_2")
            String address2,

            String city,
            String state,
            String postcode,
            String country,
            String email,
            String phone
    ) {
    }

    public record LineItem(
            Long id,

            @JsonProperty("product_id")
            Long productId,

            @JsonProperty("variation_id")
            Long variationId,

            String name,
            String sku,
            Integer quantity,
            String subtotal,
            String total,
            String price
    ) {
    }
}