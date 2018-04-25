package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ParameterGenerator.enumeratedIntParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.intParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.intParamFormat;
import static com.atlassian.oai.validator.util.ParameterGenerator.intParamMultipleOf;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

public class IntegerParameterValidatorTest {

    private final IntegerParameterValidator classUnderTest = new IntegerParameterValidator(new MessageResolver());

    @Test
    public void validate_withNullValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate(null, intParam(false)));
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate("", intParam(false)));
    }

    @Test
    public void validate_withNullValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate(null, intParam(true)), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate("", intParam(true)), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withNonNumericValue_shouldFail() {
        assertFail(classUnderTest.validate("123a", intParam()), "validation.request.parameter.invalidFormat");
    }

    @Test
    public void validate_withNonIntegerValue_shouldFail() {
        assertFail(classUnderTest.validate("123.1", intParam()), "validation.request.parameter.invalidFormat");
    }

    @Test
    public void validate_withIntegerValue_shouldPass() {
        assertPass(classUnderTest.validate("123", intParam()));
    }

    @Test
    public void validate_withValueGreaterThanMax_shouldFail_ifMaxSpecified() {
        assertFail(classUnderTest.validate("2", intParam(null, 1.0)), "validation.request.parameter.number.aboveMax");
    }

    @Test
    public void validate_withValueLessThanMin_shouldFail_ifMinSpecified() {
        assertFail(classUnderTest.validate("0", intParam(1.0, null)), "validation.request.parameter.number.belowMin");
    }

    @Test
    public void validate_withValueInRange_shouldPass() {
        assertPass(classUnderTest.validate("2", intParam(1.0, 3.0)));
    }

    @Test
    public void validate_withValueEqualToMax_shouldFail_ifExclusiveMaxSpecified() {
        assertFail(classUnderTest.validate("1", intParam(null, 1.0, null, true)),
            "validation.request.parameter.number.aboveExclusiveMax");
    }

    @Test
    public void validate_withValueEqualToMin_shouldFail_ifExclusiveMinSpecified() {
        assertFail(classUnderTest.validate("1", intParam(1.0, null, true, null)),
            "validation.request.parameter.number.belowExclusiveMin");
    }

    @Test
    public void validate_withValueNotMultipleOf_shouldFail() {
        assertFail(classUnderTest.validate("17", intParamMultipleOf(5)),
            "validation.request.parameter.number.multipleOf");
    }

    @Test
    public void validate_withValueMultipleOf_shouldPass() {
        assertPass(classUnderTest.validate("25", intParamMultipleOf(5)));
    }

    @Test
    public void validate_withFormatNull_shouldPass() {
        assertPass(classUnderTest.validate("25", intParamFormat(null)));
    }

    @Test
    public void validate_withFormatUnknown_shouldPass() {
        assertPass(classUnderTest.validate("25", intParamFormat("unknown")));
    }

    @Test
    public void validate_withNonIntegerValueFormatUnknown_shouldFail() {
        assertFail(classUnderTest.validate("123.1", intParamFormat("unknown")),
                "validation.request.parameter.invalidFormat");
    }

    @Test
    public void validate_withValidEnumeratedValue_shouldPass() {
        assertPass(classUnderTest.validate("3", enumeratedIntParam(1, 2, 3)));
    }

    @Test
    public void validate_withInvalidEnumeratedValue_shouldFail() {
        assertFail(classUnderTest.validate("4", enumeratedIntParam(1, 2, 3)),
                "validation.request.parameter.enum.invalid");
    }
}
