package kz.powersports.commerce.webhook.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class WooCommerceWebhookSignatureVerifier {

    private static final String HMAC_ALGORITHM =
            "HmacSHA256";

    private final byte[] webhookSecret;

    public WooCommerceWebhookSignatureVerifier(
            @Value("${woocommerce.webhook-secret}")
            String webhookSecret
    ) {
        if (webhookSecret == null
                || webhookSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "WooCommerce webhook secret "
                            + "не должен быть пустым"
            );
        }

        this.webhookSecret =
                webhookSecret.getBytes(
                        StandardCharsets.UTF_8
                );
    }

    public boolean isValid(
            byte[] requestBody,
            String receivedSignature
    ) {
        if (requestBody == null
                || receivedSignature == null
                || receivedSignature.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance(
                    HMAC_ALGORITHM
            );

            SecretKeySpec secretKey =
                    new SecretKeySpec(
                            webhookSecret,
                            HMAC_ALGORITHM
                    );

            mac.init(secretKey);

            byte[] expectedSignature =
                    mac.doFinal(requestBody);

            byte[] actualSignature =
                    Base64.getDecoder()
                            .decode(
                                    receivedSignature.trim()
                            );

            return MessageDigest.isEqual(
                    expectedSignature,
                    actualSignature
            );

        } catch (
                GeneralSecurityException
                | IllegalArgumentException exception
        ) {
            return false;
        }
    }
}