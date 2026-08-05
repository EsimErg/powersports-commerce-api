package kz.powersports.commerce.torgsoft.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        TorgsoftProperties.class,
        TorgsoftCatalogFormatProperties.class,
        TorgsoftManualImportProperties.class
})
public class TorgsoftConfiguration {
}