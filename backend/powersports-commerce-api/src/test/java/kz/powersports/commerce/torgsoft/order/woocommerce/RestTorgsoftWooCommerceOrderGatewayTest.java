package kz.powersports.commerce.torgsoft.order.woocommerce;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match
        .MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match
        .MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response
        .MockRestResponseCreators.withSuccess;

class RestTorgsoftWooCommerceOrderGatewayTest {

    private MockRestServiceServer mockServer;
    private RestTorgsoftWooCommerceOrderGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(
                                "http://woocommerce.test"
                        );

        mockServer =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        gateway =
                new RestTorgsoftWooCommerceOrderGateway(
                        builder.build()
                );
    }

    @Test
    void shouldLoadFullWooCommerceOrder() {
        mockServer
                .expect(
                        requestTo(
                                "http://woocommerce.test"
                                        + "/wp-json/wc/v3/orders/15"
                        )
                )
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "id": 15,
                                  "number": "15",
                                  "status": "on-hold",
                                  "currency": "KZT",
                                  "total": "340000.00",
                                  "date_created": "2026-08-05T14:00:00",
                                  "customer_note": "Позвонить",
                                  "billing": {
                                    "first_name": "Есым",
                                    "last_name": "Ергобек",
                                    "address_1": "Туркестан",
                                    "city": "Туркестан",
                                    "country": "KZ",
                                    "email": "esym@example.com",
                                    "phone": "+77001234567"
                                  },
                                  "shipping": {
                                    "first_name": "Есым",
                                    "last_name": "Ергобек",
                                    "address_1": "Туркестан",
                                    "city": "Туркестан",
                                    "country": "KZ"
                                  },
                                  "line_items": [
                                    {
                                      "id": 100,
                                      "product_id": 12,
                                      "variation_id": 0,
                                      "name": "Беговая дорожка",
                                      "sku": "POWERRUN-X1",
                                      "quantity": 1,
                                      "subtotal": "340000.00",
                                      "total": "340000.00",
                                      "price": "340000"
                                    }
                                  ]
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        WooCommerceOrderExportResponse result =
                gateway.getOrder(15L);

        assertThat(result.id())
                .isEqualTo(15L);

        assertThat(result.billing().phone())
                .isEqualTo("+77001234567");

        assertThat(result.lineItems())
                .hasSize(1);

        assertThat(result.lineItems().getFirst().sku())
                .isEqualTo("POWERRUN-X1");

        mockServer.verify();
    }
}