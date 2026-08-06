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

    private final TorgsoftOrderExportStatusGateway
            statusGateway;

    private final int maxAttempts;
    private final Duration retryDelay;

    public TorgsoftOrderExportProcessor(
            TorgsoftOrderExportJobRepository repository,
            TorgsoftOrderExporter exporter,
            TorgsoftOrderExportStatusGateway statusGateway,
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

        this.statusGateway = Objects.requireNonNull(
                statusGateway,
                "statusGateway не должен быть null"
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

        if (limit < 1) {
            throw new IllegalArgumentException(
                    "limit должен быть не меньше 1"
            );
        }

        List<Long> dueOrderIds =
                repository.findDueOrderIds(
                        now,
                        limit
                );

        int processed = 0;

        for (Long orderId : dueOrderIds) {
            processOrder(
                    orderId,
                    now
            );

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
                    "В очереди Torgsoft найден "
                            + "Order ID без задания: {}",
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

        /*
         * В этом try находится только реальный экспорт.
         *
         * Обновление metadata WooCommerce выполняется позже
         * и не должно попадать в обработку ошибки экспорта.
         */
        try {
            exporter.export(orderId);

        } catch (RuntimeException exception) {
            handleFailure(
                    processingJob,
                    now,
                    exception
            );

            return;
        }

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

        /*
         * Сначала фиксируем успешный экспорт в Redis.
         */
        repository.update(exportedJob);

        /*
         * Затем пытаемся обновить metadata WooCommerce.
         *
         * Ошибка этого запроса не запускает повторный
         * экспорт уже созданного файла.
         */
        markExportedSafely(
                exportedJob,
                now
        );

        log.info(
                "Заказ экспортирован в Torgsoft. "
                        + "Order ID: {}, попытка: {}",
                orderId,
                currentAttempt
        );
    }

    private void handleFailure(
            TorgsoftOrderExportJob processingJob,
            Instant now,
            RuntimeException exception
    ) {
        String errorMessage =
                safeErrorMessage(exception);

        if (processingJob.attempts() >= maxAttempts) {
            markAsFailed(
                    processingJob,
                    now,
                    errorMessage,
                    exception
            );

            return;
        }

        scheduleRetry(
                processingJob,
                now,
                errorMessage
        );
    }

    private void markAsFailed(
            TorgsoftOrderExportJob processingJob,
            Instant now,
            String errorMessage,
            RuntimeException exception
    ) {
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

        markFailedSafely(
                failedJob
        );

        log.error(
                "Заказ не удалось экспортировать "
                        + "в Torgsoft после {} попыток. "
                        + "Order ID: {}",
                processingJob.attempts(),
                processingJob.wooCommerceOrderId(),
                exception
        );
    }

    private void scheduleRetry(
            TorgsoftOrderExportJob processingJob,
            Instant now,
            String errorMessage
    ) {
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

        markRetrySafely(
                retryJob
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

    private void markExportedSafely(
            TorgsoftOrderExportJob exportedJob,
            Instant exportedAt
    ) {
        try {
            statusGateway.markExported(
                    exportedJob.wooCommerceOrderId(),
                    exportedJob.attempts(),
                    exportedAt
            );

        } catch (RuntimeException exception) {
            log.error(
                    "Файл заказа успешно экспортирован, "
                            + "но metadata WooCommerce "
                            + "не обновлены. Order ID: {}",
                    exportedJob.wooCommerceOrderId(),
                    exception
            );
        }
    }

    private void markRetrySafely(
            TorgsoftOrderExportJob retryJob
    ) {
        try {
            statusGateway.markRetry(
                    retryJob.wooCommerceOrderId(),
                    retryJob.attempts(),
                    retryJob.nextAttemptAt(),
                    retryJob.lastError()
            );

        } catch (RuntimeException exception) {
            log.error(
                    "Повтор экспорта запланирован в Redis, "
                            + "но metadata WooCommerce "
                            + "не обновлены. Order ID: {}",
                    retryJob.wooCommerceOrderId(),
                    exception
            );
        }
    }

    private void markFailedSafely(
            TorgsoftOrderExportJob failedJob
    ) {
        try {
            statusGateway.markFailed(
                    failedJob.wooCommerceOrderId(),
                    failedJob.attempts(),
                    failedJob.lastError()
            );

        } catch (RuntimeException exception) {
            log.error(
                    "Статус FAILED сохранён в Redis, "
                            + "но metadata WooCommerce "
                            + "не обновлены. Order ID: {}",
                    failedJob.wooCommerceOrderId(),
                    exception
            );
        }
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

        String sanitized = message
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();

        return sanitized.length() > 500
                ? sanitized.substring(0, 500)
                : sanitized;
    }
}