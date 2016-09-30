package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;
import io.swagger.models.parameters.SerializableParameter;

import javax.annotation.Nonnull;

import static com.google.common.base.MoreObjects.firstNonNull;

public class NumberParameterValidator extends BaseParameterValidator {

    public NumberParameterValidator(final MessageResolver messages) {
        super(messages);
    }

    @Override
    @Nonnull
    public String supportedParameterType() {
        return "number";
    }

    @Override
    protected void doValidate(@Nonnull final String value,
                              @Nonnull final SerializableParameter parameter,
                              @Nonnull final MutableValidationReport report) {
        if (parameter.getFormat().equalsIgnoreCase("float")) {
            try {
                Float.parseFloat(value);
            } catch (NumberFormatException e) {
                failFormatValidation(value, parameter, "float", report);
                return;
            }
        } else if (parameter.getFormat().equalsIgnoreCase("double")){
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                failFormatValidation(value, parameter, "double", report);
                return;
            }
        }

        final Double doubleValue = Double.parseDouble(value);
        validateMinimum(parameter, report, doubleValue);
        validateMaximum(parameter, report, doubleValue);
    }

    private void validateMinimum(@Nonnull SerializableParameter parameter, @Nonnull MutableValidationReport report, Double value) {
        Double minimum = parameter.getMinimum();
        boolean exclusiveMinimum = firstNonNull(parameter.isExclusiveMinimum(), false);

        if (parameter.getMinimum() != null) {
            if (exclusiveMinimum && value <= minimum) {
                report.add(messages.get("validation.request.parameter.number.belowExclusiveMin",
                    value, parameter.getName(), minimum)
                );
            } else if (!exclusiveMinimum && value < minimum) {
                report.add(messages.get("validation.request.parameter.number.belowMin",
                    value, parameter.getName(), minimum)
                );
            }
        }
    }

    private void validateMaximum(@Nonnull SerializableParameter parameter, @Nonnull MutableValidationReport report, Double value) {
        Double maximum = parameter.getMaximum();
        boolean exclusiveMaximum = firstNonNull(parameter.isExclusiveMaximum(), false);

        if (parameter.getMaximum() != null) {
            if (exclusiveMaximum && value >= maximum) {
                report.add(messages.get("validation.request.parameter.number.aboveExclusiveMax",
                    value, parameter.getName(), maximum)
                );
            } else if (!exclusiveMaximum && value > maximum) {
                report.add(messages.get("validation.request.parameter.number.aboveMax",
                    value, parameter.getName(), maximum)
                );
            }
        }
    }

    private void failFormatValidation(
            final String value,
            final SerializableParameter parameter,
            final String format,
            final MutableValidationReport report) {
        report.add(messages.get("validation.request.parameter.invalidFormat",
                value, parameter.getName(), supportedParameterType(), format)
        );

    }
}
