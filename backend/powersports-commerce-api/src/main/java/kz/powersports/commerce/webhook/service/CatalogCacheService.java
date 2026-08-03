package kz.powersports.commerce.webhook.service;

import kz.powersports.commerce.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CatalogCacheService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CatalogCacheService.class
            );

    private final CacheManager cacheManager;

    public CatalogCacheService(
            CacheManager cacheManager
    ) {
        this.cacheManager = cacheManager;
    }

    public void clearCatalogCaches() {
        clearCache(CacheConfig.PRODUCTS_CACHE);

        clearCache(
                CacheConfig.PRODUCT_BY_SLUG_CACHE
        );

        clearCache(CacheConfig.CATEGORIES_CACHE);

        log.info(
                "Кеш товаров и категорий очищен"
        );
    }

    private void clearCache(
            String cacheName
    ) {
        Cache cache =
                cacheManager.getCache(cacheName);

        if (cache == null) {
            log.warn(
                    "Кеш '{}' не найден",
                    cacheName
            );

            return;
        }

        cache.clear();

        log.info(
                "Кеш '{}' очищен",
                cacheName
        );
    }
}