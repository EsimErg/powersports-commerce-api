package kz.powersports.commerce.order.client;

import kz.powersports.commerce.common.exception.OrderCreationException;
import kz.powersports.commerce.common.exception.OrderNotFoundException;
import kz.powersports.commerce.common.exception.WooCommerceApiException;
import kz.powersports.commerce.common.exception.WooCommerceUnavailableException;
import kz.powersports.commerce.order.client.dto.WooCommerceOrderCreateRequest;
import kz.powersports.commerce.order.client.dto.WooCommerceOrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class WooCommerceOrderClient {

    private static final Logger log =
            LoggerFactory.getLogger(WooCommerceOrderClient.class);

    private static final String ORDERS_PATH =
            "/wp-json/wc/v3/orders";

    private final RestClient adminRestClient;

    public WooCommerceOrderClient(
            @Qualifier("wooCommerceAdminRestClient")
            RestClient adminRestClient
    ) {
        this.adminRestClient = adminRestClient;
    }

    public WooCommerceOrderResponse createOrder(
            WooCommerceOrderCreateRequest request
    ) {
        try {
            log.info(
                    "Отправляем запрос на создание заказа в WooCommerce"
            );

            WooCommerceOrderResponse response =
                    adminRestClient
                            .post()
                            .uri(ORDERS_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(WooCommerceOrderResponse.class);

            if (response == null) {
                throw new OrderCreationException(
                        "WooCommerce вернул пустой ответ при создании заказа"
                );
            }

            log.info(
                    "Заказ успешно создан в WooCommerce. Order ID: {}",
                    response.id()
            );

            return response;

        } catch (ResourceAccessException exception) {
            log.error(
                    "Не удалось подключиться к WooCommerce. Причина: {}",
                    exception.getMessage(),
                    exception
            );

            throw new WooCommerceUnavailableException(
                    exception
            );

        } catch (RestClientResponseException exception) {
            throw handleApiException(
                    "создание заказа",
                    exception
            );
        }
    }

    public WooCommerceOrderResponse getOrder(
            Long orderId
    ) {
        try {
            WooCommerceOrderResponse response =
                    adminRestClient
                            .get()
                            .uri(
                                    ORDERS_PATH
                                            + "/{orderId}",
                                    orderId
                            )
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .body(WooCommerceOrderResponse.class);

            if (response == null) {
                throw new OrderNotFoundException();
            }

            return response;

        } catch (ResourceAccessException exception) {
            log.error(
                    """
                    Не удалось получить заказ из WooCommerce.
                    Order ID: {}
                    Причина: {}
                    """,
                    orderId,
                    exception.getMessage(),
                    exception
            );

            throw new WooCommerceUnavailableException(
                    exception
            );

        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new OrderNotFoundException();
            }

            throw handleApiException(
                    "получение заказа " + orderId,
                    exception
            );
        }
    }

    private WooCommerceApiException handleApiException(
            String operation,
            RestClientResponseException exception
    ) {
        String responseBody =
                exception.getResponseBodyAsString();

        log.error(
                """
                WooCommerce отклонил запрос.
                Операция: {}
                HTTP status: {}
                Response body: {}
                """,
                operation,
                exception.getStatusCode().value(),
                responseBody,
                exception
        );

        return new WooCommerceApiException(
                exception.getStatusCode().value(),
                responseBody,
                exception
        );
    }
}