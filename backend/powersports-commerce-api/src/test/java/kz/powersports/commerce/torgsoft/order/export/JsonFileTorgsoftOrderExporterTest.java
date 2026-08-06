package kz.powersports.commerce.torgsoft.order.export;

import kz.powersports.commerce.torgsoft.config
        .TorgsoftOrderExportProperties;
import kz.powersports.commerce.torgsoft.config
        .TorgsoftProperties;
import kz.powersports.commerce.torgsoft.order.document
        .TorgsoftOrderDocument;
import kz.powersports.commerce.torgsoft.order.document
        .TorgsoftOrderDocumentMapper;
import kz.powersports.commerce.torgsoft.order.woocommerce
        .TorgsoftWooCommerceOrderGateway;
import kz.powersports.commerce.torgsoft.order.woocommerce
        .WooCommerceOrderExportResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonFileTorgsoftOrderExporterTest {

    @TempDir
    Path tempDirectory;

    @Mock
    private TorgsoftWooCommerceOrderGateway orderGateway;

    @Mock
    private TorgsoftOrderDocumentMapper documentMapper;

    @Mock
    private JsonMapper jsonMapper;

    @Test
    void shouldWriteOrderToJsonFileAtomically()
            throws Exception {
        WooCommerceOrderExportResponse response =
                new WooCommerceOrderExportResponse(
                        15L,
                        "15",
                        "on-hold",
                        "KZT",
                        "340000.00",
                        "2026-08-05T14:00:00",
                        "Позвонить",
                        null,
                        null,
                        List.of()
                );

        TorgsoftOrderDocument document =
                createDocument();

        byte[] json =
                """
                {
                  "wooCommerceOrderId": 15,
                  "orderNumber": "15"
                }
                """.getBytes(
                        StandardCharsets.UTF_8
                );

        when(orderGateway.getOrder(15L))
                .thenReturn(response);

        when(documentMapper.map(response))
                .thenReturn(document);

        when(jsonMapper.writeValueAsBytes(document))
                .thenReturn(json);

        JsonFileTorgsoftOrderExporter exporter =
                createExporter(
                        Path.of(
                                "orders",
                                "outgoing"
                        )
                );

        exporter.export(15L);

        Path outputDirectory =
                tempDirectory.resolve(
                        Path.of(
                                "orders",
                                "outgoing"
                        )
                );

        Path resultFile =
                outputDirectory.resolve(
                        "order-15.json"
                );

        assertThat(resultFile)
                .exists()
                .isRegularFile();

        assertThat(Files.readAllBytes(resultFile))
                .isEqualTo(json);

        try (
                Stream<Path> files =
                        Files.list(outputDirectory)
        ) {
            assertThat(
                    files.noneMatch(
                            file -> file
                                    .getFileName()
                                    .toString()
                                    .startsWith(
                                            ".tmp-order-"
                                    )
                    )
            ).isTrue();
        }

        verify(orderGateway).getOrder(15L);
        verify(documentMapper).map(response);
        verify(jsonMapper)
                .writeValueAsBytes(document);
    }

    private JsonFileTorgsoftOrderExporter createExporter(
            Path outgoingDirectory
    ) {
        TorgsoftProperties torgsoftProperties =
                new TorgsoftProperties(
                        true,
                        tempDirectory,
                        "TSGoods.trs",
                        TorgsoftProperties
                                .OrderFormat
                                .JSON,
                        TorgsoftProperties
                                .ProductStatus
                                .DRAFT
                );

        TorgsoftOrderExportProperties exportProperties =
                new TorgsoftOrderExportProperties(
                        true,
                        outgoingDirectory,
                        "order-"
                );

        return new JsonFileTorgsoftOrderExporter(
                orderGateway,
                documentMapper,
                torgsoftProperties,
                exportProperties,
                jsonMapper
        );
    }

    private TorgsoftOrderDocument createDocument() {
        TorgsoftOrderDocument.Customer customer =
                new TorgsoftOrderDocument.Customer(
                        "Есым",
                        "Ергобек",
                        "+77001234567",
                        "esym@example.com"
                );

        TorgsoftOrderDocument.Address address =
                new TorgsoftOrderDocument.Address(
                        "Туркестан",
                        "Адрес согласовать по телефону",
                        "",
                        "",
                        "",
                        "KZ"
                );

        TorgsoftOrderDocument.Item item =
                new TorgsoftOrderDocument.Item(
                        100L,
                        12L,
                        0L,
                        "POWERRUN-X1",
                        "Беговая дорожка PowerRun X1",
                        1,
                        new BigDecimal("340000.00"),
                        new BigDecimal("340000.00")
                );

        return new TorgsoftOrderDocument(
                15L,
                "15",
                LocalDateTime.parse(
                        "2026-08-05T14:00:00"
                ),
                "on-hold",
                "KZT",
                new BigDecimal("340000.00"),
                customer,
                address,
                address,
                "Позвонить",
                List.of(item)
        );
    }
}