package com.atlassian.oai.validator.util;

import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.models.parameters.SerializableParameter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValidatorTestUtil {

    /**
     * Assert that validation has failed
     */
    public static void assertFail(ValidationReport report) {
        assertThat(report.getMessages(), is(not(empty())));
    }

    /**
     * Assert that validation has passed
     */
    public static void assertPass(ValidationReport report) {
        assertThat(report.getMessages(), is(empty()));
    }

    // Int parameters

    public static SerializableParameter intParam() {
        return intParam(true, null, null);
    }

    public static SerializableParameter intParam(boolean required) {
        return intParam(required, null, null);
    }

    public static SerializableParameter intParam(final Double min, final Double max) {
        return intParam(true, min, max);
    }

    public static SerializableParameter intParam(final boolean required, final Double min, final Double max) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("integer");
        when(result.getFormat()).thenReturn("int32");
        when(result.getRequired()).thenReturn(required);
        when(result.getMinimum()).thenReturn(min);
        when(result.getMaximum()).thenReturn(max);
        return result;
    }

    // String parameters

    public static SerializableParameter stringParam() {
        return stringParam(true);
    }

    public static SerializableParameter stringParam(final boolean required) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("string");
        when(result.getRequired()).thenReturn(required);
        when(result.getMinimum()).thenReturn(null);
        when(result.getMaximum()).thenReturn(null);
        return result;
    }

    // Float parameters

    public static SerializableParameter floatParam() {
        return floatParam(true, null, null);
    }

    public static SerializableParameter floatParam(boolean required) {
        return floatParam(required, null, null);
    }

    public static SerializableParameter floatParam(final Double min, final Double max) {
        return floatParam(true, min, max);
    }

    public static SerializableParameter floatParam(final boolean required, final Double min, final Double max) {
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
