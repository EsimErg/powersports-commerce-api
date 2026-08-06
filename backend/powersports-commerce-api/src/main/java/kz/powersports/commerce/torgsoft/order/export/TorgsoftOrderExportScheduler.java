package kz.powersports.commerce.torgsoft.order.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.Objects;

public final class TorgsoftOrderExportScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TorgsoftOrderExportScheduler.class
            );

    private final TorgsoftOrderExportProcessor processor;
    private final int batchSize;

    public TorgsoftOrderExportScheduler(
            TorgsoftOrderExportProcessor processor,
            int batchSize
    ) {
        this.processor = Objects.requireNonNull(
                processor,
                "processor не должен быть null"
        );

        if (batchSize < 1 || batchSize > 100) {
            throw new IllegalArgumentException(
                    "batchSize должен находиться в диапазоне 1–100"
            );
        }

        this.batchSize = batchSize;
    }

    @Scheduled(
            fixedDelayString =
                    "${torgsoft.order-export.worker.fixed-delay:PT30S}",
            initialDelayString =
                    "${torgsoft.order-export.worker.initial-delay:PT10S}"
    )
    public void processQueue() {
        try {
            int processed =
                    processor.processDueJobs(
                            Instant.now(),
                            batchSize
                    );

            if (processed > 0) {
                log.info(
                        "Обработка очереди экспорта Torgsoft завершена. "
                                + "Обработано заданий: {}",
                        processed
                );
            }

        } catch (RuntimeException exception) {
            /*
             * Ошибка обработки очереди не должна
             * останавливать backend магазина.
             */
            log.error(
                    "Ошибка обработки очереди "
                            + "экспорта заказов Torgsoft",
                    exception
            );
        }
    }
}