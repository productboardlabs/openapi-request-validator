package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;
import io.swagger.models.parameters.SerializableParameter;

import java.math.BigInteger;
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
        String format = parameter.getFormat();
        if ("int32".equals(format)) {
            return Integer.parseInt(value);
        } else if ("int64".equals(format)) {
            return Long.parseLong(value);
        } else {
            return new BigInteger(value);
        }
    }
}
