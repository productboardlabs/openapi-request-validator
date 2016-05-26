package com.atlassian.oai.validator.parameter;

import io.swagger.models.parameters.Parameter;

public interface ParameterValidator {

    String supportedParameterType();

    boolean supports(Parameter p);

    void validate(String value, Parameter p);

    class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

}
