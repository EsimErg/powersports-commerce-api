package kz.powersports.commerce.torgsoft.catalog.woocommerce;

import kz.powersports.commerce.torgsoft.catalog.model.TorgsoftProduct;
import kz.powersports.commerce.torgsoft.catalog.sync.ProductSyncAction;
import kz.powersports.commerce.torgsoft.mapping.TorgsoftProductMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WooCommerceTorgsoftProductUpsertAdapterTest {

    @Mock
    private TorgsoftProductMappingRepository mappingRepository;

    @Mock
    private TorgsoftWooCommerceProductGateway productGateway;

    private WooCommerceTorgsoftProductUpsertAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new WooCommerceTorgsoftProductUpsertAdapter(
                mappingRepository,
                productGateway
        );
    }

    @Test
    void shouldCreateProductAndSaveMappingWhenMappingDoesNotExist() {
        TorgsoftProduct product = createProduct();

        when(
                mappingRepository.findWooCommerceProductId("GOOD-100")
        ).thenReturn(Optional.empty());

        when(
                productGateway.findProductIdBySku("PS-100")
        ).thenReturn(Optional.empty());

        when(
                productGateway.create(any())
        ).thenReturn(
                new WooCommerceProductSyncResult(77L)
        );

        ProductSyncAction result = adapter.upsert(product);

        assertEquals(ProductSyncAction.CREATED, result);

        verify(productGateway).findProductIdBySku("PS-100");
        verify(productGateway).create(any());

        verify(mappingRepository).save(
                "GOOD-100",
                77L
        );
    }

    @Test
    void shouldUpdateProductWhenMappingAlreadyExists() {
        TorgsoftProduct product = createProduct();

        when(
                mappingRepository.findWooCommerceProductId("GOOD-100")
        ).thenReturn(Optional.of(77L));

        ProductSyncAction result = adapter.upsert(product);

        assertEquals(ProductSyncAction.UPDATED, result);

        verify(productGateway).update(
                77L,
                WooCommerceProductSyncRequest.from(product)
        );

        verify(productGateway, never()).create(any());
        verify(mappingRepository, never()).save(any(), any());
    }

    private TorgsoftProduct createProduct() {
        return new TorgsoftProduct(
                "GOOD-100",
                "PS-100",
                "Беговая дорожка PowerRun X1",
                new BigDecimal("340000.00"),
                new BigDecimal("5")
        );
    }
    @Test
    void shouldRestoreMappingAndUpdateWhenProductExistsBySku() {
        TorgsoftProduct product = createProduct();

        when(
                mappingRepository.findWooCommerceProductId("GOOD-100")
        ).thenReturn(Optional.empty());

        when(
                productGateway.findProductIdBySku("PS-100")
        ).thenReturn(Optional.of(77L));

        ProductSyncAction result =
                adapter.upsert(product);

        assertEquals(
                ProductSyncAction.UPDATED,
                result
        );

        verify(mappingRepository).save(
                "GOOD-100",
                77L
        );

        verify(productGateway).update(
                77L,
                WooCommerceProductSyncRequest.from(product)
        );

        verify(productGateway, never()).create(any());
    }
}