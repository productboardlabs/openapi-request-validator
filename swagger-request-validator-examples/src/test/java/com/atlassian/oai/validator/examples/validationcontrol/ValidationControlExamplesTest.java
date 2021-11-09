package com.atlassian.oai.validator.examples.validationcontrol;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Test;
import org.slf4j.Logger;

import static com.atlassian.oai.validator.report.ValidationReport.Level.IGNORE;
import static com.google.common.collect.ImmutableMap.of;
import static io.swagger.v3.core.util.Json.pretty;
import static org.junit.Assert.assertFalse;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Examples showing how to control validation behavior to achieve various goals
 */
public class ValidationControlExamplesTest {

    private static final Logger log = getLogger(ValidationControlExamplesTest.class);

    /**
     * Example demonstrating one way to configure the validator to *only* validate requests.
     * <p>
     * In this example the supplied response is invalid, but the validation still passes because
     * response validation is ignored via the supplied {@link LevelResolver}.
     */
    @Test
    public void requestOnlyValidation() {
        final OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createFor("/oai/api-simple.yaml")
                .withLevelResolver(
                        // The key here is to use the level resolver to ignore the response validation messages
                        // Without this they would be emitted at ERROR level and cause a validation failure.
                        LevelResolver.create()
                                .withLevel("validation.response", IGNORE)
                                .build()
                )
                .build();

        assertPass(
                validator.validate(
                        SimpleRequest.Builder
                                .post("/test")
                                .withBody(pretty(of(
                                        "msg", "Hello world"
                                )))
                                .build(),
                        SimpleResponse.Builder
                                .ok()
                                .withContentType("application/json")
                                // Missing a required response body to cause validation failure
                                .build()
                )
        );
    }

    private void assertPass(final ValidationReport report) {
        if (report.hasErrors()) {
            log.info(JsonValidationReportFormat.getInstance().apply(report));
        }
        assertFalse(report.hasErrors());
    }

}
