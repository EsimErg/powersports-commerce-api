package kz.powersports.commerce.cart.client;

import kz.powersports.commerce.cart.client.dto.WooCommerceCart;
import kz.powersports.commerce.cart.client.dto.WooCommerceCartResult;
import kz.powersports.commerce.common.exception.CartInitializationException;
import kz.powersports.commerce.common.exception.WooCommerceApiException;
import kz.powersports.commerce.common.exception.WooCommerceUnavailableException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class WooCommerceCartClient {

    private static final String CART_PATH =
            "/wp-json/wc/store/v1/cart";

    private static final String ADD_ITEM_PATH =
            "/wp-json/wc/store/v1/cart/add-item";

    private static final String UPDATE_ITEM_PATH =
            "/wp-json/wc/store/v1/cart/update-item";

    private static final String REMOVE_ITEM_PATH =
            "/wp-json/wc/store/v1/cart/remove-item";

    private static final String CART_ITEMS_PATH =
            "/wp-json/wc/store/v1/cart/items";

    private static final String CART_TOKEN_HEADER =
            "Cart-Token";

    private final RestClient wooCommerceRestClient;

    public WooCommerceCartClient(
            RestClient wooCommerceRestClient
    ) {
        this.wooCommerceRestClient =
                wooCommerceRestClient;
    }

    public WooCommerceCartResult createCart() {
        try {
            ResponseEntity<WooCommerceCart> response =
                    wooCommerceRestClient
                            .get()
                            .uri(CART_PATH)
                            .retrieve()
                            .toEntity(
                                    WooCommerceCart.class
                            );

            return toResult(response, null);

        } catch (ResourceAccessException exception) {
            throw new WooCommerceUnavailableException(
                    exception
            );

        } catch (RestClientResponseException exception) {
            throw toApiException(exception);
        }
    }

    public WooCommerceCartResult getCart(
            String cartToken
    ) {
        try {
            ResponseEntity<WooCommerceCart> response =
                    wooCommerceRestClient
                            .get()
                            .uri(CART_PATH)
                            .header(
                                    CART_TOKEN_HEADER,
                                    cartToken
                            )
                            .retrieve()
                            .toEntity(
                                    WooCommerceCart.class
                            );

            return toResult(
                    response,
                    cartToken
            );

        } catch (ResourceAccessException exception) {
            throw new WooCommerceUnavailableException(
                    exception
            );

        } catch (RestClientResponseException exception) {
            throw toApiException(exception);
        }
    }

    public WooCommerceCartResult addItem(
            String cartToken,
            Long productId,
            int quantity
    ) {
        try {
            ResponseEntity<WooCommerceCart> response =
                    wooCommerceRestClient
                            .post()
                            .uri(uriBuilder -> uriBuilder
                                    .path(ADD_ITEM_PATH)
                                    .queryParam(
                                            "id",
                                            productId
                                    )
                                    .queryParam(
                                            "quantity",
                                            quantity
                                    )
                                    .build())
                            .header(
                                    CART_TOKEN_HEADER,
                                    cartToken
                            )
                            .retrieve()
                            .toEntity(
                                    WooCommerceCart.class
                            );

            return toResult(
                    response,
                    cartToken
            );

        } catch (ResourceAccessException exception) {
            throw new WooCommerceUnavailableException(
                    exception
            );

        } catch (RestClientResponseException exception) {
            throw toApiException(exception);
        }
    }

    public WooCommerceCartResult updateItem(
            String cartToken,
            String itemKey,
            int quantity
    ) {
        try {
            ResponseEntity<WooCommerceCart> response =
                    wooCommerceRestClient
                            .post()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UPDATE_ITEM_PATH)
                                    .queryParam(
                                            "key",
                                            itemKey
                                    )
                                    .queryParam(
                                            "quantity",
                                            quantity
                                    )
                                    .build())
                            .header(
                                    CART_TOKEN_HEADER,
                                    cartToken
                            )
                            .retrieve()
                            .toEntity(
                                    WooCommerceCart.class
                            );

            return toResult(
                    response,
                    cartToken
            );

        } catch (ResourceAccessException exception) {
            throw new WooCommerceUnavailableException(
                    exception
            );

        } catch (RestClientResponseException exception) {
            throw toApiException(exception);
        }
    }

    public WooCommerceCartResult removeItem(
            String cartToken,
            String itemKey
    ) {
        try {
            ResponseEntity<WooCommerceCart> response =
                    wooCommerceRestClient
                            .post()
                            .uri(uriBuilder -> uriBuilder
                                    .path(REMOVE_ITEM_PATH)
                                    .queryParam(
                                            "key",
                                            itemKey
                                    )
                                    .build())
                            .header(
                                    CART_TOKEN_HEADER,
                                    cartToken
                            )
                            .retrieve()
                            .toEntity(
                                    WooCommerceCart.class
                            );

            return toResult(
                    response,
                    cartToken
            );

        } catch (ResourceAccessException exception) {
            throw new WooCommerceUnavailableException(
                    exception
            );

        } catch (RestClientResponseException exception) {
            throw toApiException(exception);
        }
    }

    public WooCommerceCartResult clearCart(
            String cartToken
    ) {
        try {
            /*
             * WooCommerce удаляет все позиции.
             * Затем повторно получаем полную корзину,
             * чтобы вернуть frontend единый CartResponse.
             */
            wooCommerceRestClient
                    .delete()
                    .uri(CART_ITEMS_PATH)
                    .header(
                            CART_TOKEN_HEADER,
                            cartToken
                    )
                    .retrieve()
                    .toBodilessEntity();

            return getCart(cartToken);

        } catch (ResourceAccessException exception) {
            throw new WooCommerceUnavailableException(
                    exception
            );

        } catch (RestClientResponseException exception) {
            throw toApiException(exception);
        }
    }

    private WooCommerceCartResult toResult(
            ResponseEntity<WooCommerceCart> response,
            String fallbackToken
    ) {
        WooCommerceCart cart =
                response.getBody();

        if (cart == null) {
            throw new CartInitializationException(
                    "WooCommerce вернул пустой ответ корзины"
            );
        }

        String responseToken =
                response.getHeaders()
                        .getFirst(
                                CART_TOKEN_HEADER
                        );

        String finalToken =
                responseToken == null
                        || responseToken.isBlank()
                        ? fallbackToken
                        : responseToken;

        if (finalToken == null
                || finalToken.isBlank()) {
            throw new CartInitializationException(
                    "WooCommerce не вернул Cart-Token"
            );
        }

        return new WooCommerceCartResult(
                cart,
                finalToken
        );
    }

    private WooCommerceApiException toApiException(
            RestClientResponseException exception
    ) {
        return new WooCommerceApiException(
                exception.getStatusCode().value(),
                exception.getResponseBodyAsString(),
                exception
        );
    }
}