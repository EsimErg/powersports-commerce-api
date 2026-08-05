package kz.powersports.commerce.torgsoft.catalog.file;

import kz.powersports.commerce.torgsoft.catalog.model.TorgsoftProduct;
import kz.powersports.commerce.torgsoft.catalog.port.TorgsoftCatalogReader;
import kz.powersports.commerce.torgsoft.config.TorgsoftCatalogFormatProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = "enabled",
        havingValue = "true"
)
public class DelimitedTorgsoftCatalogReader
        implements TorgsoftCatalogReader {

    private final TorgsoftCatalogFormatProperties properties;

    public DelimitedTorgsoftCatalogReader(
            TorgsoftCatalogFormatProperties properties
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "properties не должен быть null"
        );
    }

    @Override
    public List<TorgsoftProduct> read(Path catalogFile) {
        Objects.requireNonNull(
                catalogFile,
                "catalogFile не должен быть null"
        );

        try (
                BufferedReader reader = Files.newBufferedReader(
                        catalogFile,
                        properties.resolvedCharset()
                )
        ) {
            String headerLine = readFirstNonBlankLine(reader);

            if (headerLine == null) {
                throw new TorgsoftCatalogParseException(
                        "Файл каталога Torgsoft пуст: " + catalogFile
                );
            }

            List<String> headers = parseLine(
                    headerLine,
                    1
            );

            Map<String, Integer> columnIndexes =
                    buildColumnIndexes(headers);

            int goodIdIndex = requiredColumn(
                    columnIndexes,
                    properties.columns().goodId()
            );

            int skuIndex = requiredColumn(
                    columnIndexes,
                    properties.columns().sku()
            );

            int nameIndex = requiredColumn(
                    columnIndexes,
                    properties.columns().name()
            );

            int priceIndex = requiredColumn(
                    columnIndexes,
                    properties.columns().price()
            );

            int stockIndex = requiredColumn(
                    columnIndexes,
                    properties.columns().stock()
            );

            List<TorgsoftProduct> products =
                    new ArrayList<>();

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                List<String> fields = parseLine(
                        line,
                        lineNumber
                );

                try {
                    products.add(
                            new TorgsoftProduct(
                                    valueAt(
                                            fields,
                                            goodIdIndex,
                                            lineNumber
                                    ),
                                    nullableValueAt(
                                            fields,
                                            skuIndex
                                    ),
                                    valueAt(
                                            fields,
                                            nameIndex,
                                            lineNumber
                                    ),
                                    parseDecimal(
                                            valueAt(
                                                    fields,
                                                    priceIndex,
                                                    lineNumber
                                            ),
                                            "цена",
                                            lineNumber
                                    ),
                                    parseStock(
                                            nullableValueAt(
                                                    fields,
                                                    stockIndex
                                            ),
                                            lineNumber
                                    )
                            )
                    );

                } catch (RuntimeException exception) {
                    throw new TorgsoftCatalogParseException(
                            "Ошибка в строке каталога Torgsoft №"
                                    + lineNumber
                                    + ": "
                                    + exception.getMessage(),
                            exception
                    );
                }
            }

            return List.copyOf(products);

        } catch (IOException exception) {
            throw new TorgsoftCatalogParseException(
                    "Не удалось прочитать файл каталога Torgsoft: "
                            + catalogFile,
                    exception
            );
        }
    }

    private String readFirstNonBlankLine(
            BufferedReader reader
    ) throws IOException {
        String line;

        while ((line = reader.readLine()) != null) {
            if (!line.isBlank()) {
                return line;
            }
        }

        return null;
    }

    private Map<String, Integer> buildColumnIndexes(
            List<String> headers
    ) {
        Map<String, Integer> result = new HashMap<>();

        for (int index = 0; index < headers.size(); index++) {
            result.put(
                    normalizeHeader(headers.get(index)),
                    index
            );
        }

        return result;
    }

    private int requiredColumn(
            Map<String, Integer> columnIndexes,
            String columnName
    ) {
        Integer index = columnIndexes.get(
                normalizeHeader(columnName)
        );

        if (index == null) {
            throw new TorgsoftCatalogParseException(
                    "В файле Torgsoft отсутствует обязательная колонка: "
                            + columnName
            );
        }

        return index;
    }

    private String normalizeHeader(String value) {
        return value
                .replace("\uFEFF", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String valueAt(
            List<String> fields,
            int index,
            int lineNumber
    ) {
        String value = nullableValueAt(fields, index);

        if (value == null) {
            throw new IllegalArgumentException(
                    "пустое обязательное значение, строка "
                            + lineNumber
            );
        }

        return value;
    }

    private String nullableValueAt(
            List<String> fields,
            int index
    ) {
        if (index < 0 || index >= fields.size()) {
            return null;
        }

        String value = fields.get(index).trim();

        return value.isBlank() ? null : value;
    }

    private BigDecimal parseDecimal(
            String rawValue,
            String fieldName,
            int lineNumber
    ) {
        String normalizedValue = rawValue
                .replace("\u00A0", "")
                .replace(" ", "")
                .replace(",", ".");

        try {
            return new BigDecimal(normalizedValue);

        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "некорректное поле «"
                            + fieldName
                            + "» в строке "
                            + lineNumber
                            + ": "
                            + rawValue,
                    exception
            );
        }
    }

    private BigDecimal parseStock(
            String rawValue,
            int lineNumber
    ) {
        if (rawValue == null) {
            return BigDecimal.ZERO;
        }

        return parseDecimal(
                rawValue,
                "остаток",
                lineNumber
        );
    }

    private List<String> parseLine(
            String line,
            int lineNumber
    ) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean insideQuotes = false;
        char delimiter = properties.delimiterChar();

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);

            if (character == '"') {
                boolean escapedQuote =
                        insideQuotes
                                && index + 1 < line.length()
                                && line.charAt(index + 1) == '"';

                if (escapedQuote) {
                    current.append('"');
                    index++;
                } else {
                    insideQuotes = !insideQuotes;
                }

                continue;
            }

            if (character == delimiter && !insideQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
                continue;
            }

            current.append(character);
        }

        if (insideQuotes) {
            throw new TorgsoftCatalogParseException(
                    "Незакрытые кавычки в строке каталога №"
                            + lineNumber
            );
        }

        result.add(current.toString().trim());

        return result;
    }
}