package kz.powersports.commerce.torgsoft.order.document;

import kz.powersports.commerce.torgsoft.order.woocommerce
        .WooCommerceOrderExportResponse;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = "enabled",
        havingValue = "true"
)
public class TorgsoftOrderDocumentMapper {

    public TorgsoftOrderDocument map(
            WooCommerceOrderExportResponse order
    ) {
        Objects.requireNonNull(
                order,
                "order не должен быть null"
        );

        WooCommerceOrderExportResponse.Address billing =
                order.billing();

        WooCommerceOrderExportResponse.Address shipping =
                order.shipping();

        List<TorgsoftOrderDocument.Item> items =
                order.lineItems() == null
                        ? List.of()
                        : order.lineItems()
                        .stream()
                        .map(this::mapItem)
                        .toList();

        return new TorgsoftOrderDocument(
                order.id(),
                order.number(),
                parseDate(order.dateCreated()),
                order.status(),
                order.currency(),
                parseMoney(
                        order.total(),
                        "total заказа"
                ),
                mapCustomer(billing),
                mapAddress(billing),
                mapAddress(shipping),
                order.customerNote(),
                items
        );
    }

    private TorgsoftOrderDocument.Customer mapCustomer(
            WooCommerceOrderExportResponse.Address billing
    ) {
        if (billing == null) {
            return new TorgsoftOrderDocument.Customer(
                    "",
                    "",
                    "",
                    ""
            );
        }

        return new TorgsoftOrderDocument.Customer(
                billing.firstName(),
                billing.lastName(),
                billing.phone(),
                billing.email()
        );
    }

    private TorgsoftOrderDocument.Address mapAddress(
            WooCommerceOrderExportResponse.Address address
    ) {
        if (address == null) {
            return new TorgsoftOrderDocument.Address(
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            );
        }

        return new TorgsoftOrderDocument.Address(
                address.city(),
                address.address1(),
                address.address2(),
                address.state(),
                address.postcode(),
                address.country()
        );
    }

    private TorgsoftOrderDocument.Item mapItem(
            WooCommerceOrderExportResponse.LineItem item
    ) {
        int quantity =
                item.quantity() == null
                        ? 0
                        : item.quantity();

        BigDecimal lineTotal =
                parseMoney(
                        item.total(),
                        "сумма позиции"
                );

        BigDecimal unitPrice =
                parseUnitPrice(
                        item.price(),
                        lineTotal,
                        quantity
                );

        return new TorgsoftOrderDocument.Item(
                item.id(),
                item.productId(),
                item.variationId(),
                item.sku(),
                item.name(),
                quantity,
                unitPrice,
                lineTotal
        );
    }

    private BigDecimal parseUnitPrice(
            String rawPrice,
            BigDecimal lineTotal,
            int quantity
    ) {
        if (rawPrice != null && !rawPrice.isBlank()) {
            return parseMoney(
                    rawPrice,
                    "цена единицы"
            );
        }

        if (quantity <= 0) {
            return BigDecimal.ZERO;
        }

        return lineTotal.divide(
                BigDecimal.valueOf(quantity),
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal parseMoney(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(
                    value.trim()
                            .replace(" ", "")
                            .replace(",", ".")
            );

        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Некорректное денежное поле «"
                            + fieldName
                            + "»: "
                            + value,
                    exception
            );
        }
    }

    private LocalDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);

        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Некорректная дата создания заказа: "
                            + value,
                    exception
            );
        }
    }
}