package kz.powersports.commerce.torgsoft.catalog.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;

public class TorgsoftCatalogScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TorgsoftCatalogScheduler.class
            );

    private final TorgsoftCatalogImportService importService;

    public TorgsoftCatalogScheduler(
            TorgsoftCatalogImportService importService
    ) {
        this.importService = Objects.requireNonNull(
                importService,
                "importService не должен быть null"
        );
    }

    @Scheduled(
            fixedDelayString =
                    "${torgsoft.scheduler.fixed-delay:PT15M}",
            initialDelayString =
                    "${torgsoft.scheduler.initial-delay:PT1M}"
    )
    public void synchronizeCatalog() {
        try {
            CatalogSyncReport report =
                    importService.importCatalog();

            log.info(
                    "Плановая синхронизация Torgsoft завершена. "
                            + "Всего: {}, создано: {}, "
                            + "обновлено: {}, ошибок: {}",
                    report.total(),
                    report.created(),
                    report.updated(),
                    report.failed()
            );

        } catch (
                TorgsoftCatalogImportAlreadyRunningException exception
        ) {
            /*
             * Например, в этот момент пользователь
             * запустил ручной импорт.
             */
            log.warn(
                    "Плановая синхронизация Torgsoft пропущена: "
                            + "импорт уже выполняется"
            );

        } catch (RuntimeException exception) {
            /*
             * Ошибка Torgsoft не должна останавливать
             * backend интернет-магазина.
             */
            log.error(
                    "Плановая синхронизация каталога "
                            + "Torgsoft завершилась ошибкой",
                    exception
            );
        }
    }
}