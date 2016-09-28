package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.SerializableParameter;
import org.junit.Before;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.stringParam;

public class StringParameterValidatorTest {

    private StringParameterValidator classUnderTest = new StringParameterValidator(new MessageResolver());
    private SerializableParameter parameter;

    @Before
    public void init() {
        parameter = new FormParameter();
        parameter.setType("string");
    }

    @Test
    public void validate_withNullValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate(null, stringParam(false)));
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate("", stringParam(false)));
    }

    @Test
    public void validate_withNullValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate(null, stringParam(true)), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate("", stringParam(true)), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withPattern_shouldFail_whenNoMatch() {
        parameter.setPattern("[a-z]*");
        assertFail(classUnderTest.validate("NO_CAPS_ALLOWED", parameter), "validation.request.parameter.string.patternMismatch");
    }

    @Test
    public void validate_withPattern_shouldPass_whenMatch() {
        parameter.setPattern("[a-z]*");
        assertPass(classUnderTest.validate("allgood", parameter));
    }

    @Test
    public void validate_withMinLength_shouldFail_whenTooShort() {
        parameter.setMinLength(6);
        assertFail(classUnderTest.validate("short", parameter), "validation.request.parameter.string.tooShort");
    }

    @Test
    public void validate_withMinLength_shouldPass_whenLongEnough() {
        parameter.setMinLength(6);
        assertPass(classUnderTest.validate("longer", parameter));
    }

    @Test
    public void validate_withMaxLength_shouldFail_whenTooLong() {
        parameter.setMaxLength(10);
        assertFail(classUnderTest.validate("far too long for my taste", parameter), "validation.request.parameter.string.tooLong");
    }

    @Test
    public void validate_withMaxLength_shouldPass_whenShortEnough() {
        parameter.setMaxLength(30);
        assertPass(classUnderTest.validate("easily short enough", parameter));
    }

    @Test
    public void validate_withDateFormat_shouldFail_whenNotAValidISODate() {
        parameter.setFormat("date");
        assertFail(classUnderTest.validate("2016--5dd", parameter), "validation.request.parameter.string.date.invalid");
    }

    @Test
    public void validate_withDateFormat_shouldPass_whenAValidISODate() {
        parameter.setFormat("date");
        assertPass(classUnderTest.validate("2016-09-28", parameter));
    }

}
