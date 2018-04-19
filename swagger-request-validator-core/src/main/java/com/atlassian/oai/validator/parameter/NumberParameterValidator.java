package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import io.swagger.v3.oas.models.parameters.Parameter;

import javax.annotation.Nonnull;
import java.math.BigDecimal;

public class NumberParameterValidator extends BaseNumericParameterValidator {

    public NumberParameterValidator(final MessageResolver messages) {
        super(messages);
    }

    @Override
    protected Number getNumericValue(final String value,
                                     final Parameter parameter) throws NumberFormatException {
        final String format = parameter.getSchema().getFormat();
        if ("float".equals(format)) {
            return Float.parseFloat(value);
        } else if ("double".equals(format)) {
            return Double.parseDouble(value);
        } else {
            return new BigDecimal(value);
        }
    }

    @Override
    @Nonnull
    public String supportedParameterType() {
        return "number";
    }

}
