package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;
import io.swagger.models.parameters.SerializableParameter;

import javax.annotation.Nonnull;

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

        final Double d = Double.parseDouble(value);
        if (parameter.getMinimum() != null && d < parameter.getMinimum()) {
            report.add(messages.get("validation.request.parameter.number.belowMin",
                    value, parameter.getName(), parameter.getMinimum())
            );
        }

        if (parameter.getMaximum() != null && d > parameter.getMaximum()) {
            report.add(messages.get("validation.request.parameter.number.aboveMax",
                    value, parameter.getName(), parameter.getMaximum())
            );
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
