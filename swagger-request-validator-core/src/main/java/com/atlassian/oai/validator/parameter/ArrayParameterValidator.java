package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.parameters.Parameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.LABEL;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.MATRIX;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.PIPEDELIMITED;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.SIMPLE;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.SPACEDELIMITED;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

/**
 * A validator for array parameters.
 * <p>
 * This is a special-case validator as it needs to handle single and collection types for validation.
 */
public class ArrayParameterValidator extends BaseParameterValidator {

    private static final String ARRAY_PARAMETER_TYPE = "array";

    private static final Map<Parameter.StyleEnum, String> SEPARATORS = ImmutableMap.of(
            SIMPLE, ",",
            SPACEDELIMITED, " ",
            PIPEDELIMITED, "\\|",
            LABEL, "\\.",
            MATRIX, ","
    );

    private final SchemaValidator schemaValidator;

    ArrayParameterValidator(@Nullable final SchemaValidator schemaValidator,
                            final MessageResolver messages) {
        super(messages);
        this.schemaValidator = schemaValidator == null ? new SchemaValidator(messages) : schemaValidator;
    }

    @Nonnull
    @Override
    public String supportedParameterType() {
        return ARRAY_PARAMETER_TYPE;
    }

    @Override
    @Nonnull
    public ValidationReport validate(@Nullable final String value, @Nullable final Parameter parameter) {
        if (!supports(parameter)) {
            return ValidationReport.empty();
        }

        final ValidationReport.MessageContext context = ValidationReport.MessageContext.create().withParameter(parameter).build();

        if (TRUE.equals(parameter.getRequired()) && (value == null || value.trim().isEmpty())) {
            return ValidationReport.singleton(
                    messages.get("validation.request.parameter.missing", parameter.getName())
            ).withAdditionalContext(context);
        }

        if (value == null || value.trim().isEmpty()) {
            return ValidationReport.empty();
        }

        return doValidate(value, parameter).withAdditionalContext(context);
    }

    public ValidationReport validate(@Nullable final Collection<String> values, @Nullable final Parameter parameter) {
        if (parameter == null) {
            return ValidationReport.empty();
        }

        final ValidationReport.MessageContext context = ValidationReport.MessageContext.create().withParameter(parameter).build();

        if (parameter.getRequired() && (values == null || values.isEmpty())) {
            return ValidationReport.singleton(
                    messages.get("validation.request.parameter.missing", parameter.getName())
            ).withAdditionalContext(context);
        }

        if (values == null) {
            return ValidationReport.empty();
        }

        // TODO: Need to support style + explode
//        if (parameter.getStyle() != FORM && parameter.getStyle() != ) {
//            return ValidationReport.singleton(
//                    messages.get("validation.request.parameter.collection.invalidFormat", parameter.getName(), parameter.getCollectionFormat(), "multi")
//            ).withAdditionalContext(context);
//        }

        return doValidate(values, parameter);
    }

    @Override
    protected ValidationReport doValidate(final String value,
                                          final Parameter parameter) {

        return doValidate(splitValue(parameter, value), parameter);
    }

    private ValidationReport doValidate(final Collection<String> values,
                                        final Parameter parameter) {

        final ValidationReport report = Stream.of(
                validateMaxItems(values, parameter),
                validateMinItems(values, parameter),
                validateUniqueItems(values, parameter)
        ).reduce(ValidationReport.empty(), ValidationReport::merge);

        if (parameter.getSchema().getEnum() != null && !parameter.getSchema().getEnum().isEmpty()) {
            final Set<String> enumValues = new HashSet<>(parameter.getSchema().getEnum());
            return values.stream()
                    .filter(v -> !enumValues.contains(v))
                    .map(v -> ValidationReport.singleton(messages.get("validation.request.parameter.enum.invalid",
                            v, parameter.getName(), parameter.getSchema().getEnum())
                    ))
                    .reduce(report, ValidationReport::merge);
        }

        return values.stream()
                .map(v -> schemaValidator.validate(v, ((ArraySchema) parameter.getSchema()).getItems()))
                .reduce(report, ValidationReport::merge);
    }

    private ValidationReport validateUniqueItems(final Collection<String> values, final Parameter parameter) {
        if (TRUE.equals(parameter.getSchema().getUniqueItems()) &&
            values.stream().distinct().count() != values.size()) {
            return ValidationReport.singleton(messages.get("validation.request.parameter.collection.duplicateItems",
                parameter.getName())
            );
        }
        return ValidationReport.empty();
    }

    private ValidationReport validateMinItems(final Collection<String> values, final Parameter parameter) {
        if (parameter.getSchema().getMinItems() != null && values.size() < parameter.getSchema().getMinItems()) {
            return ValidationReport.singleton(messages.get("validation.request.parameter.collection.tooFewItems",
                    parameter.getName(), parameter.getSchema().getMinItems(), values.size())
            );
        }
        return ValidationReport.empty();
    }

    private ValidationReport validateMaxItems(final Collection<String> values, final Parameter parameter) {
        if (parameter.getSchema().getMaxItems() != null && values.size() > parameter.getSchema().getMaxItems()) {
            return ValidationReport.singleton(messages.get("validation.request.parameter.collection.tooManyItems",
                    parameter.getName(), parameter.getSchema().getMaxItems(), values.size())
            );
        }
        return ValidationReport.empty();
    }

    private static Collection<String> splitValue(final Parameter parameter, final String value) {
        final String separator = SEPARATORS.get(parameter.getStyle());
        if (separator == null) {
            return singleton(value);
        }
        return asList(value.split(separator));
    }
}
