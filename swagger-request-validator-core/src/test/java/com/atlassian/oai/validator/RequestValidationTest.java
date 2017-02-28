package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadRequest;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadResponse;

/**
 * Tests for Request validation behavior
 */
public class RequestValidationTest {

    private final SwaggerRequestResponseValidator classUnderTest =
            SwaggerRequestResponseValidator.createFor("/oai/api-users.json").build();

    private final Response validUserResponse =
            SimpleResponse.Builder.ok().withBody(loadResponse("user-valid")).build();
    private final Response validUsersResponse =
            SimpleResponse.Builder.ok().withBody(loadResponse("users-valid")).build();
    private final SimpleResponse okResponse = SimpleResponse.Builder.ok().build();

    @Test
    public void validate_withValidRequestResponse_shouldSucceed() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();

        assertPass(classUnderTest.validate(request, validUserResponse));
    }

    @Test
    public void validate_withInvalidPathParam_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/a").build();
        final Response response = SimpleResponse.Builder.badRequest().build();

        assertFail(classUnderTest.validate(request, response),
                "validation.request.parameter.invalidFormat");
    }

    @Test
    public void validate_withInvalidRequestMethod_shouldFail() {
        final Request request = SimpleRequest.Builder.patch("/users/1").build();

        assertFail(classUnderTest.validate(request, okResponse),
                "validation.request.operation.notAllowed");
    }

    @Test
    public void validate_withRequestMissingRequiredJsonBody_shouldFail() {
        final Request request = SimpleRequest.Builder.post("/users").build();

        assertFail(classUnderTest.validate(request, okResponse),
                "validation.request.body.missing");
    }

    @Test
    public void validate_withRequestMissingRequiredFormDataBody_shouldFail() {
        final String formData = "fmail=abc%40gmail.com";
        final Request request = SimpleRequest.Builder.put("/users/1").withBody(formData).build();
        assertFail(classUnderTest.validate(request, validUserResponse), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withValidJsonBody_shouldPass() {
        final Request request = SimpleRequest.Builder.post("/users").withBody(loadRequest("newuser-valid")).build();

        assertPass(classUnderTest.validate(request, validUserResponse));
    }

    @Test
    public void validate_withValidFormDataBody_shouldPass() {
        final String formData = "email=abc%40gmail.com";
        final Request request = SimpleRequest.Builder.put("/users/1").withBody(formData).build();
        assertPass(classUnderTest.validate(request, validUserResponse));
    }

    @Test
    public void validate_formData_manyValuesForSingleKey() {
        final String formData = "email=abc%40gmail.com&email=";
        final Request request = SimpleRequest.Builder.put("/users/1").withBody(formData).build();
        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withInvalidJsonRequestBody_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadRequest("newuser-invalid-missingrequired"))
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.schema.required");
    }

    @Test
    public void validate_withInvalidFormDataRequestBody_shouldFail() {
        final String formData = "malformed-form-url-encoded";
        final Request request = SimpleRequest.Builder.put("/users/1").withBody(formData).build();
        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_authorizationHeaderIsChecked_shouldPass() {
        final Request request = SimpleRequest.Builder
                .get("/secure/users/1")
                .withHeader("Authorization", "Bearer mytoken")
                .build();
        assertPass(classUnderTest.validate(request, validUserResponse));
    }

    @Test
    public void validate_authorizationHeaderIsChecked_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/secure/users/1").build();
        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.security.missing");
    }

    @Test
    public void validate_authorizationQueryParamIsChecked_shouldPass() {
        final Request request = SimpleRequest.Builder.put("/secure/users/1").withQueryParam("authorization", "token").build();
        assertPass(classUnderTest.validate(request, validUserResponse));
    }

    @Test
    public void validate_authorizationQueryParamIsChecked_shouldFail() {
        final Request request = SimpleRequest.Builder.put("/secure/users/1").build();
        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.security.missing");
    }

    @Test
    public void validate_withRequestBody_shouldFail_whenNoneExpected() {
        final Request request = SimpleRequest.Builder.get("/users").withBody(loadRequest("newuser-valid")).build();

        assertFail(classUnderTest.validate(request, validUsersResponse),
                "validation.request.body.unexpected");
    }

    @Test
    public void validate_withValidQueryParams_shouldPass() {
        final Request request = SimpleRequest.Builder.get("/users").withQueryParam("maxCount", "10").build();

        assertPass(classUnderTest.validate(request, validUsersResponse));
    }

    @Test
    public void validate_withInvalidQueryParamFormat_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users").withQueryParam("maxCount", "a").build();

        assertFail(classUnderTest.validate(request, validUsersResponse),
                "validation.request.parameter.invalidFormat");
    }

    @Test
    public void validate_withMissingQueryParam_shouldPass_whenOptional() {
        final Request request = SimpleRequest.Builder.get("/users").build();

        assertPass(classUnderTest.validate(request, validUsersResponse));
    }

    @Test
    public void validate_withArrayQueryParam_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder.get("/users").withQueryParam("filter", "1,2,3").build();

        assertPass(classUnderTest.validate(request, validUsersResponse));
    }

    @Test
    public void validate_withArrayQueryParam_shouldFail_whenInvalidFormat() {
        final Request request = SimpleRequest.Builder.get("/users").withQueryParam("filter", "1,\"bob\",3").build();

        assertFail(classUnderTest.validate(request, validUsersResponse), "validation.schema.type");
    }

    @Test
    public void validate_withExtraQueryParams_shouldPass() {
        final Request request = SimpleRequest.Builder.get("/users")
                .withQueryParam("foo", "bar")
                .withQueryParam("something", "else")
                .build();

        assertPass(classUnderTest.validate(request, validUsersResponse));
    }

    @Test
    public void validate_withMissingQueryParam_shouldFail_whenRequired() {
        final Request request = SimpleRequest.Builder.get("/healthcheck").build();

        assertFail(classUnderTest.validate(request, okResponse), "validation.request.parameter.query.missing");
    }

    @Test
    public void validate_withValidQueryParam_shouldPass_whenRequired() {
        final Request request = SimpleRequest.Builder.get("/healthcheck").withQueryParam("type", "deep").build();

        assertPass(classUnderTest.validate(request, okResponse));
    }

    @Test
    public void validate_withNoContentType_shouldPass() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadRequest("newuser-valid"))
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
    }

    @Test
    public void validate_withMatchingContentType_shouldPass() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadRequest("newuser-valid"))
                .withHeader("Content-Type", "application/json;charset=UTF-8")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
    }

    @Test
    public void validate_withContentTypeButNoConsumes_shouldPass() {
        final Request request = SimpleRequest.Builder
                .get("/healthcheck")
                .withQueryParam("type", "deep")
                .withHeader("content-type", "application/json")
                .build();

        assertPass(classUnderTest.validate(request, okResponse));
    }

    @Test
    public void validate_withNonMatchingContentType_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadRequest("newuser-valid"))
                .withHeader("content-Type", "text/html")
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.contentType.notAllowed");
    }

    @Test
    public void validate_withInvalidContentType_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadRequest("newuser-valid"))
                .withHeader("Content-Type", "foop")
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.contentType.invalid");
    }

}
