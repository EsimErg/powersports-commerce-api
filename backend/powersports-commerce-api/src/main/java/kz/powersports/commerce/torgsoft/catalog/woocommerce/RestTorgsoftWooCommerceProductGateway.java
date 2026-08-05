package kz.powersports.commerce.torgsoft.catalog.woocommerce;

import kz.powersports.commerce.torgsoft.config.TorgsoftProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.Optional;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = "enabled",
        havingValue = "true"
)
public class RestTorgsoftWooCommerceProductGateway
        implements TorgsoftWooCommerceProductGateway {

    private static final String PRODUCTS_PATH =
            "/wp-json/wc/v3/products";

    private final RestClient wooCommerceAdminRestClient;
    private final TorgsoftProperties properties;

    public RestTorgsoftWooCommerceProductGateway(
            @Qualifier("wooCommerceAdminRestClient")
            RestClient wooCommerceAdminRestClient,
            TorgsoftProperties properties
    ) {
        this.wooCommerceAdminRestClient =
                Objects.requireNonNull(
                        wooCommerceAdminRestClient,
                        "wooCommerceAdminRestClient не должен быть null"
                );

        this.properties = Objects.requireNonNull(
                properties,
                "properties не должен быть null"
        );
    }
    @Override
    public Optional<Long> findProductIdBySku(String sku) {
        if (sku == null || sku.isBlank()) {
            return Optional.empty();
        }

        String normalizedSku = sku.trim();

        try {
            WooCommerceAdminProductResponse[] response =
                    wooCommerceAdminRestClient
                            .get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(PRODUCTS_PATH)
                                    .queryParam("sku", normalizedSku)
                                    .queryParam("per_page", 1)
                                    .build()
                            )
                            .retrieve()
                            .body(
                                    WooCommerceAdminProductResponse[].class
                            );

            if (response == null || response.length == 0) {
                return Optional.empty();
            }

            WooCommerceAdminProductResponse product =
                    response[0];

            if (product == null
                    || product.id() == null
                    || product.id() <= 0) {
                throw new IllegalStateException(
                        "WooCommerce вернул товар без корректного ID. SKU: "
                                + normalizedSku
                );
            }

            return Optional.of(product.id());

        } catch (RestClientException exception) {
            throw new TorgsoftProductSyncException(
                    "Не удалось найти товар WooCommerce по SKU: "
                            + normalizedSku,
                    exception
            );
        }
    }
    @Override
    public WooCommerceProductSyncResult create(
            WooCommerceProductSyncRequest request
    ) {
        Objects.requireNonNull(
                request,
                "request не должен быть null"
        );

        WooCommerceAdminProductRequest apiRequest =
                WooCommerceAdminProductRequest.forCreate(
                        request,
                        properties.productStatus()
                );

        try {
            WooCommerceAdminProductResponse response =
                    wooCommerceAdminRestClient
                            .post()
                            .uri(PRODUCTS_PATH)
                            .body(apiRequest)
                            .retrieve()
                            .body(
                                    WooCommerceAdminProductResponse.class
                            );

            if (response == null || response.id() == null) {
                throw new IllegalStateException(
                        "WooCommerce не вернул ID созданного товара"
                );
            }

            return new WooCommerceProductSyncResult(
                    response.id()
            );

        } catch (RestClientException exception) {
            throw new TorgsoftProductSyncException(
                    "Не удалось создать товар WooCommerce. GoodID: "
                            + request.goodId(),
                    exception
            );
        }
    }

    @Override
    public void update(
            Long productId,
            WooCommerceProductSyncRequest request
    ) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException(
                    "WooCommerce product ID должен быть положительным"
            );
        }

        Objects.requireNonNull(
                request,
                "request не должен быть null"
        );

        WooCommerceAdminProductRequest apiRequest =
                WooCommerceAdminProductRequest.forUpdate(
                        request
                );

        try {
            wooCommerceAdminRestClient
                    .put()
                    .uri(
                            PRODUCTS_PATH + "/{productId}",
                            productId
                    )
                    .body(apiRequest)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException exception) {
            throw new TorgsoftProductSyncException(
                    "Не удалось обновить товар WooCommerce. "
                            + "GoodID: " + request.goodId()
                            + ", productId: " + productId,
                    exception
            );
        }
    }
}