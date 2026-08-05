package kz.powersports.commerce.torgsoft.web;

import kz.powersports.commerce.torgsoft.config.TorgsoftManualImportProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "torgsoft.manual-import",
        name = "enabled",
        havingValue = "true"
)
public class TorgsoftAdminTokenVerifier {

    private final byte[] expectedToken;

    public TorgsoftAdminTokenVerifier(
            TorgsoftManualImportProperties properties
    ) {
        Objects.requireNonNull(
                properties,
                "properties не должен быть null"
        );

        this.expectedToken = properties
                .token()
                .getBytes(StandardCharsets.UTF_8);
    }

    public void verify(String suppliedToken) {
        if (suppliedToken == null
                || suppliedToken.isBlank()) {
            throw new InvalidTorgsoftAdminTokenException();
        }

        byte[] suppliedBytes = suppliedToken
                .trim()
                .getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(
                expectedToken,
                suppliedBytes
        )) {
            throw new InvalidTorgsoftAdminTokenException();
        }
    }
}