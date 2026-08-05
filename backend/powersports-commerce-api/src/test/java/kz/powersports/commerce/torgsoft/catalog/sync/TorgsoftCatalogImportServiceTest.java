package kz.powersports.commerce.torgsoft.catalog.sync;

import kz.powersports.commerce.torgsoft.catalog.file.TorgsoftCatalogFileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import kz.powersports.commerce.torgsoft.catalog.history.TorgsoftCatalogImportHistoryStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TorgsoftCatalogImportServiceTest {

    @Mock
    private TorgsoftCatalogFileResolver fileResolver;

    @Mock
    private TorgsoftCatalogSynchronizer synchronizer;

    private TorgsoftCatalogImportService importService;

    @BeforeEach
    void setUp() {
        importService =
                new TorgsoftCatalogImportService(
                        fileResolver,
                        synchronizer,
                        historyStore
                );
    }

    @Test
    void shouldResolveFileAndSynchronizeCatalog() {
        Path catalogFile =
                Path.of("TSGoods.trs");

        CatalogSyncReport expectedReport =
                new CatalogSyncReport(
                        2,
                        2,
                        0,
                        0
                );

        when(
                fileResolver.resolveCatalogFile()
        ).thenReturn(catalogFile);

        when(
                synchronizer.synchronize(catalogFile)
        ).thenReturn(expectedReport);

        CatalogSyncReport result =
                importService.importCatalog();

        assertEquals(expectedReport, result);

        verify(fileResolver).resolveCatalogFile();
        verify(synchronizer).synchronize(catalogFile);
    }

    @Test
    void shouldReleaseImportLockAfterFailure() {
        Path catalogFile =
                Path.of("TSGoods.trs");

        CatalogSyncReport expectedReport =
                new CatalogSyncReport(
                        1,
                        1,
                        0,
                        0
                );

        when(
                fileResolver.resolveCatalogFile()
        ).thenReturn(catalogFile);

        when(
                synchronizer.synchronize(catalogFile)
        )
                .thenThrow(new RuntimeException("Test error"))
                .thenReturn(expectedReport);

        assertThrows(
                RuntimeException.class,
                importService::importCatalog
        );

        CatalogSyncReport secondResult =
                importService.importCatalog();

        assertEquals(
                expectedReport,
                secondResult
        );
    }
    @Mock
    private TorgsoftCatalogImportHistoryStore historyStore;
}