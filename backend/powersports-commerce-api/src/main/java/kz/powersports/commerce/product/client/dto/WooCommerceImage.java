package kz.powersports.commerce.product.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WooCommerceImage(
        Long id,
        String src,
        String thumbnail,
        String alt
) {
}