package kz.powersports.commerce.torgsoft.order.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = "enabled",
        havingValue = "true"
)
public class TorgsoftOrderExportQueueService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TorgsoftOrderExportQueueService.class
            );

    private final TorgsoftOrderExportJobRepository repository;

    public TorgsoftOrderExportQueueService(
            TorgsoftOrderExportJobRepository repository
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository не должен быть null"
        );
    }

    public boolean enqueue(
            Long wooCommerceOrderId,
            String orderNumber
    ) {
        TorgsoftOrderExportJob job =
                TorgsoftOrderExportJob.pending(
                        wooCommerceOrderId,
                        orderNumber,
                        Instant.now()
                );

        boolean created = repository.enqueue(job);

        if (created) {
            log.info(
                    "Заказ поставлен в очередь экспорта Torgsoft. "
                            + "Order ID: {}, номер: {}",
                    wooCommerceOrderId,
                    orderNumber
            );
        } else {
            log.info(
                    "Заказ уже присутствует в очереди Torgsoft. "
                            + "Order ID: {}",
                    wooCommerceOrderId
            );
        }

        return created;
    }
}