package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.MessageContext;
import io.swagger.v3.oas.models.parameters.Parameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;

abstract class BaseParameterValidator implements ParameterValidator {

    protected final MessageResolver messages;

    protected BaseParameterValidator(@Nonnull final MessageResolver messages) {
        this.messages = requireNonNull(messages, "A message resolver is required");
    }

    @Override
    public boolean supports(@Nullable final Parameter p) {
        return p != null &&
                p.getSchema() != null &&
                supportedParameterType().equalsIgnoreCase(p.getSchema().getType());
    }

    @Override
    @Nonnull
    public ValidationReport validate(@Nullable final String value, @Nullable final Parameter parameter) {
        if (!supports(parameter)) {
            return ValidationReport.empty();
        }

        final MessageContext context = MessageContext.create().withParameter(parameter).build();

        if (TRUE.equals(parameter.getRequired()) && (value == null || value.trim().isEmpty())) {
            return ValidationReport.singleton(
                    messages.get("validation.request.parameter.missing", parameter.getName())
            ).withAdditionalContext(context);
        }

        if (value == null || value.trim().isEmpty()) {
            return ValidationReport.empty();
        }

        if (!matchesEnumIfDefined(value, parameter)) {
            return ValidationReport.singleton(
                    messages.get("validation.request.parameter.enum.invalid",
                            value, parameter.getName(), parameter.getSchema().getEnum())
            ).withAdditionalContext(context);
        }

        return doValidate(value, parameter).withAdditionalContext(context);
    }

    private boolean matchesEnumIfDefined(final String value, final Parameter parameter) {
        // TODO: Fix validation to match on proper value type
        return parameter.getSchema().getEnum() == null ||
                parameter.getSchema().getEnum().isEmpty() ||
                parameter.getSchema().getEnum().stream().anyMatch(value::equals);
    }

    /**
     * Perform type-specific validations and return a validation report with accumulated errors
     *
     * @param value     The value being validated
     * @param parameter The parameter the value is being validated against
     */
    protected abstract ValidationReport doValidate(@Nonnull String value, @Nonnull Parameter parameter);
}
