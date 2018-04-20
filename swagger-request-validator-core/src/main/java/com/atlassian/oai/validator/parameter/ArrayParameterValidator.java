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
import static java.util.Collections.emptyList;

/**
 * A validator for array parameters.
 * <p>
 * This is a special-case validator as it needs to handle single and collection types for validation.
 */
public class ArrayParameterValidator extends BaseParameterValidator {

    private static final String ARRAY_PARAMETER_TYPE = "array";

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

        if (!ArraySeparator.from(parameter).isMultiValueParam()) {
            return ValidationReport.singleton(
                    messages.get("validation.request.parameter.collection.invalidFormat", parameter.getName(), parameter.getStyle(), "multi")
            ).withAdditionalContext(context);
        }

        return doValidate(values, parameter);
    }

    @Override
    protected ValidationReport doValidate(final String value,
                                          final Parameter parameter) {

        return doValidate(ArraySeparator.from(parameter).split(value), parameter);
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

        ArraySeparator(final String separator, final boolean isMultiValueParam) {
            this.separator = separator;
            this.isMultiValueParam = isMultiValueParam;
        }

        boolean isMultiValueParam() {
            return isMultiValueParam;
        }

        Collection<String> split(final String value) {
            if (separator == null) {
                return emptyList();
            }
            return asList(value.split(separator));
        }
    }
}
