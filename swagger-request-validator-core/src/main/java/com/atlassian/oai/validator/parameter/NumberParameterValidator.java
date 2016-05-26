package com.atlassian.oai.validator.parameter;

import io.swagger.models.parameters.AbstractSerializableParameter;

import static java.lang.String.format;

public class NumberParameterValidator extends BaseParameterValidator {

    public static final NumberParameterValidator INSTANCE = new NumberParameterValidator();

    @Override
    public String supportedParameterType() {
        return "number";
    }

    @Override
    protected void doValidate(final String value, final AbstractSerializableParameter parameter) {
        if (parameter.getFormat().equalsIgnoreCase("float")) {
            try {
                Float.parseFloat(value);
            } catch (NumberFormatException e) {
                failFormatValidation(value, parameter, "float");
            }
        } else if (parameter.getFormat().equalsIgnoreCase("double")){
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                failFormatValidation(value, parameter, "double");
            }
        }

        final Double d = Double.parseDouble(value);
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
