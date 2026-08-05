package kz.powersports.commerce.torgsoft.catalog.file;

import kz.powersports.commerce.torgsoft.config.TorgsoftProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TorgsoftCatalogFileResolverTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldResolveExistingCatalogFile() throws Exception {
        Path catalogFile = Files.createFile(
                tempDirectory.resolve("TSGoods.trs")
        );

        TorgsoftCatalogFileResolver resolver =
                new TorgsoftCatalogFileResolver(
                        createProperties(
                                tempDirectory,
                                "TSGoods.trs"
                        )
                );

        Path result = resolver.resolveCatalogFile();

        assertEquals(
                catalogFile.toAbsolutePath().normalize(),
                result
        );
    }

    @Test
    void shouldRejectMissingCatalogFile() {
        TorgsoftCatalogFileResolver resolver =
                new TorgsoftCatalogFileResolver(
                        createProperties(
                                tempDirectory,
                                "missing.trs"
                        )
                );

        assertThrows(
                TorgsoftCatalogFileException.class,
                resolver::resolveCatalogFile
        );
    }

    @Test
    void shouldRejectFileOutsideExchangeDirectory() {
        TorgsoftCatalogFileResolver resolver =
                new TorgsoftCatalogFileResolver(
                        createProperties(
                                tempDirectory,
                                "../secret.txt"
                        )
                );

        assertThrows(
                TorgsoftCatalogFileException.class,
                resolver::resolveCatalogFile
        );
    }

    private TorgsoftProperties createProperties(
            Path exchangeDirectory,
            String catalogFileName
    ) {
        return new TorgsoftProperties(
                true,
                exchangeDirectory,
                catalogFileName,
                TorgsoftProperties.OrderFormat.JSON,
                TorgsoftProperties.ProductStatus.DRAFT
        );
    }
}