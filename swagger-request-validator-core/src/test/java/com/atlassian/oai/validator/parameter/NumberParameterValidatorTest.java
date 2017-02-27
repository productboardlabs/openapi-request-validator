package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.floatParam;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.floatParamFormat;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.floatParamMultipleOf;

public class NumberParameterValidatorTest {

    private NumberParameterValidator classUnderTest = new NumberParameterValidator(new MessageResolver());

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
        assertFail(classUnderTest.validate(null, floatParam(true)), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate("", floatParam(true)), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withNonNumericValue_shouldFail() {
        assertFail(classUnderTest.validate("not-a-Number", floatParam()), "validation.request.parameter.invalidFormat");
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
        assertFail(classUnderTest.validate("1.1", floatParam(null, 1.0)),
                "validation.request.parameter.number.aboveMax");
    }

    @Test
    public void validate_withValueEqualToMax_shouldFail_ifExclusiveMaxSpecified() {
        assertFail(classUnderTest.validate("1.0", floatParam(null, 1.0, null, true)),
            "validation.request.parameter.number.aboveExclusiveMax");
    }

    @Test
    public void validate_withValueEqualToMin_shouldFail_ifExclusiveMinSpecified() {
        assertFail(classUnderTest.validate("1.0", floatParam(1.0, null, true, null)),
            "validation.request.parameter.number.belowExclusiveMin");
    }

    @Test
    public void validate_withValueLessThanMin_shouldFail_ifMinSpecified() {
        assertFail(classUnderTest.validate("0.9", floatParam(1.0, null)),
                "validation.request.parameter.number.belowMin");
    }

    @Test
    public void validate_withValueInRange_shouldPass() {
        assertPass(classUnderTest.validate("1.1", floatParam(1.0, 1.2)));
    }

    @Test
    public void validate_withValueNotMultipleOf_shouldFail() {
        assertFail(classUnderTest.validate("1.6", floatParamMultipleOf(0.5f)),
            "validation.request.parameter.number.multipleOf");
    }

    @Test
    public void validate_withValueMultipleOf_shouldPass() {
        assertPass(classUnderTest.validate("1.5", floatParamMultipleOf(0.5f)));
    }

    @Test
    public void validate_withFormatNull_shouldPass() {
        assertPass(classUnderTest.validate("25", floatParamFormat(null)));
    }

    @Test
    public void validate_withFormatUnknown_shouldPass() {
        assertPass(classUnderTest.validate("25", floatParamFormat("unknown")));
    }

    @Test
    public void validate_withNonNumericValueFormatUnknown_shouldFail() {
        assertFail(classUnderTest.validate("not-a-Number", floatParamFormat("unknown")),
            "validation.request.parameter.invalidFormat");
    }
}
