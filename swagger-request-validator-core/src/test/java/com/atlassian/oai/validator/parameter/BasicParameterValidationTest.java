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

public class BasicParameterValidationTest {

    private final ParameterValidator parameterValidator = new ParameterValidator(new MessageResolver());

    @Test
    public void validate_withInvalidIntegerParam_shouldFail() {
        assertFail(parameterValidator.validate("1.0", intParam()), "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_withValidIntegerParam_shouldPass() {
        assertPass(parameterValidator.validate("10", intParam()));
    }

    @Test
    public void validate_withInvalidNumberParam_shouldFail() {
        assertFail(parameterValidator.validate("1.0a", floatParam()), "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_withValidFloatParam_shouldPass() {
        assertPass(parameterValidator.validate("1.0", floatParam()));
    }

    @Test
    public void validate_withValidDoubleParam_shouldPass() {
        assertPass(parameterValidator.validate("1.0", doubleParam()));
    }

    @Test
    public void validate_withInvalidStringParam_shouldFail() {
        assertFail(parameterValidator.validate("", stringParam()), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withValidStringParam_shouldPass() {
        assertPass(parameterValidator.validate("a", stringParam()));
    }

    @Test
    public void validate_withInvalidRequiredParm_shouldFail() {
        assertFail(parameterValidator.validate("", requiredParam()));
    }

}
