package kz.powersports.commerce.torgsoft.catalog.history;

import java.util.List;

public interface TorgsoftCatalogImportHistoryStore {

    void save(TorgsoftCatalogImportHistoryEntry entry);

    List<TorgsoftCatalogImportHistoryEntry> findRecent(int limit);
}