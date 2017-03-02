package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

/**
 * General behavioral tests for the {@link SwaggerRequestResponseValidator}.
 *
 * @see RequestValidationTest
 * @see ResponseValidationTest
 */
public class SwaggerRequestResponseValidatorTest {

    private final SwaggerRequestResponseValidator classUnderTest =
            SwaggerRequestResponseValidator.createFor("/oai/api-users.json").build();

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

    @Test
    public void validate_jsonPayloadAccepted() {
        SwaggerRequestResponseValidator.createFor("{}").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validate_neitherPathNorJson() {
        SwaggerRequestResponseValidator.createFor("<>").build();
    }

}
