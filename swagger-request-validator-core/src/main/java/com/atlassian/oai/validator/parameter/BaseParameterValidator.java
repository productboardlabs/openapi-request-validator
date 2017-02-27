package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;
import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.SerializableParameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

abstract class BaseParameterValidator implements ParameterValidator {

    protected final MessageResolver messages;

    protected BaseParameterValidator(@Nonnull final MessageResolver messages) {
        this.messages = requireNonNull(messages, "A message resolver is required");
    }

    @Override
    public boolean supports(@Nullable final Parameter p) {
        return p != null &&
                p instanceof SerializableParameter &&
                supportedParameterType().equalsIgnoreCase(((SerializableParameter) p).getType());
    }

    @Override
    @Nonnull
    public ValidationReport validate(@Nullable final String value, @Nullable final Parameter p) {
        final MutableValidationReport report = new MutableValidationReport();

        if (!supports(p)) {
            return report;
        }

        final SerializableParameter parameter = (SerializableParameter) p;

        if (parameter.getRequired() && (value == null || value.trim().isEmpty())) {
            return report.add(messages.get("validation.request.parameter.missing", p.getName()));
        }

        if (value == null || value.trim().isEmpty()) {
            return report;
        }

        if (!matchesEnumIfDefined(value, parameter)) {
            return report.add(
                    messages.get("validation.request.parameter.enum.invalid",
                            value, parameter.getName(), parameter.getEnum())
            );
        }

        doValidate(value, parameter, report);
        return report;
    }

    private boolean matchesEnumIfDefined(final String value, final SerializableParameter parameter) {
        return parameter.getEnum() == null ||
                parameter.getEnum().isEmpty() ||
                parameter.getEnum().stream().anyMatch(value::equals);
    }

    /**
     * Perform type-specific validations
     *
     * @param value The value being validated
     * @param parameter The parameter the value is being validated against
     * @param validationReport The report to accumulate validation errors
     */
    protected abstract void doValidate(
            @Nonnull String value,
            @Nonnull SerializableParameter parameter,
            @Nonnull MutableValidationReport validationReport);
}
