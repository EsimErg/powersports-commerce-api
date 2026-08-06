package kz.powersports.commerce.torgsoft.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "torgsoft.order-export")
public record TorgsoftOrderExportProperties(
        boolean enabled,
        Path outgoingDirectory,
        String filePrefix
) {

    public TorgsoftOrderExportProperties {
        if (outgoingDirectory == null) {
            outgoingDirectory =
                    Path.of("orders", "outgoing");
        }

        if (filePrefix == null || filePrefix.isBlank()) {
            filePrefix = "order-";
        }

        filePrefix = filePrefix.trim();

        /*
         * Не разрешаем слэши и конструкции ../../
         * внутри имени файла.
         */
        if (!filePrefix.matches(
                "[A-Za-z0-9._-]{1,50}"
        )) {
            throw new IllegalArgumentException(
                    "Недопустимый префикс файла заказа Torgsoft"
            );
        }
    }
}