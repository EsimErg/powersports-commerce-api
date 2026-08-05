package kz.powersports.commerce.torgsoft.order.export;

import java.time.Instant;

public record TorgsoftOrderExportJob(
        Long wooCommerceOrderId,
        String orderNumber,
        Instant createdAt,
        Instant nextAttemptAt,
        int attempts,
        TorgsoftOrderExportStatus status,
        String lastError
) {

    public TorgsoftOrderExportJob {
        if (wooCommerceOrderId == null
                || wooCommerceOrderId <= 0) {
            throw new IllegalArgumentException(
                    "WooCommerce order ID должен быть положительным"
            );
        }

        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Номер заказа не должен быть пустым"
            );
        }

        if (createdAt == null) {
            throw new IllegalArgumentException(
                    "Время создания задания обязательно"
            );
        }

        if (nextAttemptAt == null) {
            throw new IllegalArgumentException(
                    "Время следующей попытки обязательно"
            );
        }

        if (attempts < 0) {
            throw new IllegalArgumentException(
                    "Количество попыток не может быть отрицательным"
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "Статус задания обязателен"
            );
        }

        orderNumber = orderNumber.trim();
        lastError = normalizeError(lastError);
    }

    public static TorgsoftOrderExportJob pending(
            Long wooCommerceOrderId,
            String orderNumber,
            Instant now
    ) {
        return new TorgsoftOrderExportJob(
                wooCommerceOrderId,
                orderNumber,
                now,
                now,
                0,
                TorgsoftOrderExportStatus.PENDING,
                null
        );
    }

    private static String normalizeError(String error) {
        if (error == null || error.isBlank()) {
            return null;
        }

        String normalized = error.trim();

        return normalized.length() > 500
                ? normalized.substring(0, 500)
                : normalized;
    }
}