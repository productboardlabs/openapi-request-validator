package com.atlassian.oai.validator.parameter;

import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.floatParam;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.intParam;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.stringParam;

public class ParameterValidatorsTest {

    private final ParameterValidators parameterValidators = new ParameterValidators(null);

    @Test
    public void validate_withInvalidIntegerParam_shouldFail() {
        assertFail(parameterValidators.validate("1.0", intParam()));
    }

    @Test
    public void validate_withValidIntegerParam_shouldPass() {
        assertPass(parameterValidators.validate("10", intParam()));
    }

    @Test
    public void validate_withInvalidNumberParam_shouldFail() {
        assertFail(parameterValidators.validate("1.0a", floatParam()));
    }

    @Test
    public void validate_withValidNumberParam_shouldPass() {
        assertPass(parameterValidators.validate("1.0", floatParam()));
    }

    @Test
    public void validate_withInvalidStringParam_shouldFail() {
        assertFail(parameterValidators.validate("", stringParam()));
    }

    @Test
    public void validate_withValidStringParam_shouldPass() {
        assertPass(parameterValidators.validate("a", stringParam()));
    }

}
