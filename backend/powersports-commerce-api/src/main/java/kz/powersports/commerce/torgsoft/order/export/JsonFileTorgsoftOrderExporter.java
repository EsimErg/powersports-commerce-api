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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

@Component
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = {
                "enabled",
                "order-export.enabled"
        },
        havingValue = "true"
)
public class JsonFileTorgsoftOrderExporter
        implements TorgsoftOrderExporter {

    private static final Logger log =
            LoggerFactory.getLogger(
                    JsonFileTorgsoftOrderExporter.class
            );

    private final TorgsoftWooCommerceOrderGateway orderGateway;
    private final TorgsoftOrderDocumentMapper documentMapper;
    private final TorgsoftProperties torgsoftProperties;
    private final TorgsoftOrderExportProperties exportProperties;
    private final JsonMapper jsonMapper;

    public JsonFileTorgsoftOrderExporter(
            TorgsoftWooCommerceOrderGateway orderGateway,
            TorgsoftOrderDocumentMapper documentMapper,
            TorgsoftProperties torgsoftProperties,
            TorgsoftOrderExportProperties exportProperties,
            JsonMapper jsonMapper
    ) {
        this.orderGateway = Objects.requireNonNull(
                orderGateway,
                "orderGateway не должен быть null"
        );

        this.documentMapper = Objects.requireNonNull(
                documentMapper,
                "documentMapper не должен быть null"
        );

        this.torgsoftProperties = Objects.requireNonNull(
                torgsoftProperties,
                "torgsoftProperties не должен быть null"
        );

        this.exportProperties = Objects.requireNonNull(
                exportProperties,
                "exportProperties не должен быть null"
        );

        this.jsonMapper = Objects.requireNonNull(
                jsonMapper,
                "jsonMapper не должен быть null"
        );
    }

    @Override
    public void export(Long wooCommerceOrderId) {
        validateOrderId(wooCommerceOrderId);

        Path outputDirectory =
                resolveOutputDirectory();

        Path targetFile =
                buildTargetFile(
                        outputDirectory,
                        wooCommerceOrderId
                );

        WooCommerceOrderExportResponse wooCommerceOrder =
                orderGateway.getOrder(
                        wooCommerceOrderId
                );

        TorgsoftOrderDocument document =
                documentMapper.map(
                        wooCommerceOrder
                );

        byte[] json =
                serializeDocument(document);

        writeAtomically(
                outputDirectory,
                targetFile,
                json,
                wooCommerceOrderId
        );

        log.info(
                "Заказ сохранён в файл обмена Torgsoft. "
                        + "Order ID: {}, файл: {}",
                wooCommerceOrderId,
                targetFile
        );
    }

    private byte[] serializeDocument(
            TorgsoftOrderDocument document
    ) {
        try {
            return jsonMapper.writeValueAsBytes(
                    document
            );

        } catch (JacksonException exception) {
            throw new TorgsoftOrderExportFileException(
                    "Не удалось преобразовать заказ "
                            + "в JSON для Torgsoft",
                    exception
            );
        }
    }

    private void writeAtomically(
            Path outputDirectory,
            Path targetFile,
            byte[] json,
            Long orderId
    ) {
        Path temporaryFile = null;

        try {
            Files.createDirectories(
                    outputDirectory
            );

            /*
             * Временный файл создаётся в той же папке,
             * чтобы атомарное перемещение было возможно.
             */
            temporaryFile =
                    Files.createTempFile(
                            outputDirectory,
                            ".tmp-order-"
                                    + orderId
                                    + "-",
                            ".json"
                    );

            Files.write(
                    temporaryFile,
                    json,
                    WRITE,
                    TRUNCATE_EXISTING
            );

            moveToTarget(
                    temporaryFile,
                    targetFile
            );

            temporaryFile = null;

        } catch (IOException exception) {
            throw new TorgsoftOrderExportFileException(
                    "Не удалось сохранить файл заказа Torgsoft. "
                            + "Order ID: "
                            + orderId,
                    exception
            );

        } finally {
            deleteTemporaryFileSafely(
                    temporaryFile
            );
        }
    }

    private void moveToTarget(
            Path temporaryFile,
            Path targetFile
    ) throws IOException {
        try {
            Files.move(
                    temporaryFile,
                    targetFile,
                    ATOMIC_MOVE,
                    REPLACE_EXISTING
            );

        } catch (
                AtomicMoveNotSupportedException exception
        ) {
            /*
             * Некоторые файловые системы Windows
             * или сетевые папки не поддерживают
             * ATOMIC_MOVE.
             */
            Files.move(
                    temporaryFile,
                    targetFile,
                    REPLACE_EXISTING
            );
        }
    }

    private Path resolveOutputDirectory() {
        Path exchangeDirectory =
                torgsoftProperties
                        .exchangeDirectory()
                        .toAbsolutePath()
                        .normalize();

        Path configuredDirectory =
                exportProperties
                        .outgoingDirectory();

        Path outputDirectory =
                configuredDirectory.isAbsolute()
                        ? configuredDirectory
                        .toAbsolutePath()
                        .normalize()
                        : exchangeDirectory
                        .resolve(
                                configuredDirectory
                        )
                        .normalize();

        if (!outputDirectory.startsWith(
                exchangeDirectory
        )) {
            throw new TorgsoftOrderExportFileException(
                    "Папка экспорта заказов находится "
                            + "за пределами директории "
                            + "обмена Torgsoft: "
                            + outputDirectory
            );
        }

        return outputDirectory;
    }

    private Path buildTargetFile(
            Path outputDirectory,
            Long orderId
    ) {
        String fileName =
                exportProperties.filePrefix()
                        + orderId
                        + ".json";

        Path targetFile =
                outputDirectory
                        .resolve(fileName)
                        .normalize();

        if (!targetFile.startsWith(
                outputDirectory
        )) {
            throw new TorgsoftOrderExportFileException(
                    "Недопустимый путь файла заказа: "
                            + targetFile
            );
        }

        return targetFile;
    }

    private void deleteTemporaryFileSafely(
            Path temporaryFile
    ) {
        if (temporaryFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(
                    temporaryFile
            );

        } catch (IOException exception) {
            log.warn(
                    "Не удалось удалить временный "
                            + "файл Torgsoft: {}",
                    temporaryFile,
                    exception
            );
        }
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException(
                    "WooCommerce order ID должен быть положительным"
            );
        }
    }
}