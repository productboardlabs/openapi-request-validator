package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

public class OpenApiV31MultiTypeSchemaValidationTest {

    private final OpenApiInteractionValidator validator =
        OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-multitype.yaml").build();

    @Test
    public void validate_withIntegerId_shouldPass() {
        final Request request = SimpleRequest.Builder
            .post("/users")
            .withContentType("application/json")
            .withBody("{\"id\": 123, \"name\": \"Test User\"}")
            .build();

        final ValidationReport report = validator.validateRequest(request);
        assertPass(report);
    }

    @Test
    public void validate_withStringId_shouldPass() {
        final Request request = SimpleRequest.Builder
            .post("/users")
            .withContentType("application/json")
            .withBody("{\"id\": \"a-string-id\", \"name\": \"Test User\"}")
            .build();

        final ValidationReport report = validator.validateRequest(request);
        assertPass(report);
    }

    @Test
    public void validate_withBooleanId_shouldFail() {
        final Request request = SimpleRequest.Builder
            .post("/users")
            .withContentType("application/json")
            .withBody("{\"id\": true, \"name\": \"Test User\"}")
            .build();

        final ValidationReport report = validator.validateRequest(request);
        assertFail(report);
    }
}
