package kz.powersports.commerce.torgsoft.catalog.woocommerce;

import kz.powersports.commerce.torgsoft.config.TorgsoftProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestTorgsoftWooCommerceProductGatewayTest {

    private MockRestServiceServer mockServer;

    private RestTorgsoftWooCommerceProductGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder =
                RestClient.builder()
                        .baseUrl("http://woocommerce.test");

        mockServer =
                MockRestServiceServer
                        .bindTo(restClientBuilder)
                        .build();

        RestClient restClient =
                restClientBuilder.build();

        TorgsoftProperties properties =
                new TorgsoftProperties(
                        true,
                        Path.of("./data/torgsoft"),
                        "TSGoods.trs",
                        TorgsoftProperties.OrderFormat.JSON,
                        TorgsoftProperties.ProductStatus.DRAFT
                );

        gateway =
                new RestTorgsoftWooCommerceProductGateway(
                        restClient,
                        properties
                );
    }

    @Test
    void shouldCreateDraftProductAndReturnProductId() {
        mockServer
                .expect(
                        requestTo(
                                "http://woocommerce.test/wp-json/wc/v3/products"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Беговая дорожка PowerRun X1")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("draft")
                )
                .andExpect(
                        jsonPath("$.regular_price")
                                .value("340000")
                )
                .andExpect(
                        jsonPath("$.manage_stock")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.stock_quantity")
                                .value(5)
                )
                .andExpect(
                        jsonPath("$.stock_status")
                                .value("instock")
                )
                .andExpect(
                        jsonPath("$.meta_data[0].key")
                                .value("_torgsoft_good_id")
                )
                .andExpect(
                        jsonPath("$.meta_data[0].value")
                                .value("GOOD-100")
                )
                .andRespond(
                        withStatus(HttpStatus.CREATED)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .body("""
                                        {
                                          "id": 77
                                        }
                                        """)
                );

        WooCommerceProductSyncResult result =
                gateway.create(createRequest());

        assertEquals(77L, result.productId());

        mockServer.verify();
    }

    @Test
    void shouldUpdateProductWithoutChangingPublicationStatus() {
        mockServer
                .expect(
                        requestTo(
                                "http://woocommerce.test/wp-json/wc/v3/products/77"
                        )
                )
                .andExpect(method(HttpMethod.PUT))
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.status").doesNotExist()
                )
                .andExpect(
                        jsonPath("$.regular_price")
                                .value("340000")
                )
                .andExpect(
                        jsonPath("$.stock_quantity")
                                .value(5)
                )
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "id": 77
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        gateway.update(
                77L,
                createRequest()
        );

        mockServer.verify();
    }

    @Test
    void shouldThrowExceptionWhenWooCommerceReturnsServerError() {
        mockServer
                .expect(
                        requestTo(
                                "http://woocommerce.test/wp-json/wc/v3/products"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .body("""
                                        {
                                          "code": "woocommerce_error",
                                          "message": "Internal error"
                                        }
                                        """)
                );

        TorgsoftProductSyncException exception =
                assertThrows(
                        TorgsoftProductSyncException.class,
                        () -> gateway.create(createRequest())
                );

        assertEquals(
                "Не удалось создать товар WooCommerce. GoodID: GOOD-100",
                exception.getMessage()
        );

        mockServer.verify();
    }

    private WooCommerceProductSyncRequest createRequest() {
        return new WooCommerceProductSyncRequest(
                "GOOD-100",
                "PS-100",
                "Беговая дорожка PowerRun X1",
                new BigDecimal("340000.00"),
                new BigDecimal("5")
        );
    }
    @Test
    void shouldFindExistingProductBySku() {
        mockServer
                .expect(
                        requestTo(
                                "http://woocommerce.test/wp-json/wc/v3/products"
                                        + "?sku=PS-100&per_page=1"
                        )
                )
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
                                [
                                  {
                                    "id": 77
                                  }
                                ]
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        Optional<Long> result =
                gateway.findProductIdBySku("PS-100");

        assertEquals(
                Optional.of(77L),
                result
        );

        mockServer.verify();
    }
}