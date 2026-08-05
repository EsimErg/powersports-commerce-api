package kz.powersports.commerce.torgsoft.order.export;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TorgsoftOrderExportJobRepository {

    boolean enqueue(TorgsoftOrderExportJob job);

    Optional<TorgsoftOrderExportJob> findByOrderId(
            Long wooCommerceOrderId
    );

    List<Long> findDueOrderIds(
            Instant now,
            int limit
    );

    void update(TorgsoftOrderExportJob job);

    void schedule(
            Long wooCommerceOrderId,
            Instant nextAttemptAt
    );

    void removeFromPending(
            Long wooCommerceOrderId
    );
}