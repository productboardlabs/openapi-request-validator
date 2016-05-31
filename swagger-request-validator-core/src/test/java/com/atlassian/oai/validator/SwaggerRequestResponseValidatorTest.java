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

public class SwaggerRequestResponseValidatorTest {

    private SwaggerRequestResponseValidator classUnderTest =
            new SwaggerRequestResponseValidator("/oai/api-users.json", null);

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

        assertFail(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withInvalidRequestMethod_shouldFail() {
        final Request request = SimpleRequest.Builder.patch("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().build();

        assertFail(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withRequestMissingRequiredBody_shouldFail() {
        final Request request = SimpleRequest.Builder.post("/users").build();
        final Response response = SimpleResponse.Builder.ok().build();

        assertFail(classUnderTest.validate(request, response));
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

        assertFail(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withResponseBodyMissingRequiredField_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("user-invalid-missingrequired")).build();

        assertFail(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withResponseBodyBadDataFormat_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("user-invalid-baddataformat")).build();

        assertFail(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withResponseMissingRequiredBody_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().build();

        assertFail(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withResponseContainingMalformedJson_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().withBody(loadResponse("user-invalid-malformedjson")).build();

        assertFail(classUnderTest.validate(request, response));
    }

    @Test
    public void validate_withResponseNotMatchingSchemaForStatusCode_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.notFound().withBody(loadResponse("user-valid")).build();

        assertFail(classUnderTest.validate(request, response));
    }

}
