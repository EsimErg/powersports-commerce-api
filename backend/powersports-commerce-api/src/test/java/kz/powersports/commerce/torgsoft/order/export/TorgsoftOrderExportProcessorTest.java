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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TorgsoftOrderExportProcessorTest {

    private static final Instant NOW =
            Instant.parse("2026-08-05T09:00:00Z");

    @Mock
    private TorgsoftOrderExportJobRepository repository;

    @Mock
    private TorgsoftOrderExporter exporter;

    private TorgsoftOrderExportProcessor processor;

    @BeforeEach
    void setUp() {
        processor =
                new TorgsoftOrderExportProcessor(
                        repository,
                        exporter,
                        3,
                        Duration.ofMinutes(5)
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

        when(
                repository.findDueOrderIds(
                        NOW,
                        10
                )
        ).thenReturn(List.of(15L));

        when(
                repository.findByOrderId(15L)
        ).thenReturn(Optional.of(job));

        int processed =
                processor.processDueJobs(
                        NOW,
                        10
                );

        assertThat(processed).isEqualTo(1);

        verify(exporter).export(15L);
        verify(repository).removeFromPending(15L);

        ArgumentCaptor<TorgsoftOrderExportJob> captor =
                ArgumentCaptor.forClass(
                        TorgsoftOrderExportJob.class
                );

        verify(repository,
                org.mockito.Mockito.times(2)
        ).update(captor.capture());

        TorgsoftOrderExportJob exportedJob =
                captor.getAllValues().get(1);

        assertThat(exportedJob.status())
                .isEqualTo(
                        TorgsoftOrderExportStatus.EXPORTED
                );

        assertThat(exportedJob.attempts())
                .isEqualTo(1);
    }

    @Test
    void shouldScheduleRetryAfterFailure() {
        TorgsoftOrderExportJob job =
                TorgsoftOrderExportJob.pending(
                        15L,
                        "15",
                        NOW
                );

        when(
                repository.findDueOrderIds(
                        NOW,
                        10
                )
        ).thenReturn(List.of(15L));

        when(
                repository.findByOrderId(15L)
        ).thenReturn(Optional.of(job));

        doThrow(
                new RuntimeException("Torgsoft недоступен")
        ).when(exporter).export(15L);

        processor.processDueJobs(
                NOW,
                10
        );

        verify(repository).schedule(
                15L,
                NOW.plus(Duration.ofMinutes(5))
        );

        ArgumentCaptor<TorgsoftOrderExportJob> captor =
                ArgumentCaptor.forClass(
                        TorgsoftOrderExportJob.class
                );

        verify(repository,
                org.mockito.Mockito.times(2)
        ).update(captor.capture());

        TorgsoftOrderExportJob retryJob =
                captor.getAllValues().get(1);

        assertThat(retryJob.status())
                .isEqualTo(
                        TorgsoftOrderExportStatus.PENDING
                );

        assertThat(retryJob.attempts())
                .isEqualTo(1);

        assertThat(retryJob.lastError())
                .isEqualTo("Torgsoft недоступен");
    }
}