package com.atlassian.oai.validator.wiremock.junit5;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ServeEventListener;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.oai.validator.wiremock.junit5.WireMockRequestResponseUtil.toRequest;
import static com.atlassian.oai.validator.wiremock.junit5.WireMockRequestResponseUtil.toResponse;
import static java.util.Objects.requireNonNull;

/**
 * A WireMock extension that applies OpenAPI / Swagger validation to WireMock requests and responses.
 * <p>
 * The extension implements {@link ServeEventListener} and performs validation after the request/response
 * cycle has completed (via {@link #afterComplete(ServeEvent, Parameters)}).
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
 * Call {@link #reset()} between tests to ensure you only get validation errors for the current test execution.
 * <p>
 * Example with global validation (one OpenAPI spec URL for all):
 * <pre>
 *  private static final OpenApiValidationListener VALIDATION_LISTENER = new OpenApiValidationListener(SPEC_URL);
 *
 *  &#64;RegisterExtension
 *  private static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
 *      .options(wireMockConfig()
 *          .dynamicPort()
 *          .extensions(VALIDATION_LISTENER))
 *     .build();
 *
 *  &#64;AfterEach
 *  public void teardown() {
 *      VALIDATION_LISTENER.reset();
 *  }
 *
 *  &#64;Test
 *  public void testFoo() {
 *      // Some interactions with the WireMock server
 *      ...
 *
 *      VALIDATION_LISTENER.assertValidationPassed();
 *  }
 * </pre>
 * <p>
 * Example with local validation (OpenAPI spec URL per stub mapping):
 * <pre>
 *  private static final OpenApiValidationListener VALIDATION_LISTENER = new OpenApiValidationListener();
 *
 *  &#64;RegisterExtension
 *  private static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
 *      .options(wireMockConfig()
 *          .dynamicPort()
 *          .extensions(VALIDATION_LISTENER))
 *     .build();
 *
 *  &#64;BeforeEach
 *  void setUp() {
 *      WIREMOCK.stubFor(get(urlPathMatching("/test"))
 *          .withServeEventListener("open-api-validation-listener", new OpenApiValidationListener.OasUrlParameter(SPEC_URL))
 *          .willReturn(ok()));
 *  }
 * </pre>
 */
public class OpenApiValidationListener implements ServeEventListener {

    private static final Logger log = LoggerFactory.getLogger(OpenApiValidationListener.class);

    private final OpenApiInteractionValidator validator;

    private ValidationReport report = ValidationReport.empty();

    private final boolean isGlobal;

    /**
     * Create a global validation listener using the provided OpenAPI spec URL or inline definition.
     *
     * @param specUrlOrDefinition the URL of the OpenAPI spec or an inline spec definition
     */
    public OpenApiValidationListener(final String specUrlOrDefinition) {
        this.validator = OpenApiInteractionValidator.createFor(specUrlOrDefinition).build();
        this.isGlobal = true;
    }

    /**
     * Create a global validation listener using the provided pre-configured validator.
     *
     * @param validator a pre-configured {@link OpenApiInteractionValidator}
     */
    public OpenApiValidationListener(final OpenApiInteractionValidator validator) {
        this.validator = requireNonNull(validator, "A configured validator is required");
        this.isGlobal = true;
    }

    /**
     * Create a local (per-stub) validation listener. The OpenAPI spec URL must be provided
     * per stub mapping via {@link OasUrlParameter}.
     */
    public OpenApiValidationListener() {
        this.validator = null;
        this.isGlobal = false;
    }

    @Override
    public String getName() {
        return "open-api-validation-listener";
    }

    /**
     * Returns {@code true} when this listener was constructed with a spec URL or validator (global mode),
     * so WireMock will invoke it for every served request. Returns {@code false} in per-stub (local) mode,
     * where the listener is wired explicitly to individual stub mappings via
     * {@code .withServeEventListener(...)}.
     */
    @Override
    public boolean applyGlobally() {
        return isGlobal;
    }

    /**
     * Called by WireMock after the full request/response cycle has completed. Performs validation
     * of the request and response against the OpenAPI spec.
     * <p>
     * In global mode the validator configured at construction time is used. In local (per-stub) mode
     * the OpenAPI spec URL is read from the stub-level {@link Parameters} via {@link OasUrlParameter}.
     *
     * @param serveEvent the completed serve event containing the logged request and response
     * @param parameters stub-level parameters (used in local/per-stub mode to supply the spec URL)
     */
    @Override
    public void afterComplete(final ServeEvent serveEvent, final Parameters parameters) {
        final OpenApiInteractionValidator effectiveValidator;
        if (isGlobal) {
            effectiveValidator = validator;
        } else {
            final OasUrlParameter parameter = parameters.as(OasUrlParameter.class);
            effectiveValidator = OpenApiInteractionValidator.createFor(parameter.getOasUrl()).build();
        }

        validateRequestResponse(effectiveValidator, serveEvent);
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
     * This method should be invoked between tests to ensure validation messages don't carry over between test runs,
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

    private void validateRequestResponse(final OpenApiInteractionValidator effectiveValidator,
                                         final ServeEvent serveEvent) {
        try {
            report = report.merge(effectiveValidator.validate(
                    toRequest(serveEvent.getRequest()),
                    toResponse(serveEvent.getResponse())));
        } catch (final Exception e) {
            log.error("Exception occurred while validating request", e);
            throw e;
        }
    }

    /**
     * OpenAPI spec URL parameter to be used in per-stub mappings.
     * <p>
     * Example:
     * <pre>
     *      wiremock.stubFor(get(urlPathMatching("/test"))
     *          .withServeEventListener("open-api-validation-listener", new OpenApiValidationListener.OasUrlParameter(SPEC_URL))
     *          .willReturn(ok()));
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
