package kz.powersports.commerce.torgsoft.catalog.file;

import kz.powersports.commerce.torgsoft.config.TorgsoftProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = "enabled",
        havingValue = "true"
)
public class TorgsoftCatalogFileResolver {

    private final TorgsoftProperties properties;

    public TorgsoftCatalogFileResolver(
            TorgsoftProperties properties
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "properties не должен быть null"
        );
    }

    public Path resolveCatalogFile() {
        Path exchangeDirectory = properties
                .exchangeDirectory()
                .toAbsolutePath()
                .normalize();

        Path catalogFile = exchangeDirectory
                .resolve(properties.catalogFileName())
                .normalize();

        /*
         * Защита от значения вроде:
         * ../../secret-file.txt
         */
        if (!catalogFile.startsWith(exchangeDirectory)) {
            throw new TorgsoftCatalogFileException(
                    "Файл каталога находится за пределами "
                            + "разрешённой директории: "
                            + catalogFile
            );
        }

        if (!Files.exists(exchangeDirectory)) {
            throw new TorgsoftCatalogFileException(
                    "Директория обмена Torgsoft не найдена: "
                            + exchangeDirectory
            );
        }

        if (!Files.isDirectory(exchangeDirectory)) {
            throw new TorgsoftCatalogFileException(
                    "Путь обмена Torgsoft не является директорией: "
                            + exchangeDirectory
            );
        }

        if (!Files.exists(catalogFile)) {
            throw new TorgsoftCatalogFileException(
                    "Файл каталога Torgsoft не найден: "
                            + catalogFile
            );
        }

        if (!Files.isRegularFile(catalogFile)) {
            throw new TorgsoftCatalogFileException(
                    "Путь каталога Torgsoft не является файлом: "
                            + catalogFile
            );
        }

        if (!Files.isReadable(catalogFile)) {
            throw new TorgsoftCatalogFileException(
                    "Файл каталога Torgsoft недоступен для чтения: "
                            + catalogFile
            );
        }

        return catalogFile;
    }
}