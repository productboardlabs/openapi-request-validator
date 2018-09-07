package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import org.junit.Test;

import static com.atlassian.oai.validator.report.LevelResolverFactory.withAdditionalPropertiesIgnored;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadJsonRequest;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadRequest;

public class OpenAPIV3RequestValidationTest {

    private final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator.createFor("/oai/v3/api-users.yaml").build();

    @Test
    public void validate_withValidRequest_shouldSucceed() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withBody(loadJsonRequest("newuser-valid"))
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withMissingRequestBody_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withContentType("application/json")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.body.missing");
    }

    @Test
    public void validate_withInvalidJsonRequestBody_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withContentType("application/json")
                .withBody("not-valid")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.body.schema.invalidJson");
    }

    @Test
    public void validate_withInvalidRequestBody_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withContentType("application/json")
                .withBody("{}")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.body.schema.required");
    }

    @Test
    public void validate_withUnsupportedContentType_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withContentType("text/xml")
                .withBody(loadRequest("newuser-valid.xml"))
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.contentType.notAllowed");
    }

    @Test
    public void validate_withMissingContentType_shouldSucceed_withoutRequestBodyValidation() {
        // See https://tools.ietf.org/html/rfc7231#section-3.1.1.5
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withBody("not-json")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withInvalidPathParam_shouldFail() {
        final Request request = SimpleRequest.Builder
                .get("/users/id")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_withUnknownPath_shouldFail() {
        final Request request = SimpleRequest.Builder
                .get("/user/1")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.path.missing");
    }

    @Test
    public void validate_withUnsupportedOperation_shouldFail() {
        final Request request = SimpleRequest.Builder
                .patch("/users/1")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.operation.notAllowed");
    }

    @Test
    public void validate_withValidQueryParams_shouldPass() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withQueryParam("maxCount", "10")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withInvalidQueryParams_shouldFail() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withQueryParam("maxCount", "a")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_withArrayQueryParam_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withQueryParam("filter", "1", "2", "3")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withArrayQueryParam_shouldFail_whenInvalidAccordingToDefinedStyle() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withQueryParam("filter", "1,2,3")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_withArrayQueryParam_shouldFail_whenInvalidFormat() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withQueryParam("filter", "1", "bob", "3")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_withExtraQueryParams_shouldPass() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withQueryParam("foo", "bar")
                .withQueryParam("something", "else")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withOneOfComposition_shouldPass_whenValid() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/oneOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": \"foo\" }")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withOneOfComposition_shouldPass_whenValid_withNesting() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .put("/oneOf")
                .withContentType("application/json")
                .withBody("[{ \"stringField\": \"foo\" }, { \"intField\": 1 }]")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withOneOfComposition_shouldPass_whenValid_withNestedArrays() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .patch("/oneOf")
                .withContentType("application/json")
                .withBody("[{ \"stringField\": \"foo\" }, [{ \"intField\": 1 }, { \"boolField\": true }]]")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withOneOfComposition_fails_whenAdditionalPropertiesNotIgnored() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/oneOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": \"foo\" }")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.additionalProperties");
    }

    @Test
    public void validate_withOneOfComposition_shouldFail_whenInvalidAccordingToSchema() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/oneOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": 1 }")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.oneOf");
    }

    @Test
    public void validate_withOneOfComposition_shouldFail_whenInvalidSchema_withNesting() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .put("/oneOf")
                .withContentType("application/json")
                .withBody("[{ \"stringField\": \"foo\" }, { \"notAField\": 1 }]")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.oneOf");
    }

    @Test
    public void validate_withOneOfComposition_shouldFail_whenMatchingMultiple() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/oneOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": \"foo\", \"intField\": 1 }")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.oneOf");
    }

    @Test
    public void validate_withAllOfComposition_shouldPass_whenValid() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/allOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": \"foo\", \"intField\": 1, \"boolField\": false }")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withAllOfComposition_fails_whenAdditionalPropertiesNotIgnored() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/allOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": \"foo\", \"intField\": 1, \"boolField\": false }")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.additionalProperties");
    }

    @Test
    public void validate_withAllOfComposition_shouldFail_whenInvalidAccordingToSchema() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/allOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": \"foo\", \"intField\": false, \"boolField\": false }")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.allOf");
    }

    @Test
    public void validate_withAllOfComposition_shouldFail_whenDoesNotMatchAll() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/allOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": \"foo\", \"boolField\": false }")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.allOf");
    }

    @Test
    public void validate_withAnyOfComposition_shouldPass_whenValid() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/anyOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": \"foo\", \"intField\": 1 }")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withAnyOfComposition_shouldPass_whenValid_withNesting() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .put("/anyOf")
                .withContentType("application/json")
                .withBody("[{ \"stringField\": \"foo\", \"intField\": 1 }, { \"boolField\": false }]")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withAnyOfComposition_fails_whenAdditionalPropertiesNotIgnored() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/anyOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": \"foo\", \"intField\": 1 }")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.additionalProperties");
    }

    @Test
    public void validate_withAnyOfComposition_shouldFail_whenInvalidAccordingToSchema() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/anyOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": 1, \"intField\": false }")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.anyOf");
    }

    @Test
    public void validate_withAnyOfComposition_shouldPass_whenInvalidSchema_withNesting() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .put("/anyOf")
                .withContentType("application/json")
                .withBody("[{ \"stringField\": \"foo\", \"intField\": 1 }, 1]")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.anyOf");
    }

    @Test
    public void validate_withAnyOfComposition_shouldFail_whenMatchesNone() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-complex-composition.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/anyOf")
                .withContentType("application/json")
                .withBody("{ \"foo\": \"bar\" }")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.anyOf");
    }
}
