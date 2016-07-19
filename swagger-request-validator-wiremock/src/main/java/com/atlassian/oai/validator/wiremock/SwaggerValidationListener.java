package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReportFormatter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.Response;

/**
 * A WireMock request listener that applies Swagger request/response validation to WireMock interactions.
 * <p>
 * E.g.
 * <pre>
 *  &#64;Rule
 *  public WireMockRule wireMockRule;
 *  private SwaggerValidationListener validationListener;
 *
 *  public SwaggerValidatedWireMockTestExample() {
 *      this.validationListener = new SwaggerValidationListener(SWAGGER_JSON_URL);
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
 */
public class SwaggerValidationListener implements RequestListener {

    private final SwaggerRequestResponseValidator validator;
    private ValidationReport report = ValidationReport.empty();

    public SwaggerValidationListener(final String swaggerJsonUrl) {
        this.validator = new SwaggerRequestResponseValidator(swaggerJsonUrl);
    }

    @Override
    public void requestReceived(final Request request, final Response response) {
        report = report.merge(validator.validate(new WireMockRequest(request), new WireMockResponse(response)));
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
        this.report = ValidationReport.empty();
    }

    /**
     * Assert that the current validation report contains no errors and fail if it does.
     *
     * @throws SwaggerValidationException if the current validation report contains any errors.
     */
    public void assertValidationPassed() {
        if (report.hasErrors()) {
            throw new SwaggerValidationException(report);
        }
    }

    public static class SwaggerValidationException extends RuntimeException {

        public SwaggerValidationException(final ValidationReport report) {
            super(ValidationReportFormatter.format(report));
        }
    }
}
