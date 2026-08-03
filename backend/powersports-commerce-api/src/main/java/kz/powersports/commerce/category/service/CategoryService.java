package kz.powersports.commerce.category.service;

import kz.powersports.commerce.category.client.WooCommerceCategoryClient;
import kz.powersports.commerce.category.client.dto.WooCommerceCategory;
import kz.powersports.commerce.category.dto.CategoryResponse;
import kz.powersports.commerce.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final WooCommerceCategoryClient categoryClient;

    public CategoryService(
            WooCommerceCategoryClient categoryClient
    ) {
        this.categoryClient = categoryClient;
    }

    @Cacheable(
            cacheNames = CacheConfig.CATEGORIES_CACHE,
            key = "'all'"
    )
    public List<CategoryResponse> findAll() {
        return categoryClient
                .findAll()
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    private CategoryResponse toCategoryResponse(
            WooCommerceCategory category
    ) {
        String imageUrl =
                category.image() == null
                        ? null
                        : category.image().src();

        return new CategoryResponse(
                category.id(),
                category.name(),
                category.slug(),
                category.description(),
                category.parent(),
                category.count(),
                imageUrl
        );
    }
}