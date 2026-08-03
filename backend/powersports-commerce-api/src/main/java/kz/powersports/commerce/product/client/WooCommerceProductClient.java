package kz.powersports.commerce.product.client;

import kz.powersports.commerce.common.exception.WooCommerceApiException;
import kz.powersports.commerce.common.exception.WooCommerceUnavailableException;
import kz.powersports.commerce.product.client.dto.WooCommerceProduct;
import kz.powersports.commerce.product.client.dto.WooCommerceProductPage;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class WooCommerceProductClient {

    private static final String PRODUCTS_PATH =
            "/wp-json/wc/store/v1/products";

    private final RestClient wooCommerceRestClient;

    public WooCommerceProductClient(
            RestClient wooCommerceRestClient
    ) {
        this.wooCommerceRestClient = wooCommerceRestClient;
    }

    public WooCommerceProductPage findAll(
            int page,
            int size,
            String search,
            String category
    ) {
        try {
            int wooCommercePage = page + 1;

            ResponseEntity<WooCommerceProduct[]> response =
                    wooCommerceRestClient
                            .get()
                            .uri(uriBuilder -> {
                                var builder = uriBuilder
                                        .path(PRODUCTS_PATH)
                                        .queryParam(
                                                "page",
                                                wooCommercePage
                                        )
                                        .queryParam(
                                                "per_page",
                                                size
                                        );

                                if (search != null
                                        && !search.isBlank()) {
                                    builder.queryParam(
                                            "search",
                                            search.trim()
                                    );
                                }
                                if (category != null
                                        && !category.isBlank()) {
                                    builder.queryParam(
                                            "category",
                                            category.trim()
                                    );
                                }

                                return builder.build();
                            })
                            .retrieve()
                            .toEntity(
                                    WooCommerceProduct[].class
                            );

            WooCommerceProduct[] body =
                    response.getBody();

            List<WooCommerceProduct> products =
                    body == null
                            ? List.of()
                            : Arrays.asList(body);

            long totalElements = parseLongHeader(
                    response,
                    "X-WP-Total",
                    products.size()
            );

            int totalPages = parseIntHeader(
                    response,
                    "X-WP-TotalPages",
                    products.isEmpty() ? 0 : 1
            );

            return new WooCommerceProductPage(
                    products,
                    totalElements,
                    totalPages
            );

        } catch (ResourceAccessException exception) {
            throw new WooCommerceUnavailableException(
                    exception
            );

        } catch (RestClientResponseException exception) {
            throw new WooCommerceApiException(
                    exception.getStatusCode().value(),
                    exception.getResponseBodyAsString(),
                    exception
            );
        }
    }

    public Optional<WooCommerceProduct> findBySlug(
            String slug
    ) {
        try {
            WooCommerceProduct[] products =
                    wooCommerceRestClient
                            .get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(PRODUCTS_PATH)
                                    .queryParam("slug", slug)
                                    .queryParam("per_page", 1)
                                    .build())
                            .retrieve()
                            .body(
                                    WooCommerceProduct[].class
                            );

            if (products == null
                    || products.length == 0) {
                return Optional.empty();
            }

            return Optional.of(products[0]);

        } catch (ResourceAccessException exception) {
            throw new WooCommerceUnavailableException(
                    exception
            );

        } catch (RestClientResponseException exception) {
            throw new WooCommerceApiException(
                    exception.getStatusCode().value(),
                    exception.getResponseBodyAsString(),
                    exception
            );
        }
    }

    private long parseLongHeader(
            ResponseEntity<?> response,
            String headerName,
            long defaultValue
    ) {
        String value = response
                .getHeaders()
                .getFirst(headerName);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private int parseIntHeader(
            ResponseEntity<?> response,
            String headerName,
            int defaultValue
    ) {
        String value = response
                .getHeaders()
                .getFirst(headerName);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}