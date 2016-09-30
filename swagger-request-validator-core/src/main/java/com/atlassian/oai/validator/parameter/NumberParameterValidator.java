package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;
import io.swagger.models.parameters.SerializableParameter;

import javax.annotation.Nonnull;

import static com.google.common.base.MoreObjects.firstNonNull;

public class NumberParameterValidator extends BaseNumericParameterValidator {

    public NumberParameterValidator(final MessageResolver messages) {
        super(messages);
    }

    @Override
    protected Number getNumericValue(String value, SerializableParameter parameter) throws NumberFormatException {
        if (parameter.getFormat().equalsIgnoreCase("float")) {
            return Float.parseFloat(value);
        } else if (parameter.getFormat().equalsIgnoreCase("double")) {
            return Double.parseDouble(value);
        } else {
            throw new IllegalArgumentException(parameter.getFormat() + " is not a valid number format");
        }

    }

    @Override
    @Nonnull
    public String supportedParameterType() {
        return "number";
    }

}
