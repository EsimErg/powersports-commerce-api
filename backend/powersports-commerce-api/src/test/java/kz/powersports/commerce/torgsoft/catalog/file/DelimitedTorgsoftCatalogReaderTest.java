package kz.powersports.commerce.torgsoft.catalog.file;

import kz.powersports.commerce.torgsoft.catalog.model.TorgsoftProduct;
import kz.powersports.commerce.torgsoft.config.TorgsoftCatalogFormatProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DelimitedTorgsoftCatalogReaderTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldReadProductsFromDelimitedCatalog() throws Exception {
        Path catalogFile =
                tempDirectory.resolve("TSGoods.trs");

        Files.writeString(
                catalogFile,
                """
                GoodID;Articul;Name;Price;Quantity
                GOOD-100;PS-100;Беговая дорожка;340000,00;5
                GOOD-200;PS-200;"Силовая станция; Pro";1250000;2
                """,
                Charset.forName("windows-1251")
        );

        DelimitedTorgsoftCatalogReader reader =
                new DelimitedTorgsoftCatalogReader(
                        createProperties()
                );

        List<TorgsoftProduct> products =
                reader.read(catalogFile);

        assertEquals(2, products.size());

        assertEquals(
                "GOOD-100",
                products.getFirst().goodId()
        );

        assertEquals(
                "340000.00",
                products.getFirst()
                        .price()
                        .toPlainString()
        );

        assertEquals(
                "Силовая станция; Pro",
                products.get(1).name()
        );
    }

    @Test
    void shouldRejectInvalidPrice() throws Exception {
        Path catalogFile =
                tempDirectory.resolve("TSGoods.trs");

        Files.writeString(
                catalogFile,
                """
                GoodID;Articul;Name;Price;Quantity
                GOOD-100;PS-100;Беговая дорожка;ошибка;5
                """,
                Charset.forName("windows-1251")
        );

        DelimitedTorgsoftCatalogReader reader =
                new DelimitedTorgsoftCatalogReader(
                        createProperties()
                );

        assertThrows(
                TorgsoftCatalogParseException.class,
                () -> reader.read(catalogFile)
        );
    }

    private TorgsoftCatalogFormatProperties createProperties() {
        return new TorgsoftCatalogFormatProperties(
                "windows-1251",
                ";",
                new TorgsoftCatalogFormatProperties.Columns(
                        "GoodID",
                        "Articul",
                        "Name",
                        "Price",
                        "Quantity"
                )
        );
    }
}