package kz.powersports.commerce.torgsoft.catalog.sync;

import kz.powersports.commerce.torgsoft.catalog.model.TorgsoftProduct;

public interface TorgsoftProductUpsertPort {

    ProductSyncAction upsert(TorgsoftProduct product);
}