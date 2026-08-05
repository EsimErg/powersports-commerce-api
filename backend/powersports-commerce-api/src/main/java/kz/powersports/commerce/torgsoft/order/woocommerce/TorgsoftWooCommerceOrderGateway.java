package kz.powersports.commerce.torgsoft.order.woocommerce;

public interface TorgsoftWooCommerceOrderGateway {

    WooCommerceOrderExportResponse getOrder(
            Long wooCommerceOrderId
    );
}