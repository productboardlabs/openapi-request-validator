package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadJsonRequest;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadRequest;

public class OpenAPIV3RequestValidationTest {

    private final SwaggerRequestResponseValidator classUnderTest =
            SwaggerRequestResponseValidator.createFor("/oai/v3/api-users.yaml").build();

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

        assertFail(classUnderTest.validateRequest(request), "validation.schema.invalidJson");
    }

    @Test
    public void validate_withInvalidRequestBody_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withContentType("application/json")
                .withBody("{}")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.schema.required");
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

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.invalidFormat");
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

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.invalidFormat");
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

        assertFail(classUnderTest.validateRequest(request), "validation.schema.type");
    }

    @Test
    public void validate_withArrayQueryParam_shouldFail_whenInvalidFormat() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withQueryParam("filter", "1", "bob", "3")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.schema.type");
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

}
