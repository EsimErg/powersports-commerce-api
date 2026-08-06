package kz.powersports.commerce.torgsoft.order.woocommerce;

import kz.powersports.commerce.torgsoft.order.export
        .TorgsoftOrderExportStatusGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = {
                "enabled",
                "order-export.enabled"
        },
        havingValue = "true"
)
public final class RestTorgsoftOrderExportStatusGateway
        implements TorgsoftOrderExportStatusGateway {

    private static final int MAX_ERROR_LENGTH = 500;

    private final RestClient wooCommerceAdminRestClient;

    public RestTorgsoftOrderExportStatusGateway(
            @Qualifier("wooCommerceAdminRestClient")
            RestClient wooCommerceAdminRestClient
    ) {
        this.wooCommerceAdminRestClient =
                wooCommerceAdminRestClient;
    }

    @Override
    public void markExported(
            Long orderId,
            int attempts,
            Instant exportedAt
    ) {
        updateMetadata(
                orderId,
                List.of(
                        metadata(
                                "_torgsoft_export_status",
                                "exported"
                        ),
                        metadata(
                                "_torgsoft_export_attempts",
                                Integer.toString(attempts)
                        ),
                        metadata(
                                "_torgsoft_exported_at",
                                exportedAt.toString()
                        ),
                        metadata(
                                "_torgsoft_export_next_attempt_at",
                                ""
                        ),
                        metadata(
                                "_torgsoft_export_last_error",
                                ""
                        )
                )
        );
    }

    @Override
    public void markRetry(
            Long orderId,
            int attempts,
            Instant nextAttemptAt,
            String lastError
    ) {
        updateMetadata(
                orderId,
                List.of(
                        metadata(
                                "_torgsoft_export_status",
                                "pending"
                        ),
                        metadata(
                                "_torgsoft_export_attempts",
                                Integer.toString(attempts)
                        ),
                        metadata(
                                "_torgsoft_export_next_attempt_at",
                                nextAttemptAt.toString()
                        ),
                        metadata(
                                "_torgsoft_export_last_error",
                                sanitizeError(lastError)
                        )
                )
        );
    }

    @Override
    public void markFailed(
            Long orderId,
            int attempts,
            String lastError
    ) {
        updateMetadata(
                orderId,
                List.of(
                        metadata(
                                "_torgsoft_export_status",
                                "failed"
                        ),
                        metadata(
                                "_torgsoft_export_attempts",
                                Integer.toString(attempts)
                        ),
                        metadata(
                                "_torgsoft_export_next_attempt_at",
                                ""
                        ),
                        metadata(
                                "_torgsoft_export_last_error",
                                sanitizeError(lastError)
                        )
                )
        );
    }

    private void updateMetadata(
            Long orderId,
            List<WooCommerceOrderMetadataUpdateRequest.MetaData>
                    metadata
    ) {
        try {
            wooCommerceAdminRestClient
                    .put()
                    .uri(
                            "/wp-json/wc/v3/orders/{orderId}",
                            orderId
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                            new WooCommerceOrderMetadataUpdateRequest(
                                    metadata
                            )
                    )
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new TorgsoftOrderStatusUpdateException(
                    orderId,
                    exception
            );
        }
    }

    private WooCommerceOrderMetadataUpdateRequest.MetaData
    metadata(
            String key,
            String value
    ) {
        return new WooCommerceOrderMetadataUpdateRequest.MetaData(
                key,
                value
        );
    }

    private String sanitizeError(String error) {
        if (error == null || error.isBlank()) {
            return "";
        }

        String sanitized = error
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();

        if (sanitized.length() <= MAX_ERROR_LENGTH) {
            return sanitized;
        }

        return sanitized.substring(
                0,
                MAX_ERROR_LENGTH
        );
    }
}