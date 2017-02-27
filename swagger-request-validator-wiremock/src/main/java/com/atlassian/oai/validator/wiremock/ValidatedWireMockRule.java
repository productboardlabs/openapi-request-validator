package com.atlassian.oai.validator.wiremock;

import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * A near drop-in replacement for the {@link WireMockRule} that adds support for validating the request/response
 * interactions against a Swagger API specification.
 * <p>
 * If a validation failure is found, will throw a
 * {@link com.atlassian.oai.validator.wiremock.SwaggerValidationListener.SwaggerValidationException} that will
 * fail the test.
 *
 * @see SwaggerValidationListener
 */
public class ValidatedWireMockRule extends WireMockRule {

    private SwaggerValidationListener validationListener;

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
        this.validationListener = new SwaggerValidationListener(swaggerJsonUrl);
        this.addMockServiceRequestListener(validationListener);
    }

    @Override
    protected void before() {
        this.validationListener.reset();
    }

    @Override
    protected void after() {
        try {
            this.validationListener.assertValidationPassed();
        } finally {
            // Need to ensure the wiremock server is shutdown if the assertion fails
            stop();
        }
    }
}
