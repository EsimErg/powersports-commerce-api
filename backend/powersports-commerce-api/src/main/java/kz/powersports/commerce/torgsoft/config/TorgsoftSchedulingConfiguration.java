package kz.powersports.commerce.torgsoft.config;

import kz.powersports.commerce.torgsoft.catalog.sync
        .TorgsoftCatalogImportService;
import kz.powersports.commerce.torgsoft.catalog.sync
        .TorgsoftCatalogScheduler;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = "enabled",
        havingValue = "true"
)
public class TorgsoftSchedulingConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "torgsoft.scheduler",
            name = "enabled",
            havingValue = "true"
    )
    public TorgsoftCatalogScheduler torgsoftCatalogScheduler(
            TorgsoftCatalogImportService importService
    ) {
        return new TorgsoftCatalogScheduler(
                importService
        );
    }
}