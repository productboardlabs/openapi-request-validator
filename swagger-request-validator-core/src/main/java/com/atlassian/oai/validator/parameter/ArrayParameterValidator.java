package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MutableValidationReport;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.SerializableParameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static java.lang.String.format;

/**
 * A validator for array parameters.
 * <p>
 * This is a special-case validator as it needs to handle single and collection types for validation.
 */
public class ArrayParameterValidator extends BaseParameterValidator {

    private final SchemaValidator schemaValidator;

    private enum CollectionFormat {
        CSV(","),
        SSV(" "),
        TSV("\t"),
        PIPES("\\|"),
        MULTI(null);

        final String separator;
        CollectionFormat(String separator) {
            this.separator = separator;
        }

        Collection<String> split(final String value) {
            if (separator == null) {
                return Collections.singleton(value);
            }
            return Arrays.asList(value.split(separator));
        }

        static CollectionFormat from(final SerializableParameter parameter) {
            return valueOf(parameter.getCollectionFormat().toUpperCase());
        }
    }

    public ArrayParameterValidator() {
        this(null);
    }

    public ArrayParameterValidator(@Nullable final SchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator == null ? new SchemaValidator() : schemaValidator;
    }

    @Nonnull
    @Override
    public String supportedParameterType() {
        return "array";
    }

    public ValidationReport validate(@Nullable final Collection<String> values, @Nullable final Parameter p) {
        final MutableValidationReport report = new MutableValidationReport();
        if (p == null) {
            return report;
        }

        final SerializableParameter parameter = (SerializableParameter)p;
        if (parameter.getRequired() && (values == null || values.isEmpty())) {
            return report.addError(format("Parameter '%s' is required but is missing", p.getName()));
        }

        if (values == null) {
            return report;
        }

        if (!parameter.getCollectionFormat().equalsIgnoreCase(CollectionFormat.MULTI.name())) {
            return report.addError(
                    format("Parameter '%s' expected collection format of '%s' but '%s' was used instead.",
                    p.getName(), parameter.getCollectionFormat(), "multi")
            );
        }

        doValidate(values, parameter, report);
        return report;
    }

    @Override
    protected void doValidate(@Nonnull final String value,
                              @Nonnull final SerializableParameter parameter,
                              @Nonnull final MutableValidationReport validationReport) {

        doValidate(CollectionFormat.from(parameter).split(value),
                parameter,
                validationReport);

    }

    private void doValidate(@Nonnull final Collection<String> values,
                            @Nonnull final SerializableParameter parameter,
                            @Nonnull final MutableValidationReport validationReport) {

        if (parameter.getMaxItems() != null && values.size() > parameter.getMaxItems()) {
            validationReport.addError(
                    format("Parameter '%s' accepts a maximum of %d items. Found %d.",
                            parameter.getName(), parameter.getMaxItems(), values.size())
            );
        }

        if (parameter.getMinItems() != null && values.size() < parameter.getMinItems()) {
            validationReport.addError(
                    format("Parameter '%s' accepts a minimum of %d items. Found %d.",
                            parameter.getName(), parameter.getMinItems(), values.size())
            );
        }

        if (Boolean.TRUE.equals(parameter.isUniqueItems()) &&
                values.stream().distinct().count() != values.size()) {
            validationReport.addError(
                    format("Parameter '%s' does not allow duplicate values.", parameter.getName())
            );
        }

        values.forEach(v ->
                validationReport.addAll(schemaValidator.validate(v, parameter.getItems())));
    }
}
