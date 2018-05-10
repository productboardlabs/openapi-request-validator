package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import org.junit.Ignore;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadJsonResponse;

/**
 * Tests for Response validation behavior
 */
public class SwaggerV2ResponseValidationTest {

    private final SwaggerRequestResponseValidator classUnderTest =
            SwaggerRequestResponseValidator.createFor("/oai/v2/api-users.json").build();

    private final Request getUserRequest = SimpleRequest.Builder
            .get("/users/1")
            .withHeader("Authorization", "Basic EncryptedUsernameAndPassword")
            .build();
    private final Request getUsersRequest = SimpleRequest.Builder
            .get("/users")
            .withHeader("Authorization", "Basic EncryptedUsernameAndPassword")
            .build();
    private final Request healthcheckRequest = SimpleRequest.Builder.get("/healthcheck").withQueryParam("type", "shallow").build();

    @Test
    public void validate_withResponseBodyMissingRequiredField_shouldFail() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .withBody(loadJsonResponse("user-invalid-missingrequired"))
                .build();

        assertFail(classUnderTest.validate(getUserRequest, response),
                "validation.response.body.schema.required");
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

        assertFail(classUnderTest.validate(getUserRequest, response),
                "validation.response.body.schema.additionalProperties");
        assertFail(classUnderTest.validateResponse("/users/1", Request.Method.GET, response),
                "validation.response.body.schema.additionalProperties");
    }

    @Test
    public void validate_withResponseBodyBadDataFormat_shouldFail() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .withBody(loadJsonResponse("user-invalid-baddataformat"))
                .build();

        assertFail(classUnderTest.validate(getUserRequest, response),
                "validation.response.body.schema.type");
        assertFail(classUnderTest.validateResponse("/users/1", Request.Method.GET, response),
                "validation.response.body.schema.type");
    }

    @Test
    public void validate_withResponseMissingRequiredBody_shouldFail() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .build();

        assertFail(classUnderTest.validate(getUserRequest, response),
                "validation.response.body.missing");
        assertFail(classUnderTest.validateResponse("/users/1", Request.Method.GET, response),
                "validation.response.body.missing");
    }

    @Test
    public void validate_withRequiredResponseBodyEmpty_shouldFail() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .withBody("")
                .build();

        assertFail(classUnderTest.validate(getUserRequest, response),
                "validation.response.body.missing");
        assertFail(classUnderTest.validateResponse("/users/1", Request.Method.GET, response),
                "validation.response.body.missing");
    }

    @Test
    public void validate_withResponseContainingMalformedJson_shouldFail() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .withBody(loadJsonResponse("user-invalid-malformedjson"))
                .build();

        assertFail(classUnderTest.validate(getUserRequest, response),
                "validation.response.body.schema.invalidJson");
        assertFail(classUnderTest.validateResponse("/users/1", Request.Method.GET, response),
                "validation.response.body.schema.invalidJson");
    }

    @Test
    public void validate_withResponseNotMatchingSchemaForStatusCode_shouldFail() {
        final Response response = SimpleResponse.Builder
                .notFound()
                .withContentType("application/json")
                .withBody(loadJsonResponse("user-valid"))
                .build();

        assertFail(classUnderTest.validate(getUserRequest, response),
                "validation.response.body.schema.required");
        assertFail(classUnderTest.validateResponse("/users/1", Request.Method.GET, response),
                "validation.response.body.schema.required");
    }

    @Test
    public void validate_withResponseContainingUnknownStatusCode_shouldFail_whenNoDefaultResponseDefined() {
        final Response response = SimpleResponse.Builder.status(666).build();

        assertFail(classUnderTest.validate(getUserRequest, response),
                "validation.response.status.unknown");
        assertFail(classUnderTest.validateResponse("/users/1", Request.Method.GET, response),
                "validation.response.status.unknown");
    }

    @Test
    public void validate_withResponseContainingUnknownStatusCode_shouldPass_whenDefaultResponseDefined() {
        final Response response = SimpleResponse.Builder.status(666).withBody(loadJsonResponse("error-valid")).build();

        assertPass(classUnderTest.validate(getUsersRequest, response));
        assertPass(classUnderTest.validateResponse("/users", Request.Method.GET, response));
    }

    @Test
    public void validate_withResponseContentTypeMatchingProduces_shouldPass() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withBody(loadJsonResponse("users-valid"))
                .withHeader("Content-Type", "application/json;charset=UTF-8")
                .build();

        assertPass(classUnderTest.validate(getUsersRequest, response));
        assertPass(classUnderTest.validateResponse("/users", Request.Method.GET, response));
    }

    @Test
    public void validate_withResponseContentTypeNotMatchingProduces_shouldFail() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withBody(loadJsonResponse("users-valid"))
                .withHeader("Content-Type", "text/html")
                .build();

        assertFail(classUnderTest.validate(getUsersRequest, response),
                "validation.response.contentType.notAllowed");
        assertFail(classUnderTest.validateResponse("/users", Request.Method.GET, response),
                "validation.response.contentType.notAllowed");
    }

    @Test
    public void validate_withInvalidResponseContentType_shouldFail() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withBody(loadJsonResponse("users-valid"))
                .withHeader("Content-Type", "foop")
                .build();

        assertFail(classUnderTest.validate(getUsersRequest, response),
                "validation.response.contentType.invalid");
        assertFail(classUnderTest.validateResponse("/users", Request.Method.GET, response),
                "validation.response.contentType.invalid");
    }

    @Test
    public void validate_withValidResponseHeader_shouldPass() {
        final Response response = SimpleResponse.Builder
                .serverError()
                .withHeader("X-Failure-Code", "123456")
                .build();

        assertPass(classUnderTest.validate(healthcheckRequest, response));
        assertPass(classUnderTest.validateResponse("/healthcheck", Request.Method.GET, response));
    }

    @Test
    public void validate_withInvalidResponseHeader_shouldFail() {
        final Response response = SimpleResponse.Builder
                .serverError()
                .withHeader("X-Failure-Code", "1.0")
                .build();

        assertFail(classUnderTest.validate(healthcheckRequest, response),
                "validation.response.header.schema.type");
        assertFail(classUnderTest.validateResponse("/healthcheck", Request.Method.GET, response),
                "validation.response.header.schema.type");
    }

    @Test
    @Ignore("Swagger 2.0 parser currently does not read the 'required' flag on headers")
    public void validate_withResponseMissingRequiredHeader_shouldFail() {
        final Response response = SimpleResponse.Builder
                .serverError()
                .build();

        assertFail(classUnderTest.validate(healthcheckRequest, response),
                "validation.response.header.missing");
        assertFail(classUnderTest.validateResponse("/healthcheck", Request.Method.GET, response),
                "validation.response.header.missing");
    }

    @Test
    public void validate_withXmlBody_shouldNotApplySchemaValidation() {
        final SwaggerRequestResponseValidator classUnderTest =
                SwaggerRequestResponseValidator.createFor("/oai/v2/api-non-json-body.json").build();

        final Response response = SimpleResponse.Builder
                .ok()
                .withHeader("Content-Type", "text/xml")
                .withBody("<Result><id>100</id><name>Adam Andrews</name><score>86</score></Result>")
                .build();

        assertPass(classUnderTest.validateResponse("/results", Request.Method.GET, response));
    }

}
