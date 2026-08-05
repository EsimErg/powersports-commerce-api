package kz.powersports.commerce.torgsoft.order.document;

import kz.powersports.commerce.torgsoft.order.woocommerce
        .WooCommerceOrderExportResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TorgsoftOrderDocumentMapperTest {

    private final TorgsoftOrderDocumentMapper mapper =
            new TorgsoftOrderDocumentMapper();

    @Test
    void shouldMapWooCommerceOrderToTorgsoftDocument() {
        WooCommerceOrderExportResponse response =
                new WooCommerceOrderExportResponse(
                        15L,
                        "15",
                        "on-hold",
                        "KZT",
                        "340000.00",
                        "2026-08-05T14:00:00",
                        "Позвонить перед доставкой",
                        createAddress(),
                        createAddress(),
                        List.of(
                                new WooCommerceOrderExportResponse.LineItem(
                                        100L,
                                        12L,
                                        0L,
                                        "Беговая дорожка PowerRun X1",
                                        "POWERRUN-X1",
                                        1,
                                        "340000.00",
                                        "340000.00",
                                        "340000.00"
                                )
                        )
                );

        TorgsoftOrderDocument result =
                mapper.map(response);

        assertThat(result.wooCommerceOrderId())
                .isEqualTo(15L);

        assertThat(result.orderNumber())
                .isEqualTo("15");

        assertThat(result.currency())
                .isEqualTo("KZT");

        assertThat(result.total())
                .isEqualByComparingTo(
                        new BigDecimal("340000.00")
                );

        assertThat(result.customer().fullName())
                .isEqualTo("Есым Ергобек");

        assertThat(result.items())
                .hasSize(1);

        assertThat(result.items().getFirst().sku())
                .isEqualTo("POWERRUN-X1");

        assertThat(result.items().getFirst().quantity())
                .isEqualTo(1);

        assertThat(result.items().getFirst().lineTotal())
                .isEqualByComparingTo(
                        new BigDecimal("340000.00")
                );
    }

    private WooCommerceOrderExportResponse.Address createAddress() {
        return new WooCommerceOrderExportResponse.Address(
                "Есым",
                "Ергобек",
                "Адрес согласовать по телефону",
                "",
                "Туркестан",
                "",
                "",
                "KZ",
                "esym@example.com",
                "+77001234567"
        );
    }
}