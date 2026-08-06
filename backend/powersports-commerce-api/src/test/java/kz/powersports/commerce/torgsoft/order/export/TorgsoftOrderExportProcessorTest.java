package kz.powersports.commerce.torgsoft.order.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TorgsoftOrderExportProcessorTest {

    private static final Instant NOW =
            Instant.parse("2026-08-05T09:00:00Z");

    private static final Duration RETRY_DELAY =
            Duration.ofMinutes(5);

    @Mock
    private TorgsoftOrderExportJobRepository repository;

    @Mock
    private TorgsoftOrderExporter exporter;

    @Mock
    private TorgsoftOrderExportStatusGateway statusGateway;

    private TorgsoftOrderExportProcessor processor;

    @BeforeEach
    void setUp() {
        processor =
                new TorgsoftOrderExportProcessor(
                        repository,
                        exporter,
                        statusGateway,
                        3,
                        RETRY_DELAY
                );
    }

    @Test
    void shouldMarkOrderAsExported() {
        TorgsoftOrderExportJob job =
                TorgsoftOrderExportJob.pending(
                        15L,
                        "15",
                        NOW
                );

        prepareDueJob(job);

        int processed =
                processor.processDueJobs(
                        NOW,
                        10
                );

        assertThat(processed).isEqualTo(1);

        verify(exporter).export(15L);

        verify(statusGateway).markExported(
                15L,
                1,
                NOW
        );

        verify(repository).removeFromPending(15L);

        ArgumentCaptor<TorgsoftOrderExportJob> captor =
                ArgumentCaptor.forClass(
                        TorgsoftOrderExportJob.class
                );

        verify(repository, times(2))
                .update(captor.capture());

        TorgsoftOrderExportJob exportedJob =
                captor.getAllValues().get(1);

        assertThat(exportedJob.status())
                .isEqualTo(
                        TorgsoftOrderExportStatus.EXPORTED
                );

        assertThat(exportedJob.attempts())
                .isEqualTo(1);

        assertThat(exportedJob.lastError())
                .isNull();
    }

    @Test
    void shouldScheduleRetryAfterFailure() {
        TorgsoftOrderExportJob job =
                TorgsoftOrderExportJob.pending(
                        15L,
                        "15",
                        NOW
                );

        prepareDueJob(job);

        doThrow(
                new RuntimeException(
                        "Torgsoft недоступен"
                )
        ).when(exporter).export(15L);

        processor.processDueJobs(
                NOW,
                10
        );

        Instant nextAttemptAt =
                NOW.plus(RETRY_DELAY);

        verify(repository).schedule(
                15L,
                nextAttemptAt
        );

        verify(statusGateway).markRetry(
                15L,
                1,
                nextAttemptAt,
                "Torgsoft недоступен"
        );

        ArgumentCaptor<TorgsoftOrderExportJob> captor =
                ArgumentCaptor.forClass(
                        TorgsoftOrderExportJob.class
                );

        verify(repository, times(2))
                .update(captor.capture());

        TorgsoftOrderExportJob retryJob =
                captor.getAllValues().get(1);

        assertThat(retryJob.status())
                .isEqualTo(
                        TorgsoftOrderExportStatus.PENDING
                );

        assertThat(retryJob.attempts())
                .isEqualTo(1);

        assertThat(retryJob.nextAttemptAt())
                .isEqualTo(nextAttemptAt);

        assertThat(retryJob.lastError())
                .isEqualTo(
                        "Torgsoft недоступен"
                );
    }

    @Test
    void shouldMarkOrderAsFailedAfterLastAttempt() {
        TorgsoftOrderExportJob job =
                new TorgsoftOrderExportJob(
                        15L,
                        "15",
                        NOW,
                        NOW,
                        2,
                        TorgsoftOrderExportStatus.PENDING,
                        "Предыдущая ошибка"
                );

        prepareDueJob(job);

        doThrow(
                new RuntimeException(
                        "Torgsoft недоступен"
                )
        ).when(exporter).export(15L);

        processor.processDueJobs(
                NOW,
                10
        );

        verify(statusGateway).markFailed(
                15L,
                3,
                "Torgsoft недоступен"
        );

        verify(
                repository,
                never()
        ).schedule(
                anyLong(),
                any()
        );

        ArgumentCaptor<TorgsoftOrderExportJob> captor =
                ArgumentCaptor.forClass(
                        TorgsoftOrderExportJob.class
                );

        verify(repository, times(2))
                .update(captor.capture());

        TorgsoftOrderExportJob failedJob =
                captor.getAllValues().get(1);

        assertThat(failedJob.status())
                .isEqualTo(
                        TorgsoftOrderExportStatus.FAILED
                );

        assertThat(failedJob.attempts())
                .isEqualTo(3);

        assertThat(failedJob.lastError())
                .isEqualTo(
                        "Torgsoft недоступен"
                );
    }

    @Test
    void metadataFailureShouldNotRepeatSuccessfulExport() {
        TorgsoftOrderExportJob job =
                TorgsoftOrderExportJob.pending(
                        15L,
                        "15",
                        NOW
                );

        prepareDueJob(job);

        doThrow(
                new RuntimeException(
                        "WooCommerce недоступен"
                )
        ).when(statusGateway).markExported(
                15L,
                1,
                NOW
        );

        int processed =
                processor.processDueJobs(
                        NOW,
                        10
                );

        assertThat(processed).isEqualTo(1);

        verify(exporter).export(15L);

        verify(
                repository,
                never()
        ).schedule(
                anyLong(),
                any()
        );

        ArgumentCaptor<TorgsoftOrderExportJob> captor =
                ArgumentCaptor.forClass(
                        TorgsoftOrderExportJob.class
                );

        verify(repository, times(2))
                .update(captor.capture());

        TorgsoftOrderExportJob exportedJob =
                captor.getAllValues().get(1);

        assertThat(exportedJob.status())
                .isEqualTo(
                        TorgsoftOrderExportStatus.EXPORTED
                );
    }

    private void prepareDueJob(
            TorgsoftOrderExportJob job
    ) {
        when(
                repository.findDueOrderIds(
                        NOW,
                        10
                )
        ).thenReturn(
                List.of(
                        job.wooCommerceOrderId()
                )
        );

        when(
                repository.findByOrderId(
                        job.wooCommerceOrderId()
                )
        ).thenReturn(
                Optional.of(job)
        );
    }
}