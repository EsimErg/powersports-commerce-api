package kz.powersports.commerce.torgsoft.catalog.sync;

import kz.powersports.commerce.torgsoft.catalog.file.TorgsoftCatalogFileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = "enabled",
        havingValue = "true"
)
public class TorgsoftCatalogImportService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TorgsoftCatalogImportService.class
            );

    private final TorgsoftCatalogFileResolver fileResolver;
    private final TorgsoftCatalogSynchronizer synchronizer;

    private final AtomicBoolean importRunning =
            new AtomicBoolean(false);

    public TorgsoftCatalogImportService(
            TorgsoftCatalogFileResolver fileResolver,
            TorgsoftCatalogSynchronizer synchronizer
    ) {
        this.fileResolver = Objects.requireNonNull(
                fileResolver,
                "fileResolver не должен быть null"
        );

        this.synchronizer = Objects.requireNonNull(
                synchronizer,
                "synchronizer не должен быть null"
        );
    }

    public CatalogSyncReport importCatalog() {
        if (!importRunning.compareAndSet(false, true)) {
            throw new TorgsoftCatalogImportAlreadyRunningException();
        }

        try {
            Path catalogFile =
                    fileResolver.resolveCatalogFile();

            log.info(
                    "Начинается импорт каталога Torgsoft. Файл: {}",
                    catalogFile
            );

            CatalogSyncReport report =
                    synchronizer.synchronize(catalogFile);

            log.info(
                    "Импорт каталога Torgsoft завершён. "
                            + "Всего: {}, создано: {}, "
                            + "обновлено: {}, ошибок: {}",
                    report.total(),
                    report.created(),
                    report.updated(),
                    report.failed()
            );

            return report;

        } finally {
            importRunning.set(false);
        }
    }
}