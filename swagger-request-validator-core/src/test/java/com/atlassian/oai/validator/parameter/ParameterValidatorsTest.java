package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.floatParam;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.intParam;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.stringParam;

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
    public void validate_withValidNumberParam_shouldPass() {
        assertPass(parameterValidators.validate("1.0", floatParam()));
    }

    @Test
    public void validate_withInvalidStringParam_shouldFail() {
        assertFail(parameterValidators.validate("", stringParam()), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withValidStringParam_shouldPass() {
        assertPass(parameterValidators.validate("a", stringParam()));
    }

}
