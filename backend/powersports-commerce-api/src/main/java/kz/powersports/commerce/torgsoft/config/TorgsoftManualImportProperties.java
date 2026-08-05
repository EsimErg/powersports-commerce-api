package kz.powersports.commerce.torgsoft.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "torgsoft.manual-import")
public record TorgsoftManualImportProperties(
        boolean enabled,
        String token
) {

    public TorgsoftManualImportProperties {
        token = token == null
                ? ""
                : token.trim();

        if (enabled && token.length() < 24) {
            throw new IllegalArgumentException(
                    "TORGSOFT_MANUAL_IMPORT_TOKEN должен содержать "
                            + "не менее 24 символов"
            );
        }
    }
}