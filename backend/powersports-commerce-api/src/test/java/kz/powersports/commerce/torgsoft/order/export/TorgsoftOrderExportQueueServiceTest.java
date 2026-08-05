package kz.powersports.commerce.torgsoft.order.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TorgsoftOrderExportQueueServiceTest {

    @Mock
    private TorgsoftOrderExportJobRepository repository;

    private TorgsoftOrderExportQueueService queueService;

    @BeforeEach
    void setUp() {
        queueService =
                new TorgsoftOrderExportQueueService(
                        repository
                );
    }

    @Test
    void shouldEnqueuePendingOrderExportJob() {
        when(
                repository.enqueue(
                        org.mockito.ArgumentMatchers.any()
                )
        ).thenReturn(true);

        boolean result = queueService.enqueue(
                25L,
                "25"
        );

        assertTrue(result);

        ArgumentCaptor<TorgsoftOrderExportJob> captor =
                ArgumentCaptor.forClass(
                        TorgsoftOrderExportJob.class
                );

        verify(repository).enqueue(captor.capture());

        TorgsoftOrderExportJob job =
                captor.getValue();

        assertEquals(
                25L,
                job.wooCommerceOrderId()
        );

        assertEquals(
                "25",
                job.orderNumber()
        );

        assertEquals(
                TorgsoftOrderExportStatus.PENDING,
                job.status()
        );

        assertEquals(0, job.attempts());
        assertNotNull(job.createdAt());
        assertNotNull(job.nextAttemptAt());
    }
}