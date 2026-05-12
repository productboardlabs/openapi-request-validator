package com.atlassian.oai.validator.pact;

import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.ProviderInfo;
import au.com.dius.pact.core.model.BasePact;
import au.com.dius.pact.core.model.Interaction;
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.List;

/**
 * A JUnit5 extension that wraps {@link PactConsumerTestExt} and additionally validates the consumer
 * expectations against an API specification for the Provider API.
 * <p>
 * This gives consumers fast feedback if their expectations fail to meet the format expected by the
 * Provider API.
 * <p>
 * Usage: Register this extension instead of (or in place of) {@code @ExtendWith(PactConsumerTestExt.class)},
 * and annotate your test class with {@code @PactTestFor} as usual.
 */
public class ValidatedPactConsumerTestExtension implements Extension, BeforeTestExecutionCallback,
        BeforeAllCallback, AfterTestExecutionCallback,
        AfterAllCallback, ParameterResolver {

    /**
     * The namespace used by {@link PactConsumerTestExt} to store pact data in the extension context.
     */
    private static final ExtensionContext.Namespace PACT_NAMESPACE =
            ExtensionContext.Namespace.create("pact-jvm");

    private final PactConsumerTestExt delegate;
    private final OpenApiInteractionValidator validator;

    /**
     * Create an extension that validates pact interactions against the given OpenAPI spec URL or payload.
     *
     * @param specUrlOrPayload the URL or inline payload of the OpenAPI spec to validate against
     * @param basePathOverride an optional base path override, or {@code null} to use the spec's base path
     */
    public ValidatedPactConsumerTestExtension(final String specUrlOrPayload,
                                              final @Nullable String basePathOverride) {
        this(
                OpenApiInteractionValidator
                        .createFor(specUrlOrPayload)
                        .withLevelResolver(PactLevelResolverFactory.create())
                        .withBasePathOverride(basePathOverride)
                        .build()
        );
    }

    /**
     * Create an extension that validates pact interactions against the given pre-configured validator.
     *
     * @param validator the pre-configured {@link OpenApiInteractionValidator} to use
     */
    public ValidatedPactConsumerTestExtension(final OpenApiInteractionValidator validator) {
        this(validator, new PactConsumerTestExt());
    }

    /**
     * Create an extension with a custom delegate and validator (primarily for testing).
     *
     * @param validator the pre-configured {@link OpenApiInteractionValidator} to use
     * @param delegate  the {@link PactConsumerTestExt} delegate to wrap
     */
    public ValidatedPactConsumerTestExtension(final OpenApiInteractionValidator validator,
                                              final PactConsumerTestExt delegate) {
        this.validator = validator;
        this.delegate = delegate;
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        delegate.beforeAll(context);
    }

    /**
     * Runs pact setup via the delegate (which populates the pact in the store), then validates
     * the pact interactions against the OpenAPI spec before the test body executes.
     * <p>
     * If the test method is annotated with {@link IgnoreApiValidation}, validation is skipped.
     */
    @Override
    public void beforeTestExecution(final ExtensionContext context) throws Exception {
        delegate.beforeTestExecution(context);

        final boolean ignore = context.getTestMethod()
                .map(m -> AnnotationSupport.isAnnotated(m, IgnoreApiValidation.class))
                .orElse(false);
        if (!ignore) {
            validatePacts(context);
        }
    }

    @Override
    public void afterTestExecution(final ExtensionContext context) {
        delegate.afterTestExecution(context);
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        delegate.afterAll(context);
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext,
                                     final ExtensionContext extensionContext) throws ParameterResolutionException {
        return delegate.supportsParameter(parameterContext, extensionContext);
    }

    @Override
    public @Nullable Object resolveParameter(final ParameterContext parameterContext,
                                             final ExtensionContext extensionContext) throws ParameterResolutionException {
        return delegate.resolveParameter(parameterContext, extensionContext);
    }

    @SuppressWarnings("unchecked")
    private void validatePacts(final ExtensionContext context) {
        final ExtensionContext.Store store = context.getStore(PACT_NAMESPACE);

        // Retrieve the list of providers set up by the delegate during beforeTestExecution
        final List<Object[]> providers = (List<Object[]>) store.get("providers");
        if (providers == null) {
            return;
        }

        ValidationReport combinedReport = ValidationReport.empty();

        for (final Object entry : providers) {
            // Each entry is a Kotlin Pair<ProviderInfo, List<String>>
            final ProviderInfo providerInfo;
            if (entry instanceof kotlin.Pair) {
                final Object first = ((kotlin.Pair<?, ?>) entry).getFirst();
                if (!(first instanceof ProviderInfo)) {
                    continue;
                }
                providerInfo = (ProviderInfo) first;
            } else {
                continue;
            }

            final BasePact pact = (BasePact) store.get("pact:" + providerInfo.getProviderName());
            if (pact == null) {
                continue;
            }

            final ValidationReport report = pact.getInteractions()
                    .stream()
                    .filter(i -> i.asSynchronousRequestResponse() != null)
                    .map(Interaction::asSynchronousRequestResponse)
                    .map(i -> validator.validate(PactRequest.of(i.getRequest()), PactResponse.of(i.getResponse())))
                    .reduce(ValidationReport.empty(), ValidationReport::merge);

            combinedReport = combinedReport.merge(report);
        }

        if (combinedReport.hasErrors()) {
            throw new PactValidationError(combinedReport);
        }
    }

    /**
     * Thrown when pact interactions fail validation against the OpenAPI spec.
     */
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
