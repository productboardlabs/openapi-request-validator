package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.parameters.Parameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public final class ParameterValidators {

    private final SchemaValidator schemaValidator;
    private final MessageResolver messages;

    /**
     * Create a new validators object with a default (empty) schema
     * validator will be used and no <code>ref</code> validation will be performed.
     *
     * @param messages The message resolver to use.
     */
    public ParameterValidators(final MessageResolver messages) {
        this(null, messages);
    }

    /**
     * Create a new validators object with the given schema validator. If none is provided a default (empty) schema
     * validator will be used and no <code>ref</code> validation will be performed.
     *
     * @param schemaValidator The schema validator to use. If not provided a default (empty) validator will be used.
     * @param messages The message resolver to use.
     */
    public ParameterValidators(@Nullable final SchemaValidator schemaValidator,
                               final MessageResolver messages) {
        this.schemaValidator = schemaValidator == null ? new SchemaValidator(messages) : schemaValidator;
        this.messages = requireNonNull(messages);
    }

    /**
     * Validate the given value against the given parameter.
     * <p>
     * If the parameter is an array type, the given value will be split according to the parameter style
     * and each sub-value validated independently.
     *
     * @param value The value to validate
     * @param parameter The parameter to validate against
     *
     * @return A report with any validation errors
     */
    public ValidationReport validate(@Nullable final String value,
                                     final Parameter parameter) {
        requireNonNull(parameter);

        final ValidationReport.MessageContext context =
                ValidationReport.MessageContext.create().withParameter(parameter).build();

        if (TRUE.equals(parameter.getRequired()) && (value == null || value.trim().isEmpty())) {
            return ValidationReport.singleton(
                    messages.get("validation.request.parameter.missing", parameter.getName())
            ).withAdditionalContext(context);
        }

        if (value == null || value.trim().isEmpty()) {
            return ValidationReport.empty();
        }

        if (parameter.getSchema() instanceof ArraySchema) {
            return validateArrayParam(value, parameter).withAdditionalContext(context);
        }

        return schemaValidator.validate(value, parameter.getSchema())
                .withAdditionalContext(context);
    }

    /**
     * Validate the given values against the given parameter.
     * <p>
     * If multiple values are given, the parameter must be an array type and
     * it must have a style that supports multi-values (e.g. form + explode etc.).
     *
     * @param values The values to validate
     * @param parameter The parameter to validate against
     *
     * @return A report with any validation errors
     */
    public ValidationReport validate(@Nullable final Collection<String> values,
                                     final Parameter parameter) {
        final ValidationReport.MessageContext context =
                ValidationReport.MessageContext.create().withParameter(parameter).build();

        if (parameter.getRequired() && (values == null || values.isEmpty())) {
            return ValidationReport.singleton(
                    messages.get("validation.request.parameter.missing", parameter.getName())
            ).withAdditionalContext(context);
        }

        if (values == null || values.isEmpty()) {
            return ValidationReport.empty();
        }

        if (!(parameter.getSchema() instanceof ArraySchema)) {
            if (values.size() > 1) {
                return ValidationReport.singleton(
                        messages.get("validation.request.parameter.collection.invalid", parameter.getName())
                ).withAdditionalContext(context);
            }
            return schemaValidator.validate(values.iterator().next(), parameter.getSchema());
        }

        if (!ArraySeparator.from(parameter).isMultiValueParam()) {
            return ValidationReport.singleton(
                    messages.get("validation.request.parameter.collection.invalidFormat", parameter.getName(), parameter.getStyle(), "multi")
            ).withAdditionalContext(context);
        }

        return validateArrayParam(values, parameter).withAdditionalContext(context);
    }

    private ValidationReport validateArrayParam(final String value,
                                                final Parameter parameter) {
        return validateArrayParam(ArraySeparator.from(parameter).split(value), parameter);
    }

    private ValidationReport validateArrayParam(final Collection<String> values,
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

    private ValidationReport validatePattern(@Nonnull final String value,
                                             @Nonnull final Parameter parameter) {
        if (parameter.getSchema().getPattern() != null &&
                !value.matches(parameter.getSchema().getPattern())) {
            return ValidationReport.singleton(messages.get("validation.request.parameter.string.patternMismatch",
                    parameter.getName(), parameter.getSchema().getPattern())
            );
        }
        return ValidationReport.empty();
    }

    /**
     * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.1.md#parameterObject">OAI spec</a>
     */
    private static class ArraySeparator {

        static ArraySeparator from(final Parameter parameter) {
            if (parameter.getStyle() == null) {
                // See https://github.com/swagger-api/swagger-parser/issues/690 - mapping from Swagger 2.0 isn't fully implemented yet
                return new ArraySeparator(",", false);
            }
            final boolean explode = TRUE.equals(parameter.getExplode());
            switch (parameter.getStyle()) {
                case SIMPLE:
                    return new ArraySeparator(",", false);
                case MATRIX:
                    return explode ?
                            new ArraySeparator(null, true) :
                            new ArraySeparator(",", false);
                case LABEL:
                    return new ArraySeparator("\\.", false);
                case FORM:
                    return explode ?
                            new ArraySeparator(null, true) :
                            new ArraySeparator(",", false);
                case SPACEDELIMITED:
                    return explode ?
                            new ArraySeparator(null, false) :
                            new ArraySeparator(" ", false);
                case PIPEDELIMITED:
                    return explode ?
                            new ArraySeparator(null, false) :
                            new ArraySeparator("\\|", false);
                default:
                    // See https://github.com/swagger-api/swagger-parser/issues/690 - mapping from Swagger 2.0 isn't fully implemented yet
                    return new ArraySeparator(",", false);
            }
        }

        private final String separator;
        private final boolean isMultiValueParam;

        ArraySeparator(@Nullable final String separator,
                       final boolean isMultiValueParam) {
            this.separator = separator;
            this.isMultiValueParam = isMultiValueParam;
        }

        boolean isMultiValueParam() {
            return isMultiValueParam;
        }

        Collection<String> split(final String value) {
            if (separator == null) {
                return singletonList(value);
            }
            return asList(value.split(separator));
        }
    }

}
