package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadRequest;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadResponse;

public class SwaggerRequestResponseValidatorTest {

    private SwaggerRequestResponseValidator classUnderTest = SwaggerRequestResponseValidator.createFor("/oai/api-users.json").build();

    @Test(expected = NullPointerException.class)
    public void validate_withNullRequest_throwsNPE() {
        final Request request = null;
        final Response response = SimpleResponse.Builder.ok().build();

        classUnderTest.validate(request, response);
    }

    @Test(expected = NullPointerException.class)
    public void validate_withNullResponse_throwsNPE() {
        final Request request = SimpleRequest.Builder.get("/users").build();
        final Response response = null;

        classUnderTest.validate(request, response);
    }

    @Test
    public void validate_withValidRequestResponse_shouldSucceed() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("user-valid")).build();

        assertPass(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withInvalidPathParam_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/a").build();
        final Response response = SimpleResponse.Builder.badRequest().build();

        assertFail(classUnderTest.validate(request, response), "validation.request.parameter.invalidFormat");
    }

    @Test
    public void validate_withInvalidRequestMethod_shouldFail() {
        final Request request = SimpleRequest.Builder.patch("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().build();

        assertFail(classUnderTest.validate(request, response), "validation.request.operation.notAllowed");
    }

    @Test
    public void validate_withRequestMissingRequiredBody_shouldFail() {
        final Request request = SimpleRequest.Builder.post("/users").build();
        final Response response = SimpleResponse.Builder.ok().build();

        assertFail(classUnderTest.validate(request, response), "validation.request.body.missing");
    }

    @Test
    public void validate_withValidRequestBody_shouldPass() {
        final Request request = SimpleRequest.Builder.post("/users").withBody(loadRequest("newuser-valid")).build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("user-valid")).build();

        assertPass(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withInvalidRequestBody_shouldFail() {
        final Request request = SimpleRequest.Builder.post("/users").withBody(loadRequest("newuser-invalid-missingrequired")).build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("user-valid")).build();

        assertFail(classUnderTest.validate(request, response), "validation.schema.required");
    }

    @Test
    public void validate_withRequestBody_shouldFail_whenNoneExpected() {
        final Request request = SimpleRequest.Builder.get("/users").withBody(loadRequest("newuser-valid")).build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("users-valid")).build();

        assertFail(classUnderTest.validate(request, response), "validation.request.body.unexpected");
    }

    @Test
    public void validate_withValidQueryParams_shouldPass() {
        final Request request = SimpleRequest.Builder.get("/users").withQueryParam("maxCount", "10").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("users-valid")).build();

        assertPass(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withInvalidQueryParamFormat_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users").withQueryParam("maxCount", "a").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("users-valid")).build();

        assertFail(classUnderTest.validate(request, response), "validation.request.parameter.invalidFormat");
    }

    @Test
    public void validate_withMissingQueryParam_shouldPass_whenOptional() {
        final Request request = SimpleRequest.Builder.get("/users").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("users-valid")).build();

        assertPass(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withArrayQueryParam_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder.get("/users").withQueryParam("filter", "1,2,3").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("users-valid")).build();

        assertPass(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withArrayQueryParam_shouldFail_whenInvalidFormat() {
        final Request request = SimpleRequest.Builder.get("/users").withQueryParam("filter", "1,\"bob\",3").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("users-valid")).build();

        assertFail(classUnderTest.validate(request, response), "validation.schema.type");
    }

    @Test
    public void validate_withExtraQueryParams_shouldPass() {
        final Request request = SimpleRequest.Builder.get("/users")
                .withQueryParam("foo", "bar")
                .withQueryParam("something", "else")
                .build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("users-valid")).build();

        assertPass(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withMissingQueryParam_shouldFail_whenRequired() {
        final Request request = SimpleRequest.Builder.get("/healthcheck").build();
        final Response response = SimpleResponse.Builder.ok().build();

        assertFail(classUnderTest.validate(request, response), "validation.request.parameter.query.missing");
    }

    @Test
    public void validate_withValidQueryParam_shouldPass_whenRequired() {
        final Request request = SimpleRequest.Builder.get("/healthcheck").withQueryParam("type", "deep").build();
        final Response response = SimpleResponse.Builder.ok().build();

        assertPass(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withResponseBodyMissingRequiredField_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("user-invalid-missingrequired")).build();

        assertFail(classUnderTest.validate(request, response), "validation.schema.required");
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

    @Test
    public void validate_withFailures_shoudPass_whenLevelResolverIgnoresFailures() {
        final SwaggerRequestResponseValidator classUnderTest =
                SwaggerRequestResponseValidator
                        .createFor("/oai/api-users.json")
                        .withLevelResolver(LevelResolver
                                .create()
                                .withLoader(null)
                                .withDefaultLevel(ValidationReport.Level.IGNORE)
                                .build()
                        ).build();

        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().build();

        assertPass(classUnderTest.validate(request, response));
    }

}
