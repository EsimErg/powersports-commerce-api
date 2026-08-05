package kz.powersports.commerce.torgsoft.order.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class TorgsoftOrderExportProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TorgsoftOrderExportProcessor.class
            );

    private final TorgsoftOrderExportJobRepository repository;
    private final TorgsoftOrderExporter exporter;
    private final int maxAttempts;
    private final Duration retryDelay;

    public TorgsoftOrderExportProcessor(
            TorgsoftOrderExportJobRepository repository,
            TorgsoftOrderExporter exporter,
            int maxAttempts,
            Duration retryDelay
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository не должен быть null"
        );

        this.exporter = Objects.requireNonNull(
                exporter,
                "exporter не должен быть null"
        );

        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "maxAttempts должен быть не меньше 1"
            );
        }

        this.maxAttempts = maxAttempts;

        this.retryDelay = Objects.requireNonNull(
                retryDelay,
                "retryDelay не должен быть null"
        );

        if (retryDelay.isNegative()) {
            throw new IllegalArgumentException(
                    "retryDelay не может быть отрицательным"
            );
        }
    }

    public int processDueJobs(
            Instant now,
            int limit
    ) {
        Objects.requireNonNull(
                now,
                "now не должен быть null"
        );

        List<Long> dueOrderIds =
                repository.findDueOrderIds(
                        now,
                        limit
                );

        int processed = 0;

        for (Long orderId : dueOrderIds) {
            processOrder(orderId, now);
            processed++;
        }

        return processed;
    }

    private void processOrder(
            Long orderId,
            Instant now
    ) {
        TorgsoftOrderExportJob currentJob =
                repository
                        .findByOrderId(orderId)
                        .orElse(null);

        if (currentJob == null) {
            log.warn(
                    "В очереди Torgsoft найден Order ID без задания: {}",
                    orderId
            );

            repository.removeFromPending(orderId);
            return;
        }

        if (currentJob.status()
                == TorgsoftOrderExportStatus.EXPORTED) {
            repository.removeFromPending(orderId);
            return;
        }

        int currentAttempt =
                currentJob.attempts() + 1;

        TorgsoftOrderExportJob processingJob =
                new TorgsoftOrderExportJob(
                        currentJob.wooCommerceOrderId(),
                        currentJob.orderNumber(),
                        currentJob.createdAt(),
                        currentJob.nextAttemptAt(),
                        currentAttempt,
                        TorgsoftOrderExportStatus.PROCESSING,
                        null
                );

        repository.update(processingJob);
        repository.removeFromPending(orderId);

        try {
            exporter.export(orderId);

            TorgsoftOrderExportJob exportedJob =
                    new TorgsoftOrderExportJob(
                            processingJob.wooCommerceOrderId(),
                            processingJob.orderNumber(),
                            processingJob.createdAt(),
                            now,
                            processingJob.attempts(),
                            TorgsoftOrderExportStatus.EXPORTED,
                            null
                    );

            repository.update(exportedJob);

            log.info(
                    "Заказ экспортирован в Torgsoft. "
                            + "Order ID: {}, попытка: {}",
                    orderId,
                    currentAttempt
            );

        } catch (RuntimeException exception) {
            handleFailure(
                    processingJob,
                    now,
                    exception
            );
        }
    }

    private void handleFailure(
            TorgsoftOrderExportJob processingJob,
            Instant now,
            RuntimeException exception
    ) {
        String errorMessage =
                safeErrorMessage(exception);

        if (processingJob.attempts() >= maxAttempts) {
            TorgsoftOrderExportJob failedJob =
                    new TorgsoftOrderExportJob(
                            processingJob.wooCommerceOrderId(),
                            processingJob.orderNumber(),
                            processingJob.createdAt(),
                            now,
                            processingJob.attempts(),
                            TorgsoftOrderExportStatus.FAILED,
                            errorMessage
                    );

            repository.update(failedJob);

            log.error(
                    "Заказ не удалось экспортировать в Torgsoft "
                            + "после {} попыток. Order ID: {}",
                    processingJob.attempts(),
                    processingJob.wooCommerceOrderId(),
                    exception
            );

            return;
        }

        Instant nextAttemptAt =
                now.plus(retryDelay);

        TorgsoftOrderExportJob retryJob =
                new TorgsoftOrderExportJob(
                        processingJob.wooCommerceOrderId(),
                        processingJob.orderNumber(),
                        processingJob.createdAt(),
                        nextAttemptAt,
                        processingJob.attempts(),
                        TorgsoftOrderExportStatus.PENDING,
                        errorMessage
                );

        repository.update(retryJob);

        repository.schedule(
                retryJob.wooCommerceOrderId(),
                retryJob.nextAttemptAt()
        );

        log.warn(
                "Экспорт заказа Torgsoft завершился ошибкой. "
                        + "Order ID: {}, попытка: {}, "
                        + "следующая попытка: {}",
                processingJob.wooCommerceOrderId(),
                processingJob.attempts(),
                nextAttemptAt
        );
    }

    private String safeErrorMessage(
            Throwable throwable
    ) {
        String message = throwable.getMessage();

        if (message == null || message.isBlank()) {
            message = throwable
                    .getClass()
                    .getSimpleName();
        }

        return message.length() > 500
                ? message.substring(0, 500)
                : message;
    }
}