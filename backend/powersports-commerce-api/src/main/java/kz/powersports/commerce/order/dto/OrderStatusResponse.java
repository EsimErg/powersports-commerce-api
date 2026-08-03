package kz.powersports.commerce.order.dto;

import java.math.BigDecimal;

public record OrderStatusResponse(

        Long id,

        String number,

        String status,

        BigDecimal total,

        String currency

) {
}