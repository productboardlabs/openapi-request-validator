package com.atlassian.oai.validator;

import com.atlassian.oai.validator.interaction.request.CustomRequestValidator;
import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;

import static com.atlassian.oai.validator.report.LevelResolverFactory.withAdditionalPropertiesIgnored;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadJsonRequest;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadRequest;
import static com.google.common.collect.ImmutableMap.of;
import static io.swagger.v3.core.util.Json.pretty;

public class OpenAPIV3RequestValidationTest {

    private final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-users.yaml").build();

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
    public void validate_withInvalidNotInt32QueryParams_shouldFail() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withQueryParam("maxCount", "2147483648")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.schema.format.int32");
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
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/oneOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": \"foo\" }")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withOneOfComposition_shouldFail_whenInvalidAccordingToSchema() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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
    public void validate_withAllOfComposition_passes_whenAdditionalPropertiesNotIgnored() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
                        .withResolveCombinators(true)
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/allOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": \"foo\", \"intField\": 1, \"boolField\": false }")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withAllOfComposition_passes_whenResolveCombinatorsOptionUsed() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
                        .withResolveCombinators(true)
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/allOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": \"foo\", \"intField\": 1, \"boolField\": false }")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withAllOfComposition_shouldFail_whenInvalidAccordingToSchema() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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
    public void validate_withAnyOfComposition_passes_whenAdditionalPropertiesNotIgnored() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/anyOf")
                .withContentType("application/json")
                .withBody("{ \"stringField\": \"foo\" }")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withAnyOfComposition_shouldFail_whenInvalidAccordingToSchema() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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
                        .createForSpecificationUrl("/oai/v3/api-complex-composition.yaml")
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

    @Test
    public void validate_withOneOfDiscriminators_shouldPass_whenValid() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-discriminator.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/oneOf")
                .withContentType("application/json")
                .withBody("{ \"type\": \"Item1\", \"intField\": 1 }")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withOneOfDiscriminatorsWithMapping_shouldPass_whenValid() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createFor("/oai/v3/api-discriminator.yaml")
                        .withLevelResolver(withAdditionalPropertiesIgnored())
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/oneOfWithMapping")
                .withContentType("application/json")
                .withBody("{ \"type\": \"IntItem\", \"intField\": 1 }")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withFormData_shouldPass_whenValid() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-formdata.yaml").build();

        final Request request = SimpleRequest.Builder
                .post("/formdata")
                .withContentType("application/x-www-form-urlencoded")
                .withBody("name=John%20Smith&email=john%40example.com&age=27")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withFormData_shouldFail_whenInvalidSchema() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-formdata.yaml").build();

        final Request request = SimpleRequest.Builder
                .post("/formdata")
                .withContentType("application/x-www-form-urlencoded")
                .withBody("name=John%20Smith&email=john%40example.com&age=-27")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.body.schema.minimum");
    }

    @Test
    public void validate_withFormData_shouldFail_whenMissingRequiredField() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-formdata.yaml").build();

        final Request request = SimpleRequest.Builder
                .post("/formdata")
                .withContentType("application/x-www-form-urlencoded")
                .withBody("name=John%20Smith&email=john%40example.com&sage=27")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.body.schema.required");
    }

    @Test
    public void validate_withNullablePrimitive_shouldPass_whenNullProvided() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-nullable.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/nullablePrimitive")
                .withContentType("application/json")
                .withBody("null")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withNullablePrimitive_shouldPass_whenValueProvided() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-nullable.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/nullablePrimitive")
                .withContentType("application/json")
                .withBody("1")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withRequiredNullableProperty_shouldPass_whenNullProvided() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-nullable.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/nullableProperty")
                .withContentType("application/json")
                .withBody("{ \"name\": null }")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withRequiredNullableProperty_shouldPass_whenValueProvided() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-nullable.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/nullableProperty")
                .withContentType("application/json")
                .withBody("{ \"name\": \"foo\" }")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withRequiredNullableProperty_shouldFail_whenMissing() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-nullable.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/nullableProperty")
                .withContentType("application/json")
                .withBody("{ }")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.required");
    }

    @Test
    public void validate_withNullableArrayItem_shouldPass_whenNullProvided() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-nullable.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/nullableArrayItem")
                .withContentType("application/json")
                .withBody("[null]")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withNullableArrayItem_shouldPass_whenValueProvided() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-nullable.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/nullableArrayItem")
                .withContentType("application/json")
                .withBody("[\"foo\"]")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withNullableArrayItem_shouldFail_whenNotProvided() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-nullable.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/nullableArrayItem")
                .withContentType("application/json")
                .withBody("[]")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.minItems");
    }

    @Test
    public void validate_withReferencedRequestBody_shouldPass_whenValidBody() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-referenced-requestbody.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/test")
                .withContentType("application/json")
                .withBody("{\"intField\": 1}")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withReferencedRequestBody_shouldFail_whenInvalidBody() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v3/api-referenced-requestbody.yaml")
                        .build();

        final Request request = SimpleRequest.Builder
                .post("/test")
                .withContentType("application/json")
                .withBody("{\"intField\": 1.0}")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.type");
    }

    @Test
    public void validate_withCustomValidation_shouldPass() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v3/api-users.yaml")
                .withCustomRequestValidation(new TestValidator())
                .build();

        final Request request = SimpleRequest.Builder
                .get("/extensions")
                .withHeader("Extension", "true")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withCustomValidation_shouldFail() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v3/api-users.yaml")
                .withCustomRequestValidation(new TestValidator())
                .build();

        final Request request = SimpleRequest.Builder
                .get("/extensions")
                .withHeader("Extension", "false")
                .build();

        assertFail(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withPatterns_shouldPass_whenValid() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v3/api-with-patterns.yaml")
                .build();

        final Request request = SimpleRequest.Builder
                .post("/pattern")
                .withContentType("application/json")
                .withBody("{\"patternInline\": \"bbbbbba\", \"patternByRef\": \"aaaaaabbbbb\"}")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withPatterns_shouldFail_whenInvalidInline() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v3/api-with-patterns.yaml")
                .build();

        final Request request = SimpleRequest.Builder
                .post("/pattern")
                .withContentType("application/json")
                .withBody("{\"patternInline\": \"aaa\", \"patternByRef\": \"abbbbb\"}")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.body.schema.pattern");
    }

    @Test
    public void validate_withPatterns_shouldFail_whenInvalidByRef() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v3/api-with-patterns.yaml")
                .build();

        final Request request = SimpleRequest.Builder
                .post("/pattern")
                .withContentType("application/json")
                .withBody("{\"patternInline\": \"baaa\", \"patternByRef\": \"bbbbb\"}")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.body.schema.pattern");
    }

    @Test
    public void validate_withNonRequired_minLength_shouldPass() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v3/api-with-minlength-properties.yaml")
                .build();

        final Request request = SimpleRequest.Builder
                .post("/minLengthProperties")
                .withContentType("application/json")
                // Body missing `nonRequiredField` that also has a minLength: 1
                .withBody("{\"requiredField\": \"foo\"}")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withExamples_shouldPass_whenValid() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v3/api-with-examples.yaml")
                .build();

        final Request request = SimpleRequest.Builder
                .post("/test")
                .withContentType("application/json")
                .withBody(pretty(of(
                        "timestamp", "1937-01-01T12:00:27.87+00:20",
                        "uri", "http://example.com"
                )))
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    private class TestValidator implements CustomRequestValidator {
        @Override
        public ValidationReport validate(@Nonnull final Request request, @Nonnull final ApiOperation apiOperation) {
            final Optional<Object> extensionValue = apiOperation.getOperation().getExtensions().entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase("x-test-extension"))
                    .map(Map.Entry::getValue)
                    .findFirst();
            if (extensionValue.filter(value -> request.getHeaders().get("Extension").contains(value)).isPresent()) {
                return ValidationReport.empty();
            } else {
                return ValidationReport.singleton(ValidationReport.Message.create("test.extension", "Header extension didn't match expected value").build());
            }
        }
    }

    @Test
    public void validate_withUnexpectedQueryParam_shouldFail() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-users.yaml")
                        .withLevelResolver(LevelResolver.create()
                                .withLevel("validation.request.parameter.query.unexpected", ValidationReport.Level.ERROR)
                                .build()).build();

        final Request request = SimpleRequest.Builder.get("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withQueryParam("UnexpectedParameter", "Value").build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.query.unexpected");
    }
}
