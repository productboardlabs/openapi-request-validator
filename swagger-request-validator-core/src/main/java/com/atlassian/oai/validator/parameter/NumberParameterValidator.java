package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;
import io.swagger.models.parameters.SerializableParameter;

import java.math.BigDecimal;
import javax.annotation.Nonnull;

import static com.google.common.base.MoreObjects.firstNonNull;

public class NumberParameterValidator extends BaseNumericParameterValidator {

    public NumberParameterValidator(final MessageResolver messages) {
        super(messages);
    }

    @Override
    protected Number getNumericValue(String value, SerializableParameter parameter) throws NumberFormatException {
        String format = parameter.getFormat();
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
