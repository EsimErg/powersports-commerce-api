package kz.powersports.commerce.common.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String slug) {
        super("Товар с slug '" + slug + "' не найден");
    }
}