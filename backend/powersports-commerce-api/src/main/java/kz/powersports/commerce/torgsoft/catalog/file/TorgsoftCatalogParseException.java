package kz.powersports.commerce.torgsoft.catalog.file;

public class TorgsoftCatalogParseException extends RuntimeException {

    public TorgsoftCatalogParseException(String message) {
        super(message);
    }

    public TorgsoftCatalogParseException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}