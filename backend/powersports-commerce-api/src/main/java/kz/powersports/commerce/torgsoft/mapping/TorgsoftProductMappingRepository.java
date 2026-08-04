package kz.powersports.commerce.torgsoft.mapping;

import java.util.Optional;

public interface TorgsoftProductMappingRepository {

    Optional<Long> findWooCommerceProductId(String goodId);

    void save(String goodId, Long wooCommerceProductId);

    void delete(String goodId);
}