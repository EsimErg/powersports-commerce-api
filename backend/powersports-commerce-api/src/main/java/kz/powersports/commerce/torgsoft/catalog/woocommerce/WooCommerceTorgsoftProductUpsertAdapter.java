package kz.powersports.commerce.torgsoft.catalog.woocommerce;

import kz.powersports.commerce.torgsoft.catalog.model.TorgsoftProduct;
import kz.powersports.commerce.torgsoft.catalog.sync.ProductSyncAction;
import kz.powersports.commerce.torgsoft.catalog.sync.TorgsoftProductUpsertPort;
import kz.powersports.commerce.torgsoft.mapping.TorgsoftProductMappingRepository;

import java.util.Objects;
import java.util.Optional;

public final class WooCommerceTorgsoftProductUpsertAdapter
        implements TorgsoftProductUpsertPort {

    private final TorgsoftProductMappingRepository mappingRepository;
    private final TorgsoftWooCommerceProductGateway productGateway;

    public WooCommerceTorgsoftProductUpsertAdapter(
            TorgsoftProductMappingRepository mappingRepository,
            TorgsoftWooCommerceProductGateway productGateway
    ) {
        this.mappingRepository = Objects.requireNonNull(
                mappingRepository,
                "mappingRepository не должен быть null"
        );

        this.productGateway = Objects.requireNonNull(
                productGateway,
                "productGateway не должен быть null"
        );
    }

    @Override
    public ProductSyncAction upsert(TorgsoftProduct product) {
        Objects.requireNonNull(
                product,
                "product не должен быть null"
        );

        WooCommerceProductSyncRequest request =
                WooCommerceProductSyncRequest.from(product);

        Optional<Long> existingProductId =
                mappingRepository.findWooCommerceProductId(
                        product.goodId()
                );

        if (existingProductId.isPresent()) {
            productGateway.update(
                    existingProductId.get(),
                    request
            );

            return ProductSyncAction.UPDATED;
        }

        WooCommerceProductSyncResult result =
                productGateway.create(request);

        mappingRepository.save(
                product.goodId(),
                result.productId()
        );

        return ProductSyncAction.CREATED;
    }
}