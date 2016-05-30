package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

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
        final Request request = SimpleRequest.Builder.get("/users").build();
        final Response response = SimpleResponse.Builder.ok().withBody("[]").build();

        assertPass(classUnderTest.validate(request, response));
    }

}
