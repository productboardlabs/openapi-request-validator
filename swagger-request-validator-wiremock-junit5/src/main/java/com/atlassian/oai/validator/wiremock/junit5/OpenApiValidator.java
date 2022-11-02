package com.atlassian.oai.validator.wiremock.junit5;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.oai.validator.wiremock.junit5.WireMockRequestResponseUtil.toRequest;
import static com.atlassian.oai.validator.wiremock.junit5.WireMockRequestResponseUtil.toResponse;
import static java.util.Objects.requireNonNull;

/**
 * A WireMock extension that applies OpenAPI / Swagger validation to WireMock requests and responses.
 * <p>
 * The extension is compatible with JUnit5 and can be added to a {@link com.github.tomakehurst.wiremock.WireMockServer}
 * instance. It can be:
 * <pre>
 *  - Globally applied by providing one OpenAPI spec url when instantiating. This applies to the case where only one
 *  specific service is stubbed by a WireMock server.
 *  - Locally applied per stub mapping. This applies to cases where multiple services are being stubbed by one WireMock
 *  server.
 * </pre>
 * <p>
 * (see <a href="http://wiremock.org/docs/extending-wiremock/">Extending WireMock</a>). The current validation report
 * can be accessed with {@link #getReport()}, and the convenience method {@link #assertValidationPassed()} can be used
 * to check for validation errors and throw an exception if any are found.
 * <p>
 * <em>Important</em>: The extension will continue accumulating validation errors on each call to the WireMock server.
 * Call {@link #reset()} before your test to ensure you only get validation errors for the current test execution.
 * <p>
 * Example with global validation (one OpenAPI spec URL for all)
 * <pre>
 *  private static final OpenApiValidator OPEN_API_VALIDATOR = new OpenApiValidator(SPEC_URL);
 *
 *  &#64;RegisterExtension
 *  private static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
 *      .options(wireMockConfig()
 *          .dynamicPort()
 *          .extensions(openApiValidator))
 *     .build();
 *
 *  &#64;AfterEach
 *  public void teardown() {
 *      OPEN_API_VALIDATOR.reset();
 *  }
 *
 *  &#64;Test
 *  public void testFoo() {
 *      // Some interactions with the WireMock server
 *      ...
 *
 *      OPEN_API_VALIDATOR.assertValidationPassed();
 *  }
 * </pre>
 * <p>
 * Example with local validation (OpenAPI spec URL per stub mapping)
 * <pre>
 *  private static final OpenApiValidator OPEN_API_VALIDATOR = new OpenApiValidator();
 *
 *  &#64;RegisterExtension
 *  private static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
 *      .options(wireMockConfig()
 *          .dynamicPort()
 *          .extensions(OPEN_API_VALIDATOR))
 *     .build();
 *
 *  &#64;BeforeEach
 *  void setUp() {
 *      WIREMOCK.stubFor(get(urlPathMatching("/test"))
 *          .withPostServeAction("open-api-validator", new OpenApiValidator.OasUrlParameter(SPEC_URL))
 *          .willReturn(ok());
 *  }
 * </pre>
 */
public class OpenApiValidator extends PostServeAction {

    private static final Logger log = LoggerFactory.getLogger(OpenApiValidator.class);

    private OpenApiInteractionValidator validator = null;

    private ValidationReport report = ValidationReport.empty();

    private final boolean isGlobal;

    public OpenApiValidator(final String specUrlOrDefinition) {
        this.validator = OpenApiInteractionValidator.createFor(specUrlOrDefinition).build();
        this.isGlobal = true;
    }

    public OpenApiValidator(final OpenApiInteractionValidator validator) {
        this.validator = requireNonNull(validator, "A configured validator is required");
        this.isGlobal = true;
    }

    public OpenApiValidator() {
        this.isGlobal = false;
    }

    @Override
    public String getName() {
        return "open-api-validator";
    }

    @Override
    public void doAction(final ServeEvent serveEvent,
                         final Admin admin,
                         final Parameters parameters) {
        if (isGlobal()) {
            return;
        }

        final OasUrlParameter parameter = parameters.as(OasUrlParameter.class);
        validator = OpenApiInteractionValidator.createFor(parameter.getOasUrl()).build();

        validateRequestResponse(serveEvent.getRequest(), serveEvent.getResponse());
    }

    @Override
    public void doGlobalAction(final ServeEvent serveEvent, final Admin admin) {
        if (!isGlobal()) {
            return;
        }

        validateRequestResponse(serveEvent.getRequest(), serveEvent.getResponse());
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
     * Reset validation messages from the validation report.
     * <p>
     * This method should be invoked between tests to ensure validation messages don't carry over between test runs
     * e.g.
     * <pre>
     *     &#64;AfterEach
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

    private void validateRequestResponse(final com.github.tomakehurst.wiremock.verification.LoggedRequest loggedRequest,
                                         final com.github.tomakehurst.wiremock.http.LoggedResponse loggedResponse) {
        try {
            report = report.merge(validator.validate(toRequest(loggedRequest), toResponse(loggedResponse)));
        } catch (final Exception e) {
            log.error("Exception occurred while validating request", e);
            throw e;
        }
    }

    private boolean isGlobal() {
        return isGlobal;
    }

    /**
     * OpenAPI spec URL parameter to be used in stub mappings.
     * <p>
     * Example:
     * <pre>
     *      wiremock.stubFor(get(urlPathMatching("/test"))
     *          .withPostServeAction("open-api-validator", new OpenApiValidator.OasUrlParameter(SPEC_URL))
     *          .willReturn(ok());
     * </pre>
     */
    public static class OasUrlParameter {

        private String oasUrl;

        public OasUrlParameter(@JsonProperty final String oasUrl) {
            this.oasUrl = oasUrl;
        }

        public OasUrlParameter() {
        }

        public String getOasUrl() {
            return oasUrl;
        }
    }

}
