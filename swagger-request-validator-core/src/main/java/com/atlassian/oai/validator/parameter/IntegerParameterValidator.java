package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MutableValidationReport;
import io.swagger.models.parameters.SerializableParameter;

import javax.annotation.Nonnull;

import static java.lang.String.format;

public class IntegerParameterValidator extends BaseParameterValidator {

    public static final ParameterValidator INSTANCE = new IntegerParameterValidator();

    @Override
    @Nonnull
    public String supportedParameterType() {
        return "integer";
    }

    @Override
    protected void doValidate(
            @Nonnull final String value,
            @Nonnull final SerializableParameter parameter,
            @Nonnull final MutableValidationReport report) {

        if (parameter.getFormat().equalsIgnoreCase("int32")) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                failFormatValidation(value, parameter, "int32", report);
                return;
            }
        } else if (parameter.getFormat().equalsIgnoreCase("int64")){
            try {
                Long.parseLong(value);
            } catch (NumberFormatException e) {
                failFormatValidation(value, parameter, "int64", report);
                return;
            }
        }

        final Long d = Long.parseLong(value);
        if (parameter.getMinimum() != null && d < parameter.getMinimum()) {
            report.addError(format("Value '%s' for parameter '%s' less than allowed min value %f",
                            value, parameter.getName(), parameter.getMinimum()));
        }

        if (parameter.getMaximum() != null && d > parameter.getMaximum()) {
            report.addError(format("Value '%s' for parameter '%s' greater than allowed max value %f",
                            value, parameter.getName(), parameter.getMaximum()));
        }
    }

    private void failFormatValidation(
            final String value,
            final SerializableParameter parameter,
            final String format,
            final MutableValidationReport report) {

        report.addError(format("Value '%s' for parameter '%s' does not match type '%s' with format '%s'",
                value, parameter.getName(), supportedParameterType(), format));
    }
}
