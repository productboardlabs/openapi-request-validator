package com.atlassian.oai.validator.parameter;

import io.swagger.models.parameters.AbstractSerializableParameter;

public class StringParameterValidator extends BaseParameterValidator {

    public static final StringParameterValidator INSTANCE = new StringParameterValidator();

    @Override
    public String supportedParameterType() {
        return "string";
    }

    @Override
    protected void doValidate(String value, AbstractSerializableParameter parameter) {
        // TODO: Check pattern etc.
    }
}
