package kz.powersports.commerce.torgsoft.order.woocommerce;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = "enabled",
        havingValue = "true"
)
public class RestTorgsoftWooCommerceOrderGateway
        implements TorgsoftWooCommerceOrderGateway {

    private static final String ORDER_PATH =
            "/wp-json/wc/v3/orders/{orderId}";

    private final RestClient wooCommerceAdminRestClient;

    public RestTorgsoftWooCommerceOrderGateway(
            @Qualifier("wooCommerceAdminRestClient")
            RestClient wooCommerceAdminRestClient
    ) {
        this.wooCommerceAdminRestClient =
                Objects.requireNonNull(
                        wooCommerceAdminRestClient,
                        "wooCommerceAdminRestClient не должен быть null"
                );
    }

    @Override
    public WooCommerceOrderExportResponse getOrder(
            Long wooCommerceOrderId
    ) {
        if (wooCommerceOrderId == null
                || wooCommerceOrderId <= 0) {
            throw new IllegalArgumentException(
                    "WooCommerce order ID должен быть положительным"
            );
        }

        try {
            WooCommerceOrderExportResponse response =
                    wooCommerceAdminRestClient
                            .get()
                            .uri(
                                    ORDER_PATH,
                                    wooCommerceOrderId
                            )
                            .retrieve()
                            .body(
                                    WooCommerceOrderExportResponse.class
                            );

            if (response == null || response.id() == null) {
                throw new IllegalStateException(
                        "WooCommerce не вернул данные заказа. "
                                + "Order ID: "
                                + wooCommerceOrderId
                );
            }

            return response;

        } catch (RestClientException exception) {
            throw new TorgsoftOrderLoadException(
                    "Не удалось получить заказ WooCommerce. "
                            + "Order ID: "
                            + wooCommerceOrderId,
                    exception
            );
        }
    }
}