package com.atlassian.oai.validator.parameter;

import io.swagger.models.parameters.Parameter;

import java.util.List;

import static java.util.Arrays.asList;

public class ParameterValidators {

    private static final List<ParameterValidator> VALIDATORS = asList(
            StringParameterValidator.INSTANCE,
            NumberParameterValidator.INSTANCE,
            IntegerParameterValidator.INSTANCE
    );

    public static void validate(String value, Parameter parameter) {
        VALIDATORS.stream().filter(v -> v.supports(parameter)).forEach(v -> v.validate(value, parameter));
    }

}
