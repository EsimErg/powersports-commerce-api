package kz.powersports.commerce.torgsoft.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Locale;

@ConfigurationProperties(prefix = "torgsoft")
public record TorgsoftProperties(
        boolean enabled,
        Path exchangeDirectory,
        String catalogFileName,
        OrderFormat orderFormat,
        ProductStatus productStatus
) {

    public TorgsoftProperties {
        if (exchangeDirectory == null) {
            exchangeDirectory = Path.of("./data/torgsoft");
        }

        if (catalogFileName == null || catalogFileName.isBlank()) {
            catalogFileName = "TSGoods.trs";
        }

        if (orderFormat == null) {
            orderFormat = OrderFormat.JSON;
        }

        if (productStatus == null) {
            productStatus = ProductStatus.DRAFT;
        }
    }

    public enum OrderFormat {
        JSON,
        XML,
        SAL
    }

    public enum ProductStatus {
        DRAFT,
        PUBLISH;

        public String apiValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}