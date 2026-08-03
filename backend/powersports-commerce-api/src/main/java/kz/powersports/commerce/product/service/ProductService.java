package kz.powersports.commerce.product.service;

import kz.powersports.commerce.common.exception.ProductNotFoundException;
import kz.powersports.commerce.config.CacheConfig;
import kz.powersports.commerce.product.client.WooCommerceProductClient;
import kz.powersports.commerce.product.client.dto.WooCommerceImage;
import kz.powersports.commerce.product.client.dto.WooCommercePrices;
import kz.powersports.commerce.product.client.dto.WooCommerceProduct;
import kz.powersports.commerce.product.client.dto.WooCommerceProductPage;
import kz.powersports.commerce.product.dto.ProductPageResponse;
import kz.powersports.commerce.product.dto.ProductResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final WooCommerceProductClient productClient;

    public ProductService(
            WooCommerceProductClient productClient
    ) {
        this.productClient = productClient;
    }

    @Cacheable(cacheNames = CacheConfig.PRODUCTS_CACHE)
    public ProductPageResponse findAll(
            int page,
            int size,
            String search,
            String category
    ) {
        WooCommerceProductPage wooPage =
                productClient.findAll(
                        page,
                        size,
                        search,
                        category
                );

        List<ProductResponse> content =
                wooPage.content()
                        .stream()
                        .map(this::toProductResponse)
                        .toList();

        boolean first = page == 0;

        boolean last =
                wooPage.totalPages() == 0
                        || page + 1 >= wooPage.totalPages();

        return new ProductPageResponse(
                content,
                page,
                size,
                wooPage.totalElements(),
                wooPage.totalPages(),
                first,
                last
        );
    }

    @Cacheable(
            cacheNames = CacheConfig.PRODUCT_BY_SLUG_CACHE,
            key = "#slug"
    )
    public ProductResponse findBySlug(
            String slug
    ) {
        WooCommerceProduct product =
                productClient.findBySlug(slug)
                        .orElseThrow(
                                () ->
                                        new ProductNotFoundException(
                                                slug
                                        )
                        );

        return toProductResponse(product);
    }

    private ProductResponse toProductResponse(
            WooCommerceProduct wooProduct
    ) {
        WooCommercePrices prices =
                wooProduct.prices();

        BigDecimal price = null;
        BigDecimal regularPrice = null;
        BigDecimal salePrice = null;
        String currency = null;

        if (prices != null) {
            price = convertPrice(
                    prices.price(),
                    prices.currencyMinorUnit()
            );

            regularPrice = convertPrice(
                    prices.regularPrice(),
                    prices.currencyMinorUnit()
            );

            salePrice = convertPrice(
                    prices.salePrice(),
                    prices.currencyMinorUnit()
            );

            currency = prices.currencyCode();
        }

        return new ProductResponse(
                wooProduct.id(),
                wooProduct.name(),
                wooProduct.slug(),
                wooProduct.sku(),
                wooProduct.shortDescription(),
                price,
                regularPrice,
                salePrice,
                currency,
                wooProduct.inStock(),
                wooProduct.onSale(),
                getFirstImageUrl(
                        wooProduct.images()
                )
        );
    }

    private BigDecimal convertPrice(
            String value,
            int minorUnit
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(value)
                    .movePointLeft(minorUnit);

        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String getFirstImageUrl(
            List<WooCommerceImage> images
    ) {
        if (images == null || images.isEmpty()) {
            return null;
        }

        WooCommerceImage firstImage =
                images.get(0);

        if (firstImage == null) {
            return null;
        }

        return firstImage.src();
    }
}