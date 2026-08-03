package kz.powersports.commerce.category.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import kz.powersports.commerce.product.client.dto.WooCommerceImage;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WooCommerceCategory(

        Long id,

        String name,

        String slug,

        String description,

        Long parent,

        int count,

        WooCommerceImage image,

        @JsonProperty("review_count")
        int reviewCount,

        String permalink

) {
}