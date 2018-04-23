package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ParameterGenerator.doubleParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.floatParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.intParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.requiredParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.stringParam;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

public class ParameterValidatorsTest {

    private final ParameterValidators parameterValidators = new ParameterValidators(null, new MessageResolver());

    @Test
    public void validate_withInvalidIntegerParam_shouldFail() {
        assertFail(parameterValidators.validate("1.0", intParam()), "validation.request.parameter.invalidFormat");
    }

    @Test
    public void validate_withValidIntegerParam_shouldPass() {
        assertPass(parameterValidators.validate("10", intParam()));
    }

    @Test
    public void validate_withInvalidNumberParam_shouldFail() {
        assertFail(parameterValidators.validate("1.0a", floatParam()), "validation.request.parameter.invalidFormat");
    }

    @Test
    public void validate_withValidFloatParam_shouldPass() {
        assertPass(parameterValidators.validate("1.0", floatParam()));
    }

    @Test
    public void validate_withValidDoubleParam_shouldPass() {
        assertPass(parameterValidators.validate("1.0", doubleParam()));
    }

    @Test
    public void validate_withInvalidStringParam_shouldFail() {
        assertFail(parameterValidators.validate("", stringParam()), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withValidStringParam_shouldPass() {
        assertPass(parameterValidators.validate("a", stringParam()));
    }

    @Test
    public void validate_withInvalidRequiredParm_shouldFail() {
        assertFail(parameterValidators.validate("", requiredParam()));
    }

}
