package kz.powersports.commerce.common.exception;

public class WooCommerceApiException extends RuntimeException {

    private final int upstreamStatus;

    public WooCommerceApiException(
            int upstreamStatus,
            String responseBody,
            Throwable cause
    ) {
        super(
                "WooCommerce вернул ошибку со статусом "
                        + upstreamStatus
                        + ". Ответ: "
                        + responseBody,
                cause
        );

        this.upstreamStatus = upstreamStatus;
    }

    public int getUpstreamStatus() {
        return upstreamStatus;
    }
}