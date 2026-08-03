package kz.powersports.commerce.cart.service;

import jakarta.servlet.http.HttpSession;
import kz.powersports.commerce.cart.client.WooCommerceCartClient;
import kz.powersports.commerce.cart.client.dto.WooCommerceCart;
import kz.powersports.commerce.cart.client.dto.WooCommerceCartItem;
import kz.powersports.commerce.cart.client.dto.WooCommerceCartItemTotals;
import kz.powersports.commerce.cart.client.dto.WooCommerceCartResult;
import kz.powersports.commerce.cart.client.dto.WooCommerceCartTotals;
import kz.powersports.commerce.cart.dto.AddCartItemRequest;
import kz.powersports.commerce.cart.dto.CartItemResponse;
import kz.powersports.commerce.cart.dto.CartResponse;
import kz.powersports.commerce.cart.dto.UpdateCartItemRequest;
import kz.powersports.commerce.product.client.dto.WooCommerceImage;
import kz.powersports.commerce.product.client.dto.WooCommercePrices;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private static final String CART_TOKEN_ATTRIBUTE =
            "woocommerceCartToken";

    private final WooCommerceCartClient cartClient;

    public CartService(
            WooCommerceCartClient cartClient
    ) {
        this.cartClient = cartClient;
    }

    public CartResponse getCart(
            HttpSession session
    ) {
        String cartToken =
                getCartToken(session);

        WooCommerceCartResult result;

        if (cartToken == null) {
            result = cartClient.createCart();
        } else {
            result = cartClient.getCart(
                    cartToken
            );
        }

        saveCartToken(
                session,
                result.cartToken()
        );

        return toCartResponse(
                result.cart()
        );
    }

    public CartResponse addItem(
            HttpSession session,
            AddCartItemRequest request
    ) {
        String cartToken =
                getOrCreateCartToken(session);

        WooCommerceCartResult result =
                cartClient.addItem(
                        cartToken,
                        request.productId(),
                        request.quantity()
                );

        saveCartToken(
                session,
                result.cartToken()
        );

        return toCartResponse(
                result.cart()
        );
    }

    public CartResponse updateItem(
            HttpSession session,
            String itemKey,
            UpdateCartItemRequest request
    ) {
        String cartToken =
                getOrCreateCartToken(session);

        WooCommerceCartResult result =
                cartClient.updateItem(
                        cartToken,
                        itemKey,
                        request.quantity()
                );

        saveCartToken(
                session,
                result.cartToken()
        );

        return toCartResponse(
                result.cart()
        );
    }

    public CartResponse removeItem(
            HttpSession session,
            String itemKey
    ) {
        String cartToken =
                getOrCreateCartToken(session);

        WooCommerceCartResult result =
                cartClient.removeItem(
                        cartToken,
                        itemKey
                );

        saveCartToken(
                session,
                result.cartToken()
        );

        return toCartResponse(
                result.cart()
        );
    }

    public CartResponse clearCart(
            HttpSession session
    ) {
        String cartToken =
                getOrCreateCartToken(session);

        WooCommerceCartResult result =
                cartClient.clearCart(
                        cartToken
                );

        saveCartToken(
                session,
                result.cartToken()
        );

        return toCartResponse(
                result.cart()
        );
    }

    private String getOrCreateCartToken(
            HttpSession session
    ) {
        String existingToken =
                getCartToken(session);

        if (existingToken != null) {
            return existingToken;
        }

        WooCommerceCartResult result =
                cartClient.createCart();

        saveCartToken(
                session,
                result.cartToken()
        );

        return result.cartToken();
    }

    private String getCartToken(
            HttpSession session
    ) {
        Object value =
                session.getAttribute(
                        CART_TOKEN_ATTRIBUTE
                );

        if (value instanceof String token
                && !token.isBlank()) {
            return token;
        }

        return null;
    }

    private void saveCartToken(
            HttpSession session,
            String cartToken
    ) {
        session.setAttribute(
                CART_TOKEN_ATTRIBUTE,
                cartToken
        );
    }

    private CartResponse toCartResponse(
            WooCommerceCart cart
    ) {
        List<CartItemResponse> items =
                cart.items() == null
                        ? List.of()
                        : cart.items()
                        .stream()
                        .map(
                                this::toCartItemResponse
                        )
                        .toList();

        WooCommerceCartTotals totals =
                cart.totals();

        if (totals == null) {
            return new CartResponse(
                    items,
                    cart.itemsCount(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    cart.needsPayment(),
                    cart.needsShipping()
            );
        }

        return new CartResponse(
                items,
                cart.itemsCount(),
                convertPrice(
                        totals.totalItems(),
                        totals.currencyMinorUnit()
                ),
                convertPrice(
                        totals.totalDiscount(),
                        totals.currencyMinorUnit()
                ),
                convertPrice(
                        totals.totalShipping(),
                        totals.currencyMinorUnit()
                ),
                convertPrice(
                        totals.totalPrice(),
                        totals.currencyMinorUnit()
                ),
                convertPrice(
                        totals.totalTax(),
                        totals.currencyMinorUnit()
                ),
                totals.currencyCode(),
                cart.needsPayment(),
                cart.needsShipping()
        );
    }

    private CartItemResponse toCartItemResponse(
            WooCommerceCartItem item
    ) {
        WooCommercePrices prices =
                item.prices();

        WooCommerceCartItemTotals totals =
                item.totals();

        String currency = null;
        BigDecimal unitPrice = null;
        BigDecimal lineSubtotal = null;
        BigDecimal lineTotal = null;

        if (prices != null) {
            currency = prices.currencyCode();

            unitPrice = convertPrice(
                    prices.price(),
                    prices.currencyMinorUnit()
            );
        }

        if (totals != null) {
            currency = totals.currencyCode();

            lineSubtotal = convertPrice(
                    totals.lineSubtotal(),
                    totals.currencyMinorUnit()
            );

            lineTotal = convertPrice(
                    totals.lineTotal(),
                    totals.currencyMinorUnit()
            );
        }

        return new CartItemResponse(
                item.key(),
                item.id(),
                item.name(),
                item.sku(),
                item.quantity(),
                unitPrice,
                lineSubtotal,
                lineTotal,
                currency,
                getFirstImageUrl(
                        item.images()
                )
        );
    }

    private BigDecimal convertPrice(
            String value,
            int minorUnit
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(value)
                    .movePointLeft(minorUnit);

        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String getFirstImageUrl(
            List<WooCommerceImage> images
    ) {
        if (images == null || images.isEmpty()) {
            return null;
        }

        WooCommerceImage image =
                images.get(0);

        return image == null
                ? null
                : image.src();
    }
    public void detachCart(
            HttpSession session
    ) {
        session.removeAttribute(
                CART_TOKEN_ATTRIBUTE
        );
    }
}