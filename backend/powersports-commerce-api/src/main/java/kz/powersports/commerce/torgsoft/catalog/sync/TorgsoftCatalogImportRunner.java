package kz.powersports.commerce.torgsoft.catalog.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = "enabled",
        havingValue = "true"
)
public class TorgsoftCatalogImportRunner
        implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TorgsoftCatalogImportRunner.class
            );

    private final TorgsoftCatalogImportService importService;
    private final boolean importOnStartupEnabled;

    public TorgsoftCatalogImportRunner(
            TorgsoftCatalogImportService importService,
            @Value("${torgsoft.import-on-startup.enabled:false}")
            boolean importOnStartupEnabled
    ) {
        this.importService = Objects.requireNonNull(
                importService,
                "importService не должен быть null"
        );

        this.importOnStartupEnabled =
                importOnStartupEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!importOnStartupEnabled) {
            return;
        }

        try {
            CatalogSyncReport report =
                    importService.importCatalog();

            log.info(
                    "Автоматический импорт завершён: {}",
                    report
            );

        } catch (RuntimeException exception) {
            /*
             * Ошибка импорта не должна полностью останавливать
             * backend магазина.
             */
            log.error(
                    "Автоматический импорт каталога Torgsoft завершился ошибкой",
                    exception
            );
        }
    }
}