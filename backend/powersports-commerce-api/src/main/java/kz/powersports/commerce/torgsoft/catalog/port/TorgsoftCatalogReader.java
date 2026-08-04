package kz.powersports.commerce.torgsoft.catalog.port;

import kz.powersports.commerce.torgsoft.catalog.model.TorgsoftProduct;

import java.nio.file.Path;
import java.util.List;

public interface TorgsoftCatalogReader {

    List<TorgsoftProduct> read(Path catalogFile);
}