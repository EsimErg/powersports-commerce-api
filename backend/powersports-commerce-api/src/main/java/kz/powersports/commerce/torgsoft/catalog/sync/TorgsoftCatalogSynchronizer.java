package kz.powersports.commerce.torgsoft.catalog.sync;

import kz.powersports.commerce.torgsoft.catalog.model.TorgsoftProduct;
import kz.powersports.commerce.torgsoft.catalog.port.TorgsoftCatalogReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class TorgsoftCatalogSynchronizer {

    private static final Logger log =
            LoggerFactory.getLogger(TorgsoftCatalogSynchronizer.class);

    private final TorgsoftCatalogReader catalogReader;
    private final TorgsoftProductUpsertPort productUpsertPort;

    public TorgsoftCatalogSynchronizer(
            TorgsoftCatalogReader catalogReader,
            TorgsoftProductUpsertPort productUpsertPort
    ) {
        this.catalogReader = Objects.requireNonNull(
                catalogReader,
                "catalogReader не должен быть null"
        );

        this.productUpsertPort = Objects.requireNonNull(
                productUpsertPort,
                "productUpsertPort не должен быть null"
        );
    }

    public CatalogSyncReport synchronize(Path catalogFile) {
        Objects.requireNonNull(
                catalogFile,
                "catalogFile не должен быть null"
        );

        List<TorgsoftProduct> products =
                catalogReader.read(catalogFile);

        int created = 0;
        int updated = 0;
        int failed = 0;

        for (TorgsoftProduct product : products) {
            try {
                ProductSyncAction action =
                        productUpsertPort.upsert(product);

                if (action == ProductSyncAction.CREATED) {
                    created++;
                } else if (action == ProductSyncAction.UPDATED) {
                    updated++;
                }

            } catch (RuntimeException exception) {
                failed++;

                log.error(
                        "Не удалось синхронизировать товар Torgsoft. GoodID: {}",
                        product.goodId(),
                        exception
                );
            }
        }

        CatalogSyncReport report = new CatalogSyncReport(
                products.size(),
                created,
                updated,
                failed
        );

        log.info(
                "Синхронизация каталога завершена. Всего: {}, создано: {}, обновлено: {}, ошибок: {}",
                report.total(),
                report.created(),
                report.updated(),
                report.failed()
        );

        return report;
    }
}