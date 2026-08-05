package kz.powersports.commerce.torgsoft.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.Charset;

@ConfigurationProperties(prefix = "torgsoft.catalog")
public record TorgsoftCatalogFormatProperties(
        String charset,
        String delimiter,
        Columns columns
) {

    public TorgsoftCatalogFormatProperties {
        if (charset == null || charset.isBlank()) {
            charset = "windows-1251";
        }

        if (delimiter == null || delimiter.isEmpty()) {
            delimiter = ";";
        }

        if (delimiter.length() != 1) {
            throw new IllegalArgumentException(
                    "Разделитель каталога должен состоять из одного символа"
            );
        }

        if (columns == null) {
            columns = new Columns(
                    "GoodID",
                    "Articul",
                    "Name",
                    "Price",
                    "Quantity"
            );
        }
    }

    public Charset resolvedCharset() {
        return Charset.forName(charset);
    }

    public char delimiterChar() {
        return delimiter.charAt(0);
    }

    public record Columns(
            String goodId,
            String sku,
            String name,
            String price,
            String stock
    ) {

        public Columns {
            goodId = defaultValue(goodId, "GoodID");
            sku = defaultValue(sku, "Articul");
            name = defaultValue(name, "Name");
            price = defaultValue(price, "Price");
            stock = defaultValue(stock, "Quantity");
        }

        private static String defaultValue(
                String value,
                String defaultValue
        ) {
            return value == null || value.isBlank()
                    ? defaultValue
                    : value.trim();
        }
    }
}