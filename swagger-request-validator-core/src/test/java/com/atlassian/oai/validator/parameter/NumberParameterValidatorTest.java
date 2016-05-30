package com.atlassian.oai.validator.parameter;

import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.floatParam;

public class NumberParameterValidatorTest {

    private NumberParameterValidator classUnderTest = new NumberParameterValidator();

    @Test
    public void validate_withNullValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate(null, floatParam(false)));
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate("", floatParam(false)));
    }

    @Test
    public void validate_withNullValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate(null, floatParam(true)));
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate("", floatParam(true)));
    }

    @Test
    public void validate_withNonNumericValue_shouldFail() {
        assertFail(classUnderTest.validate("not-a-Number", floatParam()));
    }

    @Test
    public void validate_withPositiveNumericValue_shouldPass() {
        assertPass(classUnderTest.validate("123.456", floatParam()));
    }

    @Test
    public void validate_withNegativeNumericValue_shouldPass() {
        assertPass(classUnderTest.validate("-123.456", floatParam()));
    }

    @Test
    public void validate_withValueGreaterThanMax_shouldFail_ifMaxSpecified() {
        assertFail(classUnderTest.validate("1.1", floatParam(null, 1.0)));
    }

    @Test
    public void validate_withValueLessThanMin_shouldFail_ifMinSpecified() {
        assertFail(classUnderTest.validate("0.9", floatParam(1.0, null)));
    }

    @Test
    public void validate_withValueInRange_shouldPass() {
        assertPass(classUnderTest.validate("1.1", floatParam(1.0, 1.2)));
    }
}
