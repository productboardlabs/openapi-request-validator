package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.models.parameters.SerializableParameter;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NumberParameterValidatorTest {

    private NumberParameterValidator classUnderTest = new NumberParameterValidator();

    @Test
    public void validate_withNullValue_shouldPassWhenNotRequired() {
        assertPass(classUnderTest.validate(null, floatParam(false)));
    }

    @Test
    public void validate_withEmptyValue_shouldPassWhenNotRequired() {
        assertPass(classUnderTest.validate("", floatParam(false)));
    }

    @Test
    public void validate_withNullValue_shouldFailWhenRequired() {
        assertFail(classUnderTest.validate(null, floatParam(true)));
    }

    @Test
    public void validate_withEmptyValue_shouldFailWhenRequired() {
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

    private static void assertFail(ValidationReport report) {
        assertThat(report.getMessages(), is(not(empty())));
    }

    private static void assertPass(ValidationReport report) {
        assertThat(report.getMessages(), is(empty()));
    }

    private static SerializableParameter floatParam() {
        return floatParam(true, null, null);
    }

    private static SerializableParameter floatParam(boolean required) {
        return floatParam(required, null, null);
    }

    private static SerializableParameter floatParam(final Double min, final Double max) {
        return floatParam(true, min, max);
    }

    private static SerializableParameter floatParam(final boolean required, final Double min, final Double max) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("number");
        when(result.getFormat()).thenReturn("float");
        when(result.getRequired()).thenReturn(required);
        when(result.getMinimum()).thenReturn(min);
        when(result.getMaximum()).thenReturn(max);
        return result;
    }
}
