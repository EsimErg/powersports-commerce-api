package kz.powersports.commerce.torgsoft.order.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TorgsoftOrderExportSchedulerTest {

    @Mock
    private TorgsoftOrderExportProcessor processor;

    private TorgsoftOrderExportScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler =
                new TorgsoftOrderExportScheduler(
                        processor,
                        20
                );
    }

    @Test
    void shouldProcessDueOrderExportJobs() {
        when(
                processor.processDueJobs(
                        any(Instant.class),
                        eq(20)
                )
        ).thenReturn(2);

        scheduler.processQueue();

        verify(processor).processDueJobs(
                any(Instant.class),
                eq(20)
        );
    }

    @Test
    void shouldNotPropagateProcessorFailure() {
        when(
                processor.processDueJobs(
                        any(Instant.class),
                        eq(20)
                )
        ).thenThrow(
                new RuntimeException("Test error")
        );

        assertThatCode(
                scheduler::processQueue
        ).doesNotThrowAnyException();

        verify(processor).processDueJobs(
                any(Instant.class),
                eq(20)
        );
    }
}