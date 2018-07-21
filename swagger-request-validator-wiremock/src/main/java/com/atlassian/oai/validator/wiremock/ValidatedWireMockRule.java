package com.atlassian.oai.validator.wiremock;

import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * A near drop-in replacement for the {@link WireMockRule} that adds support for validating the request/response
 * interactions against a Swagger API specification.
 * <p>
 * If a validation failure is found, will throw a
 * {@link OpenApiValidationListener.OpenApiValidationException} that will
 * fail the test.
 *
 * @see OpenApiValidationListener
 */
public class ValidatedWireMockRule extends WireMockRule {

    private OpenApiValidationListener validationListener;

    public ValidatedWireMockRule(final String swaggerJsonUrl, final Options options) {
        super(options);
        setupValidationListener(swaggerJsonUrl);
    }

    public ValidatedWireMockRule(final String swaggerJsonUrl, final Options options, final boolean failOnUnmatchedStubs) {
        super(options, failOnUnmatchedStubs);
        setupValidationListener(swaggerJsonUrl);
    }

    public ValidatedWireMockRule(final String swaggerJsonUrl, final int port) {
        super(port);
        setupValidationListener(swaggerJsonUrl);
    }

    public ValidatedWireMockRule(final String swaggerJsonUrl, final int port, final Integer httpsPort) {
        super(port, httpsPort);
        setupValidationListener(swaggerJsonUrl);
    }

    public ValidatedWireMockRule(final String swaggerJsonUrl) {
        setupValidationListener(swaggerJsonUrl);
    }

    private void setupValidationListener(final String swaggerJsonUrl) {
        validationListener = new OpenApiValidationListener(swaggerJsonUrl);
        addMockServiceRequestListener(validationListener);
    }

    @Override
    protected void before() {
        validationListener.reset();
    }

    @Override
    protected void after() {
        try {
            validationListener.assertValidationPassed();
        } finally {
            // Need to ensure the wiremock server is shutdown if the assertion fails
            stop();
        }
    }
}
