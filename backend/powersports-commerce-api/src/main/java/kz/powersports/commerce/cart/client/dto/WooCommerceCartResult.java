package kz.powersports.commerce.cart.client.dto;

public record WooCommerceCartResult(
        WooCommerceCart cart,
        String cartToken
) {
}