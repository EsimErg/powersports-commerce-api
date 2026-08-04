package kz.powersports.commerce.torgsoft.catalog.woocommerce;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import kz.powersports.commerce.torgsoft.config.TorgsoftProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WooCommerceAdminProductRequest(
        String name,
        String type,
        String status,
        String sku,

        @JsonProperty("regular_price")
        String regularPrice,

        @JsonProperty("manage_stock")
        boolean manageStock,

        @JsonProperty("stock_quantity")
        int stockQuantity,

        @JsonProperty("stock_status")
        String stockStatus,

        String backorders,

        @JsonProperty("meta_data")
        List<MetaData> metaData
) {

    public static WooCommerceAdminProductRequest forCreate(
            WooCommerceProductSyncRequest request,
            TorgsoftProperties.ProductStatus productStatus
    ) {
        Objects.requireNonNull(
                productStatus,
                "productStatus не должен быть null"
        );

        return build(
                request,
                productStatus.apiValue()
        );
    }

    public static WooCommerceAdminProductRequest forUpdate(
            WooCommerceProductSyncRequest request
    ) {
        return build(request, null);
    }

    private static WooCommerceAdminProductRequest build(
            WooCommerceProductSyncRequest request,
            String status
    ) {
        Objects.requireNonNull(
                request,
                "request не должен быть null"
        );

        int stockQuantity = convertStockQuantity(
                request.stockQuantity()
        );

        return new WooCommerceAdminProductRequest(
                request.name(),
                "simple",
                status,
                request.sku(),
                request.price()
                        .stripTrailingZeros()
                        .toPlainString(),
                true,
                stockQuantity,
                stockQuantity > 0
                        ? "instock"
                        : "outofstock",
                "no",
                List.of(
                        new MetaData(
                                "_torgsoft_good_id",
                                request.goodId()
                        )
                )
        );
    }

    private static int convertStockQuantity(
            BigDecimal quantity
    ) {
        Objects.requireNonNull(
                quantity,
                "stockQuantity не должен быть null"
        );

        BigDecimal normalizedQuantity =
                quantity.max(BigDecimal.ZERO);

        try {
            return normalizedQuantity.intValueExact();

        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Остаток WooCommerce должен быть целым числом: "
                            + quantity,
                    exception
            );
        }
    }

    public record MetaData(
            String key,
            String value
    ) {
    }
}