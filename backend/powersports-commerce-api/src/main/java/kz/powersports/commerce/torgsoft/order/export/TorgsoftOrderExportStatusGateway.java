package kz.powersports.commerce.torgsoft.order.export;

import java.time.Instant;

public interface TorgsoftOrderExportStatusGateway {

    void markExported(
            Long orderId,
            int attempts,
            Instant exportedAt
    );

    void markRetry(
            Long orderId,
            int attempts,
            Instant nextAttemptAt,
            String lastError
    );

    void markFailed(
            Long orderId,
            int attempts,
            String lastError
    );
}