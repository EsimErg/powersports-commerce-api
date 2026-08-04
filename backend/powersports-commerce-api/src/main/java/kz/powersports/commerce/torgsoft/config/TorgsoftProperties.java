package kz.powersports.commerce.torgsoft.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "torgsoft")
public record TorgsoftProperties(
        boolean enabled,
        Path exchangeDirectory,
        String catalogFileName,
        OrderFormat orderFormat
) {

    public enum OrderFormat {
        JSON,
        XML,
        SAL
    }
}