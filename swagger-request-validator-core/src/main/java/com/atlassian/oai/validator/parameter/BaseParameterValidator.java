package com.atlassian.oai.validator.parameter;

import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.models.parameters.Parameter;

import static java.lang.String.format;

public abstract class BaseParameterValidator implements ParameterValidator {

    @Override
    public boolean supports(Parameter p) {
        return p != null &&
                p instanceof AbstractSerializableParameter &&
                supportedParameterType().equalsIgnoreCase(((AbstractSerializableParameter)p).getType());
    }

    @Override
    public void validate(String value, Parameter p) {
        if (!supports(p)) {
            return;
        }

        final AbstractSerializableParameter parameter = (AbstractSerializableParameter)p;

        if (parameter.getRequired() && (value == null || value.trim().isEmpty())) {
            throw new ValidationException(format("Parameter '%s' is required but is missing", p.getName()));
        }

        if (parameter.getEnum() != null &&
                !parameter.getEnum().isEmpty() &&
                !parameter.getEnum().stream().anyMatch(value::equals)) {
            throw new ValidationException(
                    format("Parameter '%s' does not match allowed values <%s>", parameter.getName(), parameter.getEnum()));
        }

        doValidate(value, parameter);
    }

    protected abstract void doValidate(String value, AbstractSerializableParameter parameter);
}
