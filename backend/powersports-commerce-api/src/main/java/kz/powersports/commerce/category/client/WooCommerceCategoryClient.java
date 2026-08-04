package kz.powersports.commerce.category.client;

import kz.powersports.commerce.category.client.dto.WooCommerceCategory;
import kz.powersports.commerce.common.exception.WooCommerceApiException;
import kz.powersports.commerce.common.exception.WooCommerceUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.List;

@Component
public class WooCommerceCategoryClient {

    private static final String CATEGORIES_PATH =
            "/wp-json/wc/store/v1/products/categories";

    private final RestClient wooCommerceRestClient;

    public WooCommerceCategoryClient(
            RestClient wooCommerceRestClient
    ) {
        this.wooCommerceRestClient = wooCommerceRestClient;
    }

    public List<WooCommerceCategory> findAll() {
        try {
            WooCommerceCategory[] categories =
                    wooCommerceRestClient
                            .get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(CATEGORIES_PATH)
                                    .queryParam(
                                            "hide_empty",
                                            false
                                    )
                                    .queryParam(
                                            "per_page",
                                            100
                                    )
                                    .queryParam(
                                            "orderby",
                                            "name"
                                    )
                                    .queryParam(
                                            "order",
                                            "asc"
                                    )
                                    .build())
                            .retrieve()
                            .body(
                                    WooCommerceCategory[].class
                            );

            if (categories == null) {
                return List.of();
            }

            return Arrays.asList(categories);

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
}