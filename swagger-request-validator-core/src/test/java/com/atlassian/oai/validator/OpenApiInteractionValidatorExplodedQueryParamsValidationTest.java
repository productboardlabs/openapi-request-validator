package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static org.junit.Assert.assertTrue;

public class OpenApiInteractionValidatorExplodedQueryParamsValidationTest {

    private final OpenApiInteractionValidator gradleValidator =
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-exploded-query-params.yaml").build();

    private static SimpleRequest buildValidRequest() {
        return SimpleRequest.Builder
                .get("/api/builds")
                .withQueryParam("sinceBuild", "someBuild")
                .withContentType("application/json")
                .build();
    }

    private static SimpleRequest buildInvalidRequest() {
        return SimpleRequest.Builder
                .get("/api/builds")
                .withQueryParam("since", "someString")
                .withContentType("application/json")
                .build();
    }

    @Test
    public void valid_OpenApi3() {
        // given:
        final Request request = buildValidRequest();

        // when:
        final ValidationReport result = gradleValidator.validateRequest(request);

        // then:
        assertPass(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void invalid_OpenApi3() {
        // given:
        final Request request = buildInvalidRequest();

        // when:
        final ValidationReport result = gradleValidator.validateRequest(request);

        // then:
        assertFail(result, "validation.request.parameter.schema.format.date");
    }
}
