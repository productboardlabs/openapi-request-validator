package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleResponse;
import org.junit.Test;

import static com.atlassian.oai.validator.model.Request.Method.GET;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadJsonResponse;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadXmlResponse;

public class OpenAPIV3ResponseValidationTest {

    private final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator.createFor("/oai/v3/api-users.yaml").build();

    @Test
    public void validate_withValidResponse_shouldSucceed() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .withBody(loadJsonResponse("users-valid"))
                .build();

        assertPass(classUnderTest.validateResponse("/users", GET, response));
    }

    @Test
    public void validate_withMissingResponseBody_shouldFail() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .build();

        assertFail(classUnderTest.validateResponse("/users", GET, response),
                "validation.response.body.missing");
    }

    @Test
    public void validate_withUnsupportedContentType_shouldFail() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("text/xml")
                .withBody(loadXmlResponse("users-valid"))
                .build();

        assertFail(classUnderTest.validateResponse("/users", GET, response),
                "validation.response.contentType.notAllowed");
    }

    @Test
    public void validate_withSupportedContentType_shouldPass_whenMultipleDefined() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("text/xml")
                .withBody(loadXmlResponse("user-valid"))
                .build();

        assertPass(classUnderTest.validateResponse("/users/1", GET, response));
    }

    @Test
    public void validate_withResponseBodyMissingRequiredField_shouldFail() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .withBody(loadJsonResponse("user-invalid-missingrequired"))
                .build();

        assertFail(classUnderTest.validateResponse("/users/1", Request.Method.GET, response),
                "validation.response.body.schema.required");
    }

    @Test
    public void validate_withResponseBodyWithAdditionalFields_shouldFail() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .withBody(loadJsonResponse("user-invalid-additionalproperties"))
                .build();

        assertFail(classUnderTest.validateResponse("/users/1", Request.Method.GET, response),
                "validation.response.body.schema.additionalProperties");
    }

    @Test
    public void validate_withResponseContainingMalformedJson_shouldFail() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .withBody(loadJsonResponse("user-invalid-malformedjson"))
                .build();

        assertFail(classUnderTest.validateResponse("/users/1", Request.Method.GET, response),
                "validation.response.body.schema.invalidJson");
    }

    @Test
    public void validate_withResponseContainingUnknownStatusCode_shouldFail_whenNoDefaultResponseDefined() {
        final Response response = SimpleResponse.Builder.status(666).build();

        assertFail(classUnderTest.validateResponse("/users/1", Request.Method.GET, response),
                "validation.response.status.unknown");
    }

    @Test
    public void validate_withResponseContainingUnknownStatusCode_shouldPass_whenDefaultResponseDefined() {
        final Response response = SimpleResponse.Builder.status(666).withBody(loadJsonResponse("error-valid")).build();

        assertPass(classUnderTest.validateResponse("/users", Request.Method.GET, response));
    }

    @Test
    public void validate_withValidResponseHeader_shouldPass() {
        final Response response = SimpleResponse.Builder
                .serverError()
                .withHeader("X-Failure-Code", "123456")
                .build();

        assertPass(classUnderTest.validateResponse("/healthcheck", Request.Method.GET, response));
    }

    @Test
    public void validate_withInvalidResponseHeader_shouldFail() {
        final Response response = SimpleResponse.Builder
                .serverError()
                .withHeader("X-Failure-Code", "1.0")
                .build();

        assertFail(classUnderTest.validateResponse("/healthcheck", Request.Method.GET, response),
                "validation.response.header.schema.type");
    }
}
