package kz.powersports.commerce.torgsoft.catalog.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TorgsoftCatalogSchedulerTest {

    @Mock
    private TorgsoftCatalogImportService importService;

    private TorgsoftCatalogScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler =
                new TorgsoftCatalogScheduler(
                        importService
                );
    }

    @Test
    void shouldRunCatalogImport() {
        CatalogSyncReport report =
                new CatalogSyncReport(
                        5,
                        1,
                        4,
                        0
                );

        when(
                importService.importCatalog()
        ).thenReturn(report);

        scheduler.synchronizeCatalog();

        verify(importService).importCatalog();
    }

    @Test
    void shouldNotPropagateImportFailure() {
        when(
                importService.importCatalog()
        ).thenThrow(
                new RuntimeException("Test error")
        );

        assertDoesNotThrow(
                scheduler::synchronizeCatalog
        );

        verify(importService).importCatalog();
    }

    @Test
    void shouldSkipWhenImportIsAlreadyRunning() {
        when(
                importService.importCatalog()
        ).thenThrow(
                new TorgsoftCatalogImportAlreadyRunningException()
        );

        assertDoesNotThrow(
                scheduler::synchronizeCatalog
        );

        verify(importService).importCatalog();
    }
}