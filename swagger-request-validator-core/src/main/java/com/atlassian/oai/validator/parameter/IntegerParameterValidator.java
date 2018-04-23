package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import io.swagger.v3.oas.models.parameters.Parameter;

import javax.annotation.Nonnull;
import java.math.BigInteger;

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
    protected Number getNumericValue(final String value,
                                     final Parameter parameter) throws NumberFormatException {
        final String format = parameter.getSchema().getFormat();
        if ("int32".equals(format)) {
            return Integer.parseInt(value);
        } else if ("int64".equals(format)) {
            return Long.parseLong(value);
        } else {
            return new BigInteger(value);
        }
    }
}
