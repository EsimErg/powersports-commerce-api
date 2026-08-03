package kz.powersports.commerce.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(

        List<CartItemResponse> items,

        int itemsCount,

        BigDecimal itemsSubtotal,

        BigDecimal discountTotal,

        BigDecimal shippingTotal,

        BigDecimal totalPrice,

        BigDecimal taxTotal,

        String currency,

        boolean needsPayment,

        boolean needsShipping

) {
}