package kz.powersports.commerce.torgsoft.testing;

import kz.powersports.commerce.torgsoft.catalog.model.TorgsoftProduct;
import kz.powersports.commerce.torgsoft.catalog.sync.ProductSyncAction;
import kz.powersports.commerce.torgsoft.catalog.sync.TorgsoftProductUpsertPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = "enabled",
        havingValue = "true"
)
public class TorgsoftProductSmokeTestRunner
        implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TorgsoftProductSmokeTestRunner.class
            );

    private final TorgsoftProductUpsertPort productUpsertPort;
    private final boolean smokeTestEnabled;

    public TorgsoftProductSmokeTestRunner(
            TorgsoftProductUpsertPort productUpsertPort,
            @Value("${torgsoft.smoke-test.enabled:false}")
            boolean smokeTestEnabled
    ) {
        this.productUpsertPort = Objects.requireNonNull(
                productUpsertPort,
                "productUpsertPort не должен быть null"
        );

        this.smokeTestEnabled = smokeTestEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info(
                "Настройка Torgsoft smoke test: {}",
                smokeTestEnabled
        );

        if (!smokeTestEnabled) {
            log.info("Тестовая синхронизация Torgsoft отключена");
            return;
        }

        TorgsoftProduct testProduct =
                new TorgsoftProduct(
                        "SMOKE-TEST-001",
                        "TS-SMOKE-001",
                        "Тест интеграции Torgsoft",
                        new BigDecimal("1000.00"),
                        new BigDecimal("3")
                );

        log.info(
                "Запуск тестовой синхронизации Torgsoft. GoodID: {}",
                testProduct.goodId()
        );

        ProductSyncAction action =
                productUpsertPort.upsert(testProduct);

        log.info(
                "Тестовая синхронизация Torgsoft завершена. "
                        + "GoodID: {}, действие: {}",
                testProduct.goodId(),
                action
        );
    }
}