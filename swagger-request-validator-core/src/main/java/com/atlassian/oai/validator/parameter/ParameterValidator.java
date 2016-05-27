package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.models.parameters.Parameter;

public interface ParameterValidator {

    String supportedParameterType();

    boolean supports(Parameter p);

    ValidationReport validate(String value, Parameter p);

}
