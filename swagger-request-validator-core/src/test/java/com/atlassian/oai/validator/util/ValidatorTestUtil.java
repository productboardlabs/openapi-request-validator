package com.atlassian.oai.validator.util;

import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReportFormatter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValidatorTestUtil {

    private ValidatorTestUtil() { }

    private static final Logger log = LoggerFactory.getLogger(ValidatorTestUtil.class);

    /**
     * Assert that validation has failed.
     */
    public static void assertFail(final ValidationReport report, final String... expectedKeys) {
        log.trace(ValidationReportFormatter.format(report));
        assertThat(report.getMessages(), is(not(empty())));

        final List<String> foundKeys = report.getMessages().stream().map(ValidationReport.Message::getKey).collect(toList());

        for (String key : expectedKeys) {
            assertThat(format("Expected message key '%s' but not found. Found <%s>.", key, foundKeys.toString()),
                foundKeys.contains(key), is(true));
        }

    }

    /**
     * Assert that validation has passed.
     */
    public static void assertPass(final ValidationReport report) {
        assertTrue(report.getMessages().isEmpty() ||
            report.getMessages().stream().allMatch(m -> m.getLevel() == ValidationReport.Level.IGNORE));
    }

    /**
     * Load a response JSON file with the given name.
     *
     * @param responseName The name of the response to load
     * @return The response JSON as a String, or <code>null</code> if it cannot be loaded
     */
    public static String loadResponse(final String responseName) {
        return loadResource("/responses/" + responseName + ".json");
    }

    /**
     * Load a request JSON file with the given name.
     *
     * @param requestName The name of the request to load
     * @return The response JSON as a String, or <code>null</code> if it cannot be loaded
     */
    public static String loadRequest(final String requestName) {
        return loadResource("/requests/" + requestName + ".json");
    }

    private static String loadResource(final String path) {
        try {
            final InputStream stream = ValidatorTestUtil.class.getResourceAsStream(path);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            final StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Int parameters

    public static SerializableParameter intParam() {
        return intParam(true, null, null);
    }

    public static SerializableParameter intParam(final boolean required) {
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
        when(result.getMinimum()).thenReturn(min == null ? null : BigDecimal.valueOf(min));
        when(result.getMaximum()).thenReturn(max == null ? null : BigDecimal.valueOf(max));
        return result;
    }

    public static SerializableParameter intParam(final Double min, final Double max,
                                                 final Boolean exclusiveMin, final Boolean exclusiveMax) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("integer");
        when(result.getFormat()).thenReturn("int32");
        when(result.getRequired()).thenReturn(true);
        when(result.getMinimum()).thenReturn(min == null ? null : BigDecimal.valueOf(min));
        when(result.getMaximum()).thenReturn(max == null ? null : BigDecimal.valueOf(max));
        when(result.isExclusiveMinimum()).thenReturn(exclusiveMin);
        when(result.isExclusiveMaximum()).thenReturn(exclusiveMax);
        return result;
    }

    public static SerializableParameter intParamFormat(final String format) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("integer");
        when(result.getFormat()).thenReturn(format);
        when(result.getRequired()).thenReturn(true);
        when(result.getMinimum()).thenReturn(null);
        when(result.getMaximum()).thenReturn(null);
        when(result.isExclusiveMinimum()).thenReturn(null);
        when(result.isExclusiveMaximum()).thenReturn(null);
        return result;
    }

    public static SerializableParameter intParamMultipleOf(final Number multipleOf) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("integer");
        when(result.getFormat()).thenReturn("int64");
        when(result.getMultipleOf()).thenReturn(multipleOf);
        when(result.getRequired()).thenReturn(true);
        when(result.getMinimum()).thenReturn(null);
        when(result.getMaximum()).thenReturn(null);
        when(result.isExclusiveMinimum()).thenReturn(null);
        when(result.isExclusiveMaximum()).thenReturn(null);
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
        when(result.getMinLength()).thenReturn(null);
        when(result.getMaxLength()).thenReturn(null);
        return result;
    }

    // Float parameters

    public static SerializableParameter floatParam() {
        return floatParam(true, null, null);
    }

    public static SerializableParameter doubleParam() {
        return doubleParam(true, null, null);
    }

    public static SerializableParameter floatParam(final boolean required) {
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
        when(result.getMinimum()).thenReturn(min == null ? null : BigDecimal.valueOf(min));
        when(result.getMaximum()).thenReturn(max == null ? null : BigDecimal.valueOf(max));
        return result;
    }

    public static SerializableParameter doubleParam(final boolean required, final Double min, final Double max) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("number");
        when(result.getFormat()).thenReturn("double");
        when(result.getRequired()).thenReturn(required);
        when(result.getMinimum()).thenReturn(min == null ? null : BigDecimal.valueOf(min));
        when(result.getMaximum()).thenReturn(max == null ? null : BigDecimal.valueOf(max));
        return result;
    }

    public static SerializableParameter floatParam(final Double min, final Double max,
                                                   final Boolean exclusiveMin, final Boolean exclusiveMax) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("number");
        when(result.getFormat()).thenReturn("float");
        when(result.getRequired()).thenReturn(true);
        when(result.getMinimum()).thenReturn(min == null ? null : BigDecimal.valueOf(min));
        when(result.getMaximum()).thenReturn(max == null ? null : BigDecimal.valueOf(max));
        when(result.isExclusiveMinimum()).thenReturn(exclusiveMin);
        when(result.isExclusiveMaximum()).thenReturn(exclusiveMax);
        return result;
    }

    public static SerializableParameter floatParamFormat(final String format) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("number");
        when(result.getFormat()).thenReturn(format);
        when(result.getMinimum()).thenReturn(null);
        when(result.getMaximum()).thenReturn(null);
        when(result.isExclusiveMinimum()).thenReturn(null);
        when(result.isExclusiveMaximum()).thenReturn(null);
        when(result.getMultipleOf()).thenReturn(null);
        return result;
    }

    public static SerializableParameter floatParamMultipleOf(final Number multipleOf) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("number");
        when(result.getFormat()).thenReturn("float");
        when(result.getMinimum()).thenReturn(null);
        when(result.getMaximum()).thenReturn(null);
        when(result.isExclusiveMinimum()).thenReturn(null);
        when(result.isExclusiveMaximum()).thenReturn(null);
        when(result.getMultipleOf()).thenReturn(multipleOf);
        return result;
    }

    // Array parameters

    public static SerializableParameter intArrayParam(final boolean required,
                                                      final String collectionFormat) {
        final IntegerProperty property = new IntegerProperty();
        return arrayParam(required, collectionFormat, null, null, null, property);
    }

    public static SerializableParameter stringArrayParam(final boolean required,
                                                         final String collectionFormat) {
        final StringProperty property = new StringProperty();
        return arrayParam(required, collectionFormat, null, null, null, property);
    }

    public static SerializableParameter enumeratedArrayParam(final boolean required,
                                                             final String collectionFormat,
                                                             final String... enumValues) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("array");
        when(result.getCollectionFormat()).thenReturn(collectionFormat);
        when(result.getRequired()).thenReturn(required);
        when(result.getMaxItems()).thenReturn(null);
        when(result.getMinItems()).thenReturn(null);
        when(result.getEnum()).thenReturn(asList(enumValues));
        return result;
    }

    public static SerializableParameter arrayParam(final boolean required,
                                                   final String collectionFormat,
                                                   final Integer minItems,
                                                   final Integer maxItems,
                                                   final Boolean unique,
                                                   final Property items) {

        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("array");
        when(result.getCollectionFormat()).thenReturn(collectionFormat);
        when(result.getRequired()).thenReturn(required);
        when(result.getMinItems()).thenReturn(minItems);
        when(result.getMaxItems()).thenReturn(maxItems);
        when(result.isUniqueItems()).thenReturn(unique);
        when(result.getItems()).thenReturn(items);
        return result;
    }
}
