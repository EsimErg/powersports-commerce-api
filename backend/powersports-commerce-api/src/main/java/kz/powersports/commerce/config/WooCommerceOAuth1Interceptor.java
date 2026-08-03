package kz.powersports.commerce.config;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class WooCommerceOAuth1Interceptor
        implements ClientHttpRequestInterceptor {

    private static final String SIGNATURE_METHOD =
            "HMAC-SHA256";

    private static final String MAC_ALGORITHM =
            "HmacSHA256";

    private final String consumerKey;
    private final String consumerSecret;

    public WooCommerceOAuth1Interceptor(
            String consumerKey,
            String consumerSecret
    ) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {

        URI signedUri = createSignedUri(
                request.getMethod(),
                request.getURI()
        );

        HttpRequest signedRequest =
                new HttpRequestWrapper(request) {

                    @Override
                    public URI getURI() {
                        return signedUri;
                    }
                };

        return execution.execute(
                signedRequest,
                body
        );
    }

    private URI createSignedUri(
            HttpMethod method,
            URI originalUri
    ) {
        String nonce = UUID.randomUUID()
                .toString()
                .replace("-", "");

        String timestamp = String.valueOf(
                Instant.now().getEpochSecond()
        );

        List<OAuthParameter> originalParameters =
                parseQueryParameters(
                        originalUri.getRawQuery()
                );

        List<OAuthParameter> signatureParameters =
                new ArrayList<>(originalParameters);

        signatureParameters.add(
                new OAuthParameter(
                        "oauth_consumer_key",
                        consumerKey
                )
        );

        signatureParameters.add(
                new OAuthParameter(
                        "oauth_nonce",
                        nonce
                )
        );

        signatureParameters.add(
                new OAuthParameter(
                        "oauth_signature_method",
                        SIGNATURE_METHOD
                )
        );

        signatureParameters.add(
                new OAuthParameter(
                        "oauth_timestamp",
                        timestamp
                )
        );

        String normalizedParameters =
                normalizeParameters(
                        signatureParameters
                );

        String baseUrl =
                buildBaseUrl(originalUri);

        String signatureBaseString =
                method.name()
                        + "&"
                        + percentEncode(baseUrl)
                        + "&"
                        + percentEncode(
                        normalizedParameters
                );

        String signature =
                calculateSignature(
                        signatureBaseString
                );

        List<OAuthParameter> finalParameters =
                new ArrayList<>(
                        originalParameters
                );

        finalParameters.add(
                new OAuthParameter(
                        "oauth_consumer_key",
                        consumerKey
                )
        );

        finalParameters.add(
                new OAuthParameter(
                        "oauth_nonce",
                        nonce
                )
        );

        finalParameters.add(
                new OAuthParameter(
                        "oauth_signature_method",
                        SIGNATURE_METHOD
                )
        );

        finalParameters.add(
                new OAuthParameter(
                        "oauth_timestamp",
                        timestamp
                )
        );

        finalParameters.add(
                new OAuthParameter(
                        "oauth_signature",
                        signature
                )
        );

        String query = finalParameters.stream()
                .map(parameter ->
                        percentEncode(parameter.name())
                                + "="
                                + percentEncode(
                                parameter.value()
                        )
                )
                .reduce(
                        (left, right) ->
                                left + "&" + right
                )
                .orElse("");

        return URI.create(
                baseUrl + "?" + query
        );
    }

    private String normalizeParameters(
            List<OAuthParameter> parameters
    ) {
        return parameters.stream()
                .map(parameter ->
                        new EncodedParameter(
                                percentEncode(
                                        parameter.name()
                                ),
                                percentEncode(
                                        parameter.value()
                                )
                        )
                )
                .sorted(
                        Comparator
                                .comparing(
                                        EncodedParameter::name
                                )
                                .thenComparing(
                                        EncodedParameter::value
                                )
                )
                .map(parameter ->
                        parameter.name()
                                + "="
                                + parameter.value()
                )
                .reduce(
                        (left, right) ->
                                left + "&" + right
                )
                .orElse("");
    }

    private String calculateSignature(
            String signatureBaseString
    ) {
        try {
            String signingKey =
                    percentEncode(consumerSecret)
                            + "&";

            Mac mac = Mac.getInstance(
                    MAC_ALGORITHM
            );

            SecretKeySpec key =
                    new SecretKeySpec(
                            signingKey.getBytes(
                                    StandardCharsets.UTF_8
                            ),
                            MAC_ALGORITHM
                    );

            mac.init(key);

            byte[] signatureBytes =
                    mac.doFinal(
                            signatureBaseString.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return Base64.getEncoder()
                    .encodeToString(
                            signatureBytes
                    );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Не удалось подписать запрос WooCommerce",
                    exception
            );
        }
    }

    private List<OAuthParameter> parseQueryParameters(
            String rawQuery
    ) {
        List<OAuthParameter> parameters =
                new ArrayList<>();

        if (rawQuery == null
                || rawQuery.isBlank()) {
            return parameters;
        }

        for (String part : rawQuery.split("&")) {
            String[] pair =
                    part.split("=", 2);

            String name =
                    decode(pair[0]);

            String value =
                    pair.length == 2
                            ? decode(pair[1])
                            : "";

            parameters.add(
                    new OAuthParameter(
                            name,
                            value
                    )
            );
        }

        return parameters;
    }

    private String buildBaseUrl(
            URI uri
    ) {
        String path = uri.getRawPath();

        if (path == null || path.isBlank()) {
            path = "/";
        }

        return uri.getScheme()
                + "://"
                + uri.getRawAuthority()
                + path;
    }

    private String percentEncode(
            String value
    ) {
        return URLEncoder
                .encode(
                        value == null ? "" : value,
                        StandardCharsets.UTF_8
                )
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    private String decode(
            String value
    ) {
        return URLDecoder.decode(
                value,
                StandardCharsets.UTF_8
        );
    }

    private record OAuthParameter(
            String name,
            String value
    ) {
    }

    private record EncodedParameter(
            String name,
            String value
    ) {
    }
}