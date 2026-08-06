package kz.powersports.commerce.torgsoft.order.woocommerce;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.springframework.test.web.client
        .match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client
        .match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client
        .match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client
        .response.MockRestResponseCreators.withSuccess;

class RestTorgsoftOrderExportStatusGatewayTest {

    private MockRestServiceServer server;

    private RestTorgsoftOrderExportStatusGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient
                .builder()
                .baseUrl("http://localhost:8085");

        server = MockRestServiceServer
                .bindTo(builder)
                .build();

        gateway = new RestTorgsoftOrderExportStatusGateway(
                builder.build()
        );
    }

    @Test
    void markExportedShouldUpdateWooCommerceMetadata() {
        server.expect(
                        requestTo(
                                "http://localhost:8085"
                                        + "/wp-json/wc/v3/orders/26"
                        )
                )
                .andExpect(method(HttpMethod.PUT))
                .andExpect(
                        content().json(
                                """
                                {
                                  "meta_data": [
                                    {
                                      "key": "_torgsoft_export_status",
                                      "value": "exported"
                                    },
                                    {
                                      "key": "_torgsoft_export_attempts",
                                      "value": "1"
                                    },
                                    {
                                      "key": "_torgsoft_exported_at",
                                      "value": "2026-08-06T09:16:04Z"
                                    },
                                    {
                                      "key": "_torgsoft_export_next_attempt_at",
                                      "value": ""
                                    },
                                    {
                                      "key": "_torgsoft_export_last_error",
                                      "value": ""
                                    }
                                  ]
                                }
                                """
                        )
                )
                .andRespond(
                        withSuccess(
                                "{}",
                                MediaType.APPLICATION_JSON
                        )
                );

        gateway.markExported(
                26L,
                1,
                Instant.parse("2026-08-06T09:16:04Z")
        );

        server.verify();
    }
}