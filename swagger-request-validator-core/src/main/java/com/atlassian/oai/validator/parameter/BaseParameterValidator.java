package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MutableValidationReport;
import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.SerializableParameter;

import javax.annotation.Nonnull;

import static java.lang.String.format;

public abstract class BaseParameterValidator implements ParameterValidator {

    @Override
    public boolean supports(Parameter p) {
        return p != null &&
                p instanceof SerializableParameter &&
                supportedParameterType().equalsIgnoreCase(((SerializableParameter)p).getType());
    }

    @Override
    public ValidationReport validate(final String value, final Parameter p) {
        final MutableValidationReport report = new MutableValidationReport();

        if (!supports(p)) {
            return report;
        }

        final SerializableParameter parameter = (SerializableParameter)p;

        if (parameter.getRequired() && (value == null || value.trim().isEmpty())) {
            report.addError(format("Parameter '%s' is required but is missing", p.getName()));
            return report;
        }

        if (value == null || value.trim().isEmpty()) {
            return report;
        }

        if (parameter.getEnum() != null &&
                !parameter.getEnum().isEmpty() &&
                !parameter.getEnum().stream().anyMatch(value::equals)) {
            report.addError(format("Parameter '%s' does not match allowed values <%s>",
                    parameter.getName(), parameter.getEnum()));
            return report;
        }

        doValidate(value, parameter, report);
        return report;
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
