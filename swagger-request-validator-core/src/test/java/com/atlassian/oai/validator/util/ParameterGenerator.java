package com.atlassian.oai.validator.util;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.EmailSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

public class ParameterGenerator {

    private ParameterGenerator() {
    }

    public static Parameter param(final Schema schema, final boolean isRequired) {
        final Parameter result = new Parameter();
        result.setName("Test Parameter");
        result.setRequired(isRequired);
        result.setSchema(schema);
        return result;
    }

    public static Parameter requiredParam() {
        return stringParam(true);
    }

    public static Parameter emptyAllowedQueryParam() {
        return emptyAllowedQueryParam(true);
    }

    public static Parameter emptyAllowedQueryParam(final boolean isRequired) {
        final Parameter parameter = stringParam(isRequired);
        parameter.setAllowEmptyValue(true);
        parameter.setIn("query");
        return parameter;
    }

    public static Parameter emptyAllowedNonConformQueryParam(final boolean isRequired) {
        final Parameter parameter = patternStringParam("^.+$", isRequired);
        parameter.setAllowEmptyValue(true);
        parameter.setIn("query");
        return parameter;
    }

    public static Parameter emptyAllowedHeaderParam() {
        return emptyAllowedHeaderParam(true);
    }

    public static Parameter emptyAllowedHeaderParam(final boolean isRequired) {
        final Parameter parameter = stringParam(isRequired);
        parameter.setAllowEmptyValue(true);
        parameter.setIn("head");
        return parameter;
    }

    public static Parameter emptyAllowedNonConformHeaderParam(final boolean isRequired) {
        final Parameter parameter = patternStringParam("^.+$", isRequired);
        parameter.setAllowEmptyValue(true);
        parameter.setIn("head");
        return parameter;
    }

    // Int parameters

    public static Parameter intParam() {
        return intParam(true, null, null);
    }

    public static Parameter intParam(final boolean required) {
        return intParam(required, null, null);
    }

    public static Parameter intParam(final Double min, final Double max) {
        return intParam(true, min, max);
    }

    public static Parameter intParam(final boolean required, final Double min, final Double max) {
        final Schema schema = new IntegerSchema();
        schema.setFormat("int32");
        schema.setMinimum(min == null ? null : BigDecimal.valueOf(min));
        schema.setMaximum(max == null ? null : BigDecimal.valueOf(max));
        return param(schema, required);
    }

    public static Parameter intParam(final Double min, final Double max,
                                     final Boolean exclusiveMin, final Boolean exclusiveMax) {
        final Schema schema = new IntegerSchema();
        schema.setFormat("int32");
        schema.setMinimum(min == null ? null : BigDecimal.valueOf(min));
        schema.setMaximum(max == null ? null : BigDecimal.valueOf(max));
        schema.setExclusiveMinimum(exclusiveMin);
        schema.setExclusiveMaximum(exclusiveMax);
        return param(schema, true);
    }

    public static Parameter enumeratedIntParam(final Integer... allowed) {
        final IntegerSchema schema = new IntegerSchema();
        schema.setEnum(asList(allowed));
        return param(schema, true);
    }

    public static Parameter intParamFormat(final String format) {
        final Schema schema = new IntegerSchema();
        schema.setFormat(format);
        return param(schema, true);
    }

    public static Parameter intParamMultipleOf(final Integer multipleOf) {
        final Schema schema = new IntegerSchema();
        schema.setFormat("int64");
        schema.setMultipleOf(multipleOf == null ? null : BigDecimal.valueOf(multipleOf));
        return param(schema, true);
    }

    // String parameters

    public static Parameter stringParam() {
        return stringParam(true);
    }

    public static Parameter stringParam(final boolean required) {
        return param(new StringSchema(), required);
    }

    public static Parameter stringParam(final Integer minLength, final Integer maxLength) {
        return stringParam(minLength, maxLength, true);
    }

    public static Parameter stringParam(final Integer minLength, final Integer maxLength, final boolean required) {
        final StringSchema schema = new StringSchema();
        schema.setMinLength(minLength);
        schema.setMaxLength(maxLength);
        return param(schema, required);
    }

    public static Parameter patternStringParam(final String pattern) {
        return patternStringParam(pattern, true);
    }

    public static Parameter patternStringParam(final String pattern, final boolean isRequired) {
        final StringSchema schema = new StringSchema();
        schema.setPattern(pattern);
        return param(schema, isRequired);
    }

    public static Parameter enumeratedStringParam(final String... _enum) {
        return enumeratedStringParam(true, _enum);
    }

    public static Parameter enumeratedStringParam(final boolean isRequired, final String... _enum) {
        final StringSchema schema = new StringSchema();
        schema.setEnum(asList(_enum));
        return param(schema, isRequired);
    }

    public static Parameter stringParamFormat(final String format) {
        final StringSchema schema = new StringSchema();
        schema.format(format);
        return param(schema, true);
    }

    public static Parameter uuidParam() {
        return param(new UUIDSchema(), true);
    }

    public static Parameter dateParam() {
        return param(new DateSchema(), true);
    }

    public static Parameter dateTimeParam() {
        return param(new DateTimeSchema(), true);
    }

    public static Parameter emailParam() {
        return param(new EmailSchema(), true);
    }

    public static Parameter ipv4Param() {
        return stringParamFormat("ipv4");
    }

    public static Parameter ipv6Param() {
        return stringParamFormat("ipv6");
    }

    public static Parameter uriParam() {
        return stringParamFormat("uri");
    }

    // Float parameters

    public static Parameter floatParam() {
        return floatParam(true, null, null);
    }

    public static Parameter doubleParam() {
        return doubleParam(true, null, null);
    }

    public static Parameter floatParam(final boolean required) {
        return floatParam(required, null, null);
    }

    public static Parameter floatParam(final Double min, final Double max) {
        return floatParam(true, min, max);
    }

    public static Parameter floatParam(final boolean required, final Double min, final Double max) {
        final Schema schema = new NumberSchema();
        schema.setFormat("float");
        schema.setMinimum(min == null ? null : BigDecimal.valueOf(min));
        schema.setMaximum(max == null ? null : BigDecimal.valueOf(max));
        return param(schema, required);
    }

    public static Parameter doubleParam(final boolean required, final Double min, final Double max) {
        final Schema schema = new NumberSchema();
        schema.setFormat("double");
        schema.setMinimum(min == null ? null : BigDecimal.valueOf(min));
        schema.setMaximum(max == null ? null : BigDecimal.valueOf(max));
        return param(schema, required);
    }

    public static Parameter floatParam(final Double min, final Double max,
                                       final Boolean exclusiveMin, final Boolean exclusiveMax) {
        final Schema schema = new NumberSchema();
        schema.setFormat("float");
        schema.setMinimum(min == null ? null : BigDecimal.valueOf(min));
        schema.setMaximum(max == null ? null : BigDecimal.valueOf(max));
        schema.setExclusiveMinimum(exclusiveMin);
        schema.setExclusiveMaximum(exclusiveMax);
        return param(schema, true);
    }

    public static Parameter floatParamFormat(final String format) {
        final Schema schema = new NumberSchema();
        schema.setFormat(format);
        return param(schema, true);
    }

    public static Parameter floatParamMultipleOf(final Float multipleOf) {
        final Schema schema = new NumberSchema();
        schema.setFormat("float");
        schema.setMultipleOf(multipleOf == null ? null : BigDecimal.valueOf(multipleOf));
        return param(schema, true);
    }

    public static Parameter enumeratedFloatParam(final Float... allowed) {
        final NumberSchema schema = new NumberSchema();
        schema.format("float");
        schema.setEnum(stream(allowed).map(BigDecimal::valueOf).collect(Collectors.toList()));
        return param(schema, true);
    }

    // Boolean parameters
    public static Parameter boolParam() {
        return boolParam(true);
    }

    public static Parameter boolParam(final boolean required) {
        final Schema schema = new BooleanSchema();
        return param(schema, required);
    }

    // Array parameters

    public static Parameter intArrayParam(final Parameter.StyleEnum style) {
        return intArrayParam(false, style, false);
    }

    public static Parameter intArrayParam(final boolean required,
                                          final Parameter.StyleEnum style,
                                          final boolean explode) {
        return arrayParam(required, style, explode, null, null, null, new IntegerSchema());
    }

    public static Parameter stringArrayParam(final Parameter.StyleEnum style) {
        return stringArrayParam(false, style, false);
    }

    public static Parameter stringArrayParam(final boolean required,
                                             final Parameter.StyleEnum style,
                                             final boolean explode) {
        return arrayParam(required, style, explode, null, null, null, new StringSchema());
    }

    public static Parameter enumeratedArrayParam(final boolean required,
                                                 final Parameter.StyleEnum style,
                                                 final String... enumValues) {

        final StringSchema items = new StringSchema();
        items.setEnum(asList(enumValues));

        final ArraySchema schema = new ArraySchema();
        schema.setType("array");
        schema.setItems(items);

        final Parameter result = new Parameter();
        result.setName("Test Parameter");
        result.setRequired(required);
        result.setSchema(schema);
        result.setStyle(style);

        return result;
    }

    public static Parameter arrayParam(final boolean required,
                                       final Parameter.StyleEnum style,
                                       final boolean explode,
                                       final Integer minItems,
                                       final Integer maxItems,
                                       final Boolean unique,
                                       final Schema items) {

        final ArraySchema schema = new ArraySchema();
        schema.setType("array");
        schema.setMinItems(minItems);
        schema.setMaxItems(maxItems);
        schema.setUniqueItems(unique);
        schema.setItems(items);

        final Parameter result = new Parameter();
        result.setName("Test Parameter");
        result.setRequired(required);
        result.setSchema(schema);
        result.setStyle(style);
        result.setExplode(explode);
        return result;
    }

}
