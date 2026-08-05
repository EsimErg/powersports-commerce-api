package kz.powersports.commerce.torgsoft.catalog.sync;

import kz.powersports.commerce.torgsoft.catalog.file.TorgsoftCatalogFileResolver;
import kz.powersports.commerce.torgsoft.catalog.history.TorgsoftCatalogImportHistoryEntry;
import kz.powersports.commerce.torgsoft.catalog.history.TorgsoftCatalogImportHistoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
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
    private final TorgsoftCatalogImportHistoryStore historyStore;

    private final AtomicBoolean importRunning =
            new AtomicBoolean(false);

    public TorgsoftCatalogImportService(
            TorgsoftCatalogFileResolver fileResolver,
            TorgsoftCatalogSynchronizer synchronizer,
            TorgsoftCatalogImportHistoryStore historyStore
    ) {
        this.fileResolver = Objects.requireNonNull(
                fileResolver,
                "fileResolver не должен быть null"
        );

        this.synchronizer = Objects.requireNonNull(
                synchronizer,
                "synchronizer не должен быть null"
        );

        this.historyStore = Objects.requireNonNull(
                historyStore,
                "historyStore не должен быть null"
        );
    }

    public CatalogSyncReport importCatalog() {
        if (!importRunning.compareAndSet(false, true)) {
            throw new TorgsoftCatalogImportAlreadyRunningException();
        }

        String importId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();

        try {
            Path catalogFile =
                    fileResolver.resolveCatalogFile();

            log.info(
                    "Начинается импорт каталога Torgsoft. "
                            + "Import ID: {}, файл: {}",
                    importId,
                    catalogFile
            );

            CatalogSyncReport report =
                    synchronizer.synchronize(catalogFile);

            Instant finishedAt = Instant.now();

            saveHistorySafely(
                    TorgsoftCatalogImportHistoryEntry.success(
                            importId,
                            startedAt,
                            finishedAt,
                            report
                    )
            );

            log.info(
                    "Импорт каталога Torgsoft завершён. "
                            + "Import ID: {}, всего: {}, создано: {}, "
                            + "обновлено: {}, ошибок: {}",
                    importId,
                    report.total(),
                    report.created(),
                    report.updated(),
                    report.failed()
            );

            return report;

        } catch (RuntimeException exception) {
            saveHistorySafely(
                    TorgsoftCatalogImportHistoryEntry.failure(
                            importId,
                            startedAt,
                            Instant.now(),
                            exception
                    )
            );

            throw exception;

        } finally {
            importRunning.set(false);
        }
    }

    private void saveHistorySafely(
            TorgsoftCatalogImportHistoryEntry entry
    ) {
        try {
            historyStore.save(entry);

        } catch (RuntimeException exception) {
            /*
             * Ошибка записи журнала не должна отменять
             * уже выполненную синхронизацию товаров.
             */
            log.error(
                    "Не удалось сохранить историю импорта Torgsoft. "
                            + "Import ID: {}",
                    entry.id(),
                    exception
            );
        }
    }
}