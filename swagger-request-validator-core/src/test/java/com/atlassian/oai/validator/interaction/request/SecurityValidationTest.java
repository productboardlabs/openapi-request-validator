package com.atlassian.oai.validator.interaction.request;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

/**
 * Tests for behavior of security validation
 * <p>
 * See https://swagger.io/docs/specification/authentication/
 */
public class SecurityValidationTest {

    private final OpenApiInteractionValidator validator =
            OpenApiInteractionValidator.createFor("/oai/v3/api-with-securityschemes.yaml").build();

    @Test
    public void basicAuth_shouldFail_whenMissing() {
        final Request request = SimpleRequest.Builder
                .get("/secured/basic")
                .withContentType("application/json")
                .build();

        assertFail(validator.validateRequest(request), "validation.request.security.missing");
    }

    @Test
    public void basicAuth_shouldFail_whenNotBasicAuth() {
        final Request request = SimpleRequest.Builder
                .get("/secured/basic")
                .withAuthorization("Bearer foo")
                .withContentType("application/json")
                .build();

        assertFail(validator.validateRequest(request), "validation.request.security.invalid");
    }

    @Test
    public void basicAuth_shouldPass_whenBasicAuthProvided() {
        final Request request = SimpleRequest.Builder
                .get("/secured/basic")
                .withAuthorization("Basic foo")
                .withContentType("application/json")
                .build();

        assertPass(validator.validateRequest(request));
    }

    @Test
    public void apiKeyAuth_shouldFail_whenMissing_inHeader() {
        final Request request = SimpleRequest.Builder
                .get("/secured/apikey/header")
                .withContentType("application/json")
                .build();

        assertFail(validator.validateRequest(request), "validation.request.security.missing");
    }

    @Test
    public void apiKeyAuth_shouldPass_whenApiKeyProvided_inHeader() {
        final Request request = SimpleRequest.Builder
                .get("/secured/apikey/header")
                .withHeader("X-Api-Key", "some-key")
                .withContentType("application/json")
                .build();

        assertPass(validator.validateRequest(request));
    }

    @Test
    public void apiKeyAuth_shouldFail_whenEmptyKey_inHeader() {
        final Request request = SimpleRequest.Builder
                .get("/secured/apikey/header")
                .withHeader("X-Api-Key")
                .withContentType("application/json")
                .build();

        assertFail(validator.validateRequest(request), "validation.request.security.missing");
    }

    @Test
    public void apiKeyAuth_shouldFail_whenMissing_inQuery() {
        final Request request = SimpleRequest.Builder
                .get("/secured/apikey/query")
                .withContentType("application/json")
                .build();

        assertFail(validator.validateRequest(request), "validation.request.security.missing");
    }

    @Test
    public void apiKeyAuth_shouldPass_whenApiKeyProvided_inQuery() {
        final Request request = SimpleRequest.Builder
                .get("/secured/apikey/query")
                .withQueryParam("apiKey", "some-key")
                .withContentType("application/json")
                .build();

        assertPass(validator.validateRequest(request));
    }

    @Test
    public void apiKeyAuth_shouldFail_whenEmptyKey_inQuery() {
        final Request request = SimpleRequest.Builder
                .get("/secured/apikey/query")
                .withQueryParam("apiKey")
                .withContentType("application/json")
                .build();

        assertFail(validator.validateRequest(request), "validation.request.security.missing");
    }

    @Test
    public void andCombinedAuth_shouldPass_whenAllProvided() {
        final Request request = SimpleRequest.Builder
                .get("/secured/combined/and")
                .withQueryParam("apiKey", "some-key")
                .withAuthorization("Basic foo")
                .withContentType("application/json")
                .build();

        assertPass(validator.validateRequest(request));
    }

    @Test
    public void andCombinedAuth_shouldFail_whenOneMissing() {
        final Request request = SimpleRequest.Builder
                .get("/secured/combined/and")
                .withQueryParam("apiKey", "some-key")
                .withContentType("application/json")
                .build();

        assertFail(validator.validateRequest(request), "validation.request.security.missing");
    }

    @Test
    public void orCombinedAuth_shouldPass_whenAllProvided() {
        final Request request = SimpleRequest.Builder
                .get("/secured/combined/or")
                .withQueryParam("apiKey", "some-key")
                .withAuthorization("Basic foo")
                .withContentType("application/json")
                .build();

        assertPass(validator.validateRequest(request));
    }

    @Test
    public void orCombinedAuth_shouldPass_whenOneMissing() {
        final Request request = SimpleRequest.Builder
                .get("/secured/combined/or")
                .withQueryParam("apiKey", "some-key")
                .withContentType("application/json")
                .build();

        assertPass(validator.validateRequest(request));
    }

    @Test
    public void shouldPass_whenUnsupportedSecurityScheme() {
        final Request request = SimpleRequest.Builder
                .get("/secured/unsupported")
                .withContentType("application/json")
                .build();

        assertPass(validator.validateRequest(request));
    }

    @Test
    public void ignoresMissingSecuritySchemes() {
        final OpenApiInteractionValidator validator =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-with-missing-securityschemes.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/secured")
                .withContentType("application/json")
                .build();

        assertPass(validator.validateRequest(request));
    }
}
