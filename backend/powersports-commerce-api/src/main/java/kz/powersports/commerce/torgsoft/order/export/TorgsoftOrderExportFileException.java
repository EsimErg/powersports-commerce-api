package kz.powersports.commerce.torgsoft.order.export;

public class TorgsoftOrderExportFileException
        extends RuntimeException {

    public TorgsoftOrderExportFileException(
            String message
    ) {
        super(message);
    }

    public TorgsoftOrderExportFileException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}