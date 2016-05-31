package com.atlassian.oai.validator.pact;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRule;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.PactFragment;
import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReportFormatter;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * An extension to the PactProviderRule that additionally validates the consumer expectations against an
 * API specification for the Provider API.
 *
 * This gives consumers fast feedback if their expectations fail to meet the format expected by the Provider API.
 */
public class ValidatedPactProviderRule implements TestRule {
    private final PactProviderRule delegate;
    private final String providerId;
    private final Object target;
    private final SwaggerRequestResponseValidator validator;

    public ValidatedPactProviderRule(final String swaggerJsonUrl, final String basePathOverride, final String providerId, final Object target) {
        this.delegate = new PactProviderRule(providerId, target);
        this.providerId = providerId;
        this.target = target;

        this.validator = new SwaggerRequestResponseValidator(swaggerJsonUrl, basePathOverride);
    }

    public MockProviderConfig getConfig() {
        return delegate.getConfig();
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
        final Optional<PactFragment> pactFragment = getPactFragment(pactVerification);
        if (!pactFragment.isPresent()) {
            return;
        }

        final ValidationReport report = pactFragment.get().toPact()
                .getInteractions()
                .stream()
                .map(i -> validator.validate(new PactRequest(i.getRequest()), new PactResponse(i.getResponse())))
                .reduce(ValidationReport.empty(), ValidationReport::merge);

        if (report.hasErrors()) {
            throw new PactValidationError(ValidationReportFormatter.toString(report));
        }
    }

    private Optional<PactFragment> getPactFragment(final PactVerification pactVerification) throws Exception {
        final Optional<Method> possiblePactMethod = findPactMethod(pactVerification);
        if (!possiblePactMethod.isPresent()) {
            // Fail silently and let the delegate Pact rule do error reporting
            return Optional.empty();
        }

        final Method method = possiblePactMethod.get();
        final Pact pact = method.getAnnotation(Pact.class);
        final PactDslWithProvider dslBuilder = ConsumerPactBuilder.consumer(pact.consumer()).hasPactWith(providerId);
        try {
            return Optional.of((PactFragment) method.invoke(target, dslBuilder));
        } catch (Exception e) {
            throw e;
        }
    }

    private Optional<Method> findPactMethod(PactVerification pactVerification) {
        final String pactFragment = pactVerification.fragment();
        for (Method method : target.getClass().getMethods()) {
            final Pact pact = method.getAnnotation(Pact.class);
            if (pact != null && pact.provider().equals(providerId)
                    && (pactFragment.isEmpty() || pactFragment.equals(method.getName()))) {

                boolean hasValidPactSignature =
                        PactFragment.class.isAssignableFrom(method.getReturnType())
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
        public PactValidationError(String message) {
            super(message);
        }
    }
}