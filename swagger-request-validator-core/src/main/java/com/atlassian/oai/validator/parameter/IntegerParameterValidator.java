package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;
import io.swagger.models.parameters.SerializableParameter;

import javax.annotation.Nonnull;

public class IntegerParameterValidator extends BaseNumericParameterValidator {

    public IntegerParameterValidator(final MessageResolver messages) {
        super(messages);
    }

    @Override
    @Nonnull
    public String supportedParameterType() {
        return "integer";
    }

    @Override
    protected Number getNumericValue(String value, SerializableParameter parameter) throws NumberFormatException {
        if (parameter.getFormat().equalsIgnoreCase("int32")) {
            return Integer.parseInt(value);
        } else if (parameter.getFormat().equalsIgnoreCase("int64")) {
            return Long.parseLong(value);
        } else {
            throw new IllegalArgumentException(parameter.getFormat() + " is not a valid integer format");
        }
    }
}
