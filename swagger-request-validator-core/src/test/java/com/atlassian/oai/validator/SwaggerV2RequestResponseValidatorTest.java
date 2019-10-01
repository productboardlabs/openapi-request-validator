package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.LevelResolver;
import org.junit.Test;

import static com.atlassian.oai.validator.report.ValidationReport.Level.IGNORE;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

/**
 * General behavioral tests for the {@link OpenApiInteractionValidator}.
 *
 * @see SwaggerV2RequestValidationTest
 * @see SwaggerV2ResponseValidationTest
 */
public class SwaggerV2RequestResponseValidatorTest {

    private final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v2/api-users.json").build();

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
    public void validate_withFailures_shouldPass_whenLevelResolverIgnoresFailures() {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v2/api-users.json")
                        .withLevelResolver(LevelResolver
                                .create()
                                .withLoader(null)
                                .withDefaultLevel(IGNORE)
                                .build()
                        ).build();

        final Request request = SimpleRequest.Builder.get("/users/1").build();
        final Response response = SimpleResponse.Builder.ok().build();

        assertPass(classUnderTest.validate(request, response));
    }

    @Test(expected = Exception.class)
    public void create_withNeitherPathNorJson_throwsException() {
        OpenApiInteractionValidator.createFor("<>").build();
    }

    @Test(expected = NullPointerException.class)
    public void create_withNullAuthHeaderKey_throwsNPE() throws Exception {
        OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-users.json")
                .withAuthHeaderData(null, null)
                .build();
    }

    @Test
    public void create_withNullAuthHeaderValue() throws Exception {
        OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-users.json")
                .withAuthHeaderData("api-key", null)
                .build();
    }

    @Test
    public void validate_withBasePathOverride() throws Exception {
        final OpenApiInteractionValidator classUnderTest =
                OpenApiInteractionValidator
                        .createForSpecificationUrl("/oai/v2/api-users.json")
                        .withBasePathOverride("/test")
                        .build();

        final Request request = SimpleRequest.Builder
                .get("/test/users/1")
                .withHeader("Authorization", "Basic EncryptedUsernameAndPassword")
                .build();
        final Response response = SimpleResponse.Builder.ok().withBody("{\"id\":1,\"name\":\"Max\",\"email\":\"max@example.com\"}").build();

        assertPass(classUnderTest.validate(request, response));
    }
}
