package kz.powersports.commerce.torgsoft.config;

import kz.powersports.commerce.torgsoft.order.export
        .TorgsoftOrderExportJobRepository;
import kz.powersports.commerce.torgsoft.order.export
        .TorgsoftOrderExportProcessor;
import kz.powersports.commerce.torgsoft.order.export
        .TorgsoftOrderExportScheduler;
import kz.powersports.commerce.torgsoft.order.export
        .TorgsoftOrderExportStatusGateway;
import kz.powersports.commerce.torgsoft.order.export
        .TorgsoftOrderExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = {
                "enabled",
                "order-export.enabled"
        },
        havingValue = "true"
)
public class TorgsoftOrderExportConfiguration {

    @Bean
    public TorgsoftOrderExportProcessor
    torgsoftOrderExportProcessor(
            TorgsoftOrderExportJobRepository repository,
            TorgsoftOrderExporter exporter,
            TorgsoftOrderExportStatusGateway statusGateway,

            @Value(
                    "${torgsoft.order-export.worker.max-attempts:5}"
            )
            int maxAttempts,

            @Value(
                    "${torgsoft.order-export.worker.retry-delay:PT5M}"
            )
            Duration retryDelay
    ) {
        return new TorgsoftOrderExportProcessor(
                repository,
                exporter,
                statusGateway,
                maxAttempts,
                retryDelay
        );
    }

    @Bean
    public TorgsoftOrderExportScheduler
    torgsoftOrderExportScheduler(
            TorgsoftOrderExportProcessor processor,

            @Value(
                    "${torgsoft.order-export.worker.batch-size:20}"
            )
            int batchSize
    ) {
        return new TorgsoftOrderExportScheduler(
                processor,
                batchSize
        );
    }
}