package kz.powersports.commerce.torgsoft.catalog.sync;

public class TorgsoftCatalogImportAlreadyRunningException
        extends RuntimeException {

    public TorgsoftCatalogImportAlreadyRunningException() {
        super("Импорт каталога Torgsoft уже выполняется");
    }
}