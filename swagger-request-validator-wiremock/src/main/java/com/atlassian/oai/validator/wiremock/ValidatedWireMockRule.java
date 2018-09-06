package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * A near drop-in replacement for the {@link WireMockRule} that adds support for validating the request/response
 * interactions against an OpenAPI / Swagger specification.
 * <p>
 * If a validation failure is found, will throw a
 * {@link OpenApiValidationListener.OpenApiValidationException} that will
 * fail the test.
 *
 * @see OpenApiValidationListener
 */
public class ValidatedWireMockRule extends WireMockRule {

    private OpenApiValidationListener validationListener;

    public ValidatedWireMockRule(final String specUrlOrPayload,
                                 final Options options) {
        super(options);
        setupValidationListener(specUrlOrPayload);
    }

    public ValidatedWireMockRule(final OpenApiInteractionValidator validator,
                                 final Options options) {
        super(options);
        setupValidationListener(validator);
    }

    public ValidatedWireMockRule(final String specUrlOrPayload,
                                 final Options options,
                                 final boolean failOnUnmatchedStubs) {
        super(options, failOnUnmatchedStubs);
        setupValidationListener(specUrlOrPayload);
    }

    public ValidatedWireMockRule(final OpenApiInteractionValidator validator,
                                 final Options options,
                                 final boolean failOnUnmatchedStubs) {
        super(options, failOnUnmatchedStubs);
        setupValidationListener(validator);
    }

    public ValidatedWireMockRule(final String specUrlOrPayload,
                                 final int port) {
        super(port);
        setupValidationListener(specUrlOrPayload);
    }

    public ValidatedWireMockRule(final OpenApiInteractionValidator validator,
                                 final int port) {
        super(port);
        setupValidationListener(validator);
    }

    public ValidatedWireMockRule(final String specUrlOrPayload,
                                 final int port,
                                 final Integer httpsPort) {
        super(port, httpsPort);
        setupValidationListener(specUrlOrPayload);
    }

    public ValidatedWireMockRule(final OpenApiInteractionValidator validator,
                                 final int port,
                                 final Integer httpsPort) {
        super(port, httpsPort);
        setupValidationListener(validator);
    }

    public ValidatedWireMockRule(final String specUrlOrPayload) {
        setupValidationListener(specUrlOrPayload);
    }

    public ValidatedWireMockRule(final OpenApiInteractionValidator validator) {
        setupValidationListener(validator);
    }

    private void setupValidationListener(final String specUrlOrPayload) {
        validationListener = new OpenApiValidationListener(specUrlOrPayload);
        addMockServiceRequestListener(validationListener);
    }

    private void setupValidationListener(final OpenApiInteractionValidator validator) {
        validationListener = new OpenApiValidationListener(validator);
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
