package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadResponse;

/**
 * Tests for Response validation behavior
 */
public class ResponseValidationTest {

    private final SwaggerRequestResponseValidator classUnderTest =
            SwaggerRequestResponseValidator.createFor("/oai/api-users.json").build();

    @Test
    public void validate_withResponseBodyMissingRequiredField_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("user-invalid-missingrequired")).build();

        assertFail(classUnderTest.validate(request, response), "validation.schema.required");
    }

    @Test
    public void validate_withResponseBodyWithAdditionalFields_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("user-invalid-additionalproperties")).build();

        assertFail(classUnderTest.validate(request, response), "validation.schema.additionalProperties");
    }

    @Test
    public void validate_withResponseBodyBadDataFormat_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("user-invalid-baddataformat")).build();

        assertFail(classUnderTest.validate(request, response), "validation.schema.type");
    }

    @Test
    public void validate_withResponseMissingRequiredBody_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().build();

        assertFail(classUnderTest.validate(request, response), "validation.response.body.missing");
    }

    @Test
    public void validate_withRequiredResponseBodyEmpty_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().withBody("").build();

        assertFail(classUnderTest.validate(request, response), "validation.response.body.missing");
    }

    @Test
    public void validate_withResponseContainingMalformedJson_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("user-invalid-malformedjson")).build();

        assertFail(classUnderTest.validate(request, response), "validation.schema.invalidJson");
    }

    @Test
    public void validate_withResponseNotMatchingSchemaForStatusCode_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.notFound().withBody(loadResponse("user-valid")).build();

        assertFail(classUnderTest.validate(request, response), "validation.schema.required");
    }

    @Test
    public void validate_withResponseContainingUnknownStatusCode_shouldFail_whenNoDefaultResponseDefined() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.status(666).build();

        assertFail(classUnderTest.validate(request, response), "validation.response.status.unknown");
    }

    @Test
    public void validate_withResponseContainingUnknownStatusCode_shouldPass_whenDefaultResponseDefined() {
        final Request request = SimpleRequest.Builder.get("/users").build();
        final Response response = SimpleResponse.Builder.status(666).withBody(loadResponse("error-valid")).build();

        assertPass(classUnderTest.validate(request, response));
    }
}
