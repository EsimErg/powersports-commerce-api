package kz.powersports.commerce.torgsoft.catalog.history;

import kz.powersports.commerce.torgsoft.catalog.sync.CatalogSyncReport;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record TorgsoftCatalogImportHistoryEntry(
        String id,
        Instant startedAt,
        Instant finishedAt,
        long durationMs,
        TorgsoftCatalogImportStatus status,
        int total,
        int created,
        int updated,
        int failed,
        String errorMessage
) {

    public static TorgsoftCatalogImportHistoryEntry success(
            String id,
            Instant startedAt,
            Instant finishedAt,
            CatalogSyncReport report
    ) {
        Objects.requireNonNull(report, "report не должен быть null");

        return new TorgsoftCatalogImportHistoryEntry(
                id,
                startedAt,
                finishedAt,
                duration(startedAt, finishedAt),
                TorgsoftCatalogImportStatus.SUCCESS,
                report.total(),
                report.created(),
                report.updated(),
                report.failed(),
                null
        );
    }

    public static TorgsoftCatalogImportHistoryEntry failure(
            String id,
            Instant startedAt,
            Instant finishedAt,
            Throwable throwable
    ) {
        return new TorgsoftCatalogImportHistoryEntry(
                id,
                startedAt,
                finishedAt,
                duration(startedAt, finishedAt),
                TorgsoftCatalogImportStatus.FAILED,
                0,
                0,
                0,
                0,
                safeErrorMessage(throwable)
        );
    }

    private static long duration(
            Instant startedAt,
            Instant finishedAt
    ) {
        return Math.max(
                0,
                Duration.between(startedAt, finishedAt).toMillis()
        );
    }

    private static String safeErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Неизвестная ошибка";
        }

        String message = throwable.getMessage();

        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }

        return message.length() > 500
                ? message.substring(0, 500)
                : message;
    }
}