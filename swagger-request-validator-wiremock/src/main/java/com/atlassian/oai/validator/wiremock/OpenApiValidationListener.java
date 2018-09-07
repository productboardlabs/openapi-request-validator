package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * A WireMock request listener that applies OpenAPI / Swagger validation to WireMock interactions.
 * <p>
 * The listener can be added to a {@link com.github.tomakehurst.wiremock.junit.WireMockRule} or
 * {@link com.github.tomakehurst.wiremock.WireMockServer} instance
 * (see <a href="http://wiremock.org/docs/extending-wiremock/">Extending WireMock</a>). The current validation report
 * can be accessed with {@link #getReport()}, and the convenience method {@link #assertValidationPassed()} can be used
 * to check for validation errors and throw an exception if any are found.
 * <p>
 * <em>Important</em>: The listener will continue accumulating validation errors on each call to the WireMock server.
 * Call {@link #reset()} before your test to ensure you only get validation errors for the current test execution.
 * <p>
 * E.g.
 * <pre>
 *  &#64;Rule
 *  public WireMockRule wireMockRule;
 *  private OpenApiValidationListener validationListener;
 *
 *  public ValidatedWireMockTestExample() {
 *      this.validationListener = new OpenApiValidationListener(SPEC_URL);
 *      this.wireMockRule = new WireMockRule(PORT);
 *      this.wireMockRule.addMockServiceRequestListener(validationListener);
 *  }
 *
 *  &#64;After
 *  public void teardown() {
 *      this.validationListener.reset();
 *  }
 *
 *  &#64;Test
 *  public void testFoo() {
 *      // Some interactions with the WireMock server
 *      ...
 *
 *      this.validationListener.assertValidationPassed();
 *  }
 * </pre>
 *
 * @see ValidatedWireMockRule
 */
public class OpenApiValidationListener implements RequestListener {

    private static final Logger log = LoggerFactory.getLogger(OpenApiValidationListener.class);

    private final OpenApiInteractionValidator validator;
    private ValidationReport report = ValidationReport.empty();

    public OpenApiValidationListener(final String specUrlOrDefinition) {
        validator = OpenApiInteractionValidator.createFor(specUrlOrDefinition).build();
    }

    public OpenApiValidationListener(final OpenApiInteractionValidator validator) {
        this.validator = requireNonNull(validator, "A configured validator is required");
    }

    @Override
    public void requestReceived(final Request request, final Response response) {
        try {
            report = report.merge(validator.validate(WireMockRequest.of(request), WireMockResponse.of(response)));
        } catch (final Exception e) {
            log.error("Exception occurred while validating request", e);
            throw e;
        }
    }

    /**
     * Access the current validation report. This will contain all messages since the last call to {@link #reset()}.
     * <p>
     * Most often clients will simply want to invoke {@link #assertValidationPassed()} rather than access
     * the report directly.
     *
     * @return the current validation report.
     */
    public ValidationReport getReport() {
        return report;
    }

    /**
     * Reset this listener instance and remove validation messages from the validation report.
     * <p>
     * This method should be invoked between tests to ensure validation messages don't carry over between test runs
     * e.g.
     * <pre>
     *     &#64;After
     *     public void tearDown() {
     *          validationListener.reset();
     *     }
     * </pre>
     */
    public void reset() {
        report = ValidationReport.empty();
    }

    /**
     * Assert that the current validation report contains no errors and fail if it does.
     *
     * @throws OpenApiValidationException if the current validation report contains any errors.
     */
    public void assertValidationPassed() {
        if (report.hasErrors()) {
            throw new OpenApiValidationException(report);
        }
    }

    public static class OpenApiValidationException extends RuntimeException {

        private final ValidationReport report;

        public OpenApiValidationException(final ValidationReport report) {
            super(JsonValidationReportFormat.getInstance().apply(report));
            this.report = report;
        }

        public ValidationReport getValidationReport() {
            return report;
        }
    }
}
