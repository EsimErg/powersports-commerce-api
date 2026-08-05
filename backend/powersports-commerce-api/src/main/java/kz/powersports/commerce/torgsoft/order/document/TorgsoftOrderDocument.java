package kz.powersports.commerce.torgsoft.order.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TorgsoftOrderDocument(
        Long wooCommerceOrderId,
        String orderNumber,
        LocalDateTime createdAt,
        String status,
        String currency,
        BigDecimal total,
        Customer customer,
        Address billingAddress,
        Address shippingAddress,
        String customerNote,
        List<Item> items
) {

    public TorgsoftOrderDocument {
        if (wooCommerceOrderId == null
                || wooCommerceOrderId <= 0) {
            throw new IllegalArgumentException(
                    "WooCommerce order ID должен быть положительным"
            );
        }

        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Номер заказа не должен быть пустым"
            );
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(
                    "Валюта заказа не должна быть пустой"
            );
        }

        if (total == null || total.signum() < 0) {
            throw new IllegalArgumentException(
                    "Сумма заказа не должна быть отрицательной"
            );
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Заказ должен содержать хотя бы один товар"
            );
        }

        orderNumber = orderNumber.trim();
        status = normalize(status);
        currency = currency.trim();
        customerNote = normalize(customerNote);
        items = List.copyOf(items);
    }

    public record Customer(
            String firstName,
            String lastName,
            String phone,
            String email
    ) {
        public Customer {
            firstName = normalize(firstName);
            lastName = normalize(lastName);
            phone = normalize(phone);
            email = normalize(email);
        }

        public String fullName() {
            return (firstName + " " + lastName).trim();
        }
    }

    public record Address(
            String city,
            String addressLine1,
            String addressLine2,
            String state,
            String postcode,
            String country
    ) {
        public Address {
            city = normalize(city);
            addressLine1 = normalize(addressLine1);
            addressLine2 = normalize(addressLine2);
            state = normalize(state);
            postcode = normalize(postcode);
            country = normalize(country);
        }

        public String fullAddress() {
            return String.join(
                    ", ",
                    List.of(
                                    addressLine1,
                                    addressLine2,
                                    city,
                                    state,
                                    postcode,
                                    country
                            )
                            .stream()
                            .filter(value -> !value.isBlank())
                            .toList()
            );
        }
    }

    public record Item(
            Long wooCommerceLineItemId,
            Long productId,
            Long variationId,
            String sku,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
        public Item {
            if (productId == null || productId <= 0) {
                throw new IllegalArgumentException(
                        "Product ID должен быть положительным"
                );
            }

            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Название товара не должно быть пустым"
                );
            }

            if (quantity <= 0) {
                throw new IllegalArgumentException(
                        "Количество товара должно быть положительным"
                );
            }

            if (lineTotal == null
                    || lineTotal.signum() < 0) {
                throw new IllegalArgumentException(
                        "Сумма позиции не должна быть отрицательной"
                );
            }

            sku = normalize(sku);
            name = name.trim();

            if (unitPrice == null) {
                unitPrice = BigDecimal.ZERO;
            }

            if (variationId == null) {
                variationId = 0L;
            }
        }
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim();
    }
}