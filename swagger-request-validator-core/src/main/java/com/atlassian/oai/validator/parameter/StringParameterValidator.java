package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MutableValidationReport;
import io.swagger.models.parameters.SerializableParameter;

import javax.annotation.Nonnull;

public class StringParameterValidator extends BaseParameterValidator {

    public static final StringParameterValidator INSTANCE = new StringParameterValidator();

    @Override
    @Nonnull
    public String supportedParameterType() {
        return "string";
    }

    @Override
    protected void doValidate(
            @Nonnull final String value,
            @Nonnull final SerializableParameter parameter,
            @Nonnull final MutableValidationReport report) {
        // TODO: Check pattern etc.
    }
}
