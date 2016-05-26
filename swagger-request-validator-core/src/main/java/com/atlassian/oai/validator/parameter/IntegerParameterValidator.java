package com.atlassian.oai.validator.parameter;

import io.swagger.models.parameters.AbstractSerializableParameter;

import static java.lang.String.format;

public class IntegerParameterValidator extends BaseParameterValidator {

    public static final ParameterValidator INSTANCE = new IntegerParameterValidator();

    @Override
    public String supportedParameterType() {
        return "integer";
    }

    @Override
    protected void doValidate(final String value, final AbstractSerializableParameter parameter) {
        if (parameter.getFormat().equalsIgnoreCase("int32")) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                failFormatValidation(value, parameter, "int32");
            }
        } else if (parameter.getFormat().equalsIgnoreCase("int64")){
            try {
                Long.parseLong(value);
            } catch (NumberFormatException e) {
                failFormatValidation(value, parameter, "int64");
            }
        }

        final Long d = Long.parseLong(value);
        if (parameter.getMinimum() != null && d < parameter.getMinimum()) {
            throw new ValidationException(
                    format("Value '%s' for parameter '%s' less than allowed min value %f",
                            value, parameter.getName(), parameter.getMinimum()));
        }

        if (parameter.getMaximum() != null && d > parameter.getMaximum()) {
            throw new ValidationException(
                    format("Value '%s' for parameter '%s' greater than allowed max value %f",
                            value, parameter.getName(), parameter.getMaximum()));
        }
    }

    private void failFormatValidation(final String value, final AbstractSerializableParameter parameter, final String format) {
        throw new ValidationException(
                format("Value '%s' for parameter '%s' does not match type '%s' with format '%s'",
                        value, parameter.getName(), supportedParameterType(), format)
        );
    }
}
