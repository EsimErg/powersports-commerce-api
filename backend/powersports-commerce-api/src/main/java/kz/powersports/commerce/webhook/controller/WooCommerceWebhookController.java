package kz.powersports.commerce.webhook.controller;

import kz.powersports.commerce.webhook.security.WooCommerceWebhookSignatureVerifier;
import kz.powersports.commerce.webhook.service.CatalogCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "/api/v1/webhooks/woocommerce"
)
public class WooCommerceWebhookController {

    private static final Logger log =
            LoggerFactory.getLogger(
                    WooCommerceWebhookController.class
            );

    private final WooCommerceWebhookSignatureVerifier
            signatureVerifier;

    private final CatalogCacheService
            catalogCacheService;

    public WooCommerceWebhookController(
            WooCommerceWebhookSignatureVerifier
                    signatureVerifier,
            CatalogCacheService catalogCacheService
    ) {
        this.signatureVerifier =
                signatureVerifier;

        this.catalogCacheService =
                catalogCacheService;
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(

            @RequestHeader(
                    value =
                            "X-WC-Webhook-Signature",
                    required = false
            )
            String signature,

            @RequestHeader(
                    value = "X-WC-Webhook-Topic",
                    required = false
            )
            String topic,

            @RequestHeader(
                    value = "X-WC-Webhook-Resource",
                    required = false
            )
            String resource,

            @RequestBody
            byte[] requestBody
    ) {
        boolean valid =
                signatureVerifier.isValid(
                        requestBody,
                        signature
                );

        if (!valid) {
            log.warn(
                    "Получен webhook "
                            + "с неправильной подписью"
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        log.info(
                "Получен WooCommerce webhook. "
                        + "Topic: {}, resource: {}",
                topic,
                resource
        );

        catalogCacheService
                .clearCatalogCaches();

        return ResponseEntity
                .noContent()
                .build();
    }
}