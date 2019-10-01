package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

public class OpenAPIV3RequestCookieParamValidationTest {

    private final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-with-cookie-param.yaml").build();

    @Test
    public void validate_withSingleValidCookieParams_shouldPass() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withHeader("Cookie", "maxCount=10")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withMultipleValidCookieParams_shouldPass() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withHeader("Cookie", "maxCount=10; sorting=name,email; authorization=EncryptedUsernameAndPassword")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withOneInvalidCookieParamsAmongMultiple_shouldFail() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withHeader("Cookie", "maxCount=10; sorting=name; authorization=EncryptedUsernameAndPassword")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.schema.enum");
    }

    @Test
    public void validate_withInvalidCookieParams_shouldFail() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withHeader("Cookie", "maxCount=aaa")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_missCookieParams_shouldFail() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.cookie.missing");
    }

    @Test
    public void validate_withArrayCookieParam_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withHeader("Cookie", "maxCount=10; filter=1; filter=2; filter=3")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withArrayCookieParam_shouldFail_whenInvalidAccordingToDefinedStyle() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withHeader("Cookie", "maxCount=10; filter=1,2,3")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_withArrayCookieParam_shouldFail_whenInvalidFormat() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withHeader("Cookie", "maxCount=10; filter=1; filter=bob; filter=3")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.schema.type");
    }
}
