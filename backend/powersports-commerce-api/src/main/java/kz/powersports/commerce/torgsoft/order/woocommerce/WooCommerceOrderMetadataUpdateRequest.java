package kz.powersports.commerce.torgsoft.order.woocommerce;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public record WooCommerceOrderMetadataUpdateRequest(

        @JsonProperty("meta_data")
        List<MetaData> metaData

) {

    public WooCommerceOrderMetadataUpdateRequest {
        Objects.requireNonNull(
                metaData,
                "metaData не должен быть null"
        );

        metaData = List.copyOf(metaData);
    }

    public record MetaData(
            String key,
            String value
    ) {

        public MetaData {
            Objects.requireNonNull(
                    key,
                    "key не должен быть null"
            );

            Objects.requireNonNull(
                    value,
                    "value не должен быть null"
            );
        }
    }
}