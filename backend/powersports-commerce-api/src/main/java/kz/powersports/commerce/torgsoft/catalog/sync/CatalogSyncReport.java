package kz.powersports.commerce.torgsoft.catalog.sync;

public record CatalogSyncReport(
        int total,
        int created,
        int updated,
        int failed
) {
}