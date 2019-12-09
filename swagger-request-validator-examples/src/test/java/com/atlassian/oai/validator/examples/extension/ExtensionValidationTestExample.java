package com.atlassian.oai.validator.examples.extension;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.interaction.request.CustomRequestValidator;
import com.atlassian.oai.validator.interaction.response.CustomResponseValidator;
import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * In this example we create a validator with a custom request and response validator.
 * <p>
 * The custom validators will simply verify that a header value matches the value of the test extension,
 * and report if it does not.
 */
public class ExtensionValidationTestExample {

    private static final String OPENAPI_SPEC_URL = "/extension/api-extension.yaml";
    private static final String VALIDATION_REPORT_MESSAGE_KEY = "test.extension";
    private static final String VALIDATION_REPORT_MESSAGE_VALUE = "Header Extension did not match expected value";

    private final TestExtensionValidator testExtensionValidator = new TestExtensionValidator();
    private final OpenApiInteractionValidator validator = OpenApiInteractionValidator.createForSpecificationUrl(OPENAPI_SPEC_URL)
            .withCustomRequestValidation(testExtensionValidator)
            .withCustomResponseValidation(testExtensionValidator)
            .build();
    private final ValidationReport.Message validationReportMessage = ValidationReport.Message
            .create(VALIDATION_REPORT_MESSAGE_KEY, VALIDATION_REPORT_MESSAGE_VALUE)
            .build();

    @Test
    public void testRequestSatisfiesExtensionCriterion() {
        final Request request = SimpleRequest.Builder
                .get("/test")
                .withHeader("Extension", "true")
                .build();
        final ValidationReport report = validator.validateRequest(request);
        final List<String> reportKeys = report.getMessages().stream()
                .map(ValidationReport.Message::getKey)
                .collect(Collectors.toList());

        assertThat(reportKeys, not(hasItem(VALIDATION_REPORT_MESSAGE_KEY)));
    }

    @Test
    public void testRequestDoesNotSatisfyExtensionCriterion() {
        final Request request = SimpleRequest.Builder
                .get("/test")
                .withHeader("Extension", "false")
                .build();
        final ValidationReport report = validator.validateRequest(request);
        final List<String> reportKeys = report.getMessages().stream()
                .map(ValidationReport.Message::getKey)
                .collect(Collectors.toList());

        assertThat(reportKeys, hasItem(VALIDATION_REPORT_MESSAGE_KEY));
    }

    @Test
    public void testResponseSatisfiesExtensionCriterion() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withHeader("Extension", "true")
                .build();
        final ValidationReport report = validator.validateResponse("/test", Request.Method.GET, response);
        final List<String> reportKeys = report.getMessages().stream()
                .map(ValidationReport.Message::getKey)
                .collect(Collectors.toList());

        assertThat(reportKeys, not(hasItem(VALIDATION_REPORT_MESSAGE_KEY)));
    }

    @Test
    public void testResponseDoesNotSatisfyExtensionCriterion() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withHeader("Extension", "false")
                .build();
        final ValidationReport report = validator.validateResponse("/test", Request.Method.GET, response);
        final List<String> reportKeys = report.getMessages().stream()
                .map(ValidationReport.Message::getKey)
                .collect(Collectors.toList());

        assertThat(reportKeys, hasItem(VALIDATION_REPORT_MESSAGE_KEY));
    }

    private class TestExtensionValidator implements CustomRequestValidator, CustomResponseValidator {
        @Override
        public ValidationReport validate(@Nonnull final Request request, @Nonnull final ApiOperation apiOperation) {
            return validate(request.getHeaderValues("Extension"), apiOperation);
        }

        @Override
        public ValidationReport validate(@Nonnull final Response response, @Nonnull final ApiOperation apiOperation) {
            return validate(response.getHeaderValues("Extension"), apiOperation);
        }

        private ValidationReport validate(@Nonnull final Collection<String> extensionHeaderValues, @Nonnull final ApiOperation apiOperation) {
            final Optional<String> extensionValue = apiOperation.getOperation().getExtensions().entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase("x-test-extension"))
                    .map(Map.Entry::getValue)
                    .map(objValue -> (String) objValue)
                    .findFirst();

            if (extensionValue.filter(extensionHeaderValues::contains).isPresent()) {
                return ValidationReport.empty();
            } else {
                return ValidationReport.singleton(validationReportMessage);
            }
        }
    }
}
