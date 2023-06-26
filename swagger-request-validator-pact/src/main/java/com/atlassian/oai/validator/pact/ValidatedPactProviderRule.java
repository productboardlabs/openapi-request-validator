package com.atlassian.oai.validator.pact;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * An extension to the PactProviderRule that additionally validates the consumer expectations against an
 * API specification for the Provider API.
 * <p>
 * This gives consumers fast feedback if their expectations fail to meet the format expected by the Provider API.
 */
public class ValidatedPactProviderRule implements TestRule {
    private final PactProviderRule delegate;
    private final String providerId;
    private final Object target;
    private final OpenApiInteractionValidator validator;

    public ValidatedPactProviderRule(final String specUrlOrPayload,
                                     final String basePathOverride,
                                     final String providerId,
                                     final Object target) {
        this(specUrlOrPayload, basePathOverride, providerId, target, new PactProviderRule(providerId, target));
    }

    public ValidatedPactProviderRule(final String specUrlOrPayload,
                                     final String basePathOverride,
                                     final String providerId,
                                     final String host,
                                     final Integer port,
                                     final Object target) {
        this(specUrlOrPayload, basePathOverride, providerId, target, new PactProviderRule(providerId, host, port, target));
    }

    private ValidatedPactProviderRule(final String specUrlOrPayload,
                                      final String basePathOverride,
                                      final String providerId,
                                      final Object target,
                                      final PactProviderRule delegate) {
        validator = OpenApiInteractionValidator
                .createFor(specUrlOrPayload)
                .withLevelResolver(PactLevelResolverFactory.create())
                .withBasePathOverride(basePathOverride)
                .build();

        this.providerId = providerId;
        this.target = target;
        this.delegate = delegate;
    }

    public ValidatedPactProviderRule(final OpenApiInteractionValidator validator,
                                     final String providerId,
                                     final Object target) {
        this.validator = validator;
        this.providerId = providerId;
        this.target = target;
        delegate = new PactProviderRule(providerId, target);
    }

    public ValidatedPactProviderRule(final OpenApiInteractionValidator validator,
                                     final String providerId,
                                     final String host,
                                     final Integer port,
                                     final Object target) {
        this.validator = validator;
        this.providerId = providerId;
        this.target = target;
        delegate = new PactProviderRule(providerId, host, port, target);
    }

    public MockProviderConfig getConfig() {
        return delegate.getConfig();
    }

    public MockServer getMockServer() {
        return delegate.getMockServer();
    }

    public String getUrl() {
        return getMockServer().getUrl();
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final PactVerification pactDef = description.getAnnotation(PactVerification.class);
                final IgnoreApiValidation ignore = description.getAnnotation(IgnoreApiValidation.class);
                if (pactDef != null && ignore == null) {
                    validatePactDef(pactDef);
                }
                delegate.apply(base, description).evaluate();
            }
        };
    }

    private void validatePactDef(final PactVerification pactVerification) throws Exception {
        final Optional<RequestResponsePact> requestResponsePact = getRequestResponsePact(pactVerification);
        if (!requestResponsePact.isPresent()) {
            return;
        }

        final ValidationReport report = requestResponsePact.get()
                .getInteractions()
                .stream()
                .map(Interaction::asSynchronousRequestResponse)
                .map(i -> validator.validate(PactRequest.of(i.getRequest()), PactResponse.of(i.getResponse())))
                .reduce(ValidationReport.empty(), ValidationReport::merge);

        if (report.hasErrors()) {
            throw new PactValidationError(report);
        }
    }

    private Optional<RequestResponsePact> getRequestResponsePact(final PactVerification pactVerification) throws Exception {
        final Optional<Method> possiblePactMethod = findPactMethod(pactVerification);
        if (!possiblePactMethod.isPresent()) {
            // Fail silently and let the delegate Pact rule do error reporting
            return Optional.empty();
        }

        final Method method = possiblePactMethod.get();
        final Pact pact = method.getAnnotation(Pact.class);
        final PactDslWithProvider dslBuilder = ConsumerPactBuilder.consumer(pact.consumer()).hasPactWith(providerId);

        return Optional.of((RequestResponsePact) method.invoke(target, dslBuilder));

    }

    private Optional<Method> findPactMethod(final PactVerification pactVerification) {
        final String pactFragment = pactVerification.fragment();
        for (final Method method : target.getClass().getMethods()) {
            final Pact pact = method.getAnnotation(Pact.class);
            if (pact != null && pact.provider().equals(providerId)
                    && (pactFragment.isEmpty() || pactFragment.equals(method.getName()))) {

                final boolean hasValidPactSignature =
                        RequestResponsePact.class.isAssignableFrom(method.getReturnType())
                                && method.getParameterTypes().length == 1
                                && method.getParameterTypes()[0].isAssignableFrom(PactDslWithProvider.class);

                if (!hasValidPactSignature) {
                    return Optional.empty();
                }
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    public static class PactValidationError extends RuntimeException {
        private final ValidationReport report;

        public PactValidationError(final ValidationReport report) {
            super(JsonValidationReportFormat.getInstance().apply(report));
            this.report = report;
        }

        /**
         * @return The validation report that generated this exception
         */
        public ValidationReport getValidationReport() {
            return report;
        }
    }
}
