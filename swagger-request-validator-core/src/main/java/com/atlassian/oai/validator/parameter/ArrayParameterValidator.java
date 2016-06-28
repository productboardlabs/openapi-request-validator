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
        CSV(','),
        SSV(' '),
        TSV('\t'),
        PIPES('|'),
        MULTI(null);

        final Character separator;
        CollectionFormat(Character separator) {
            this.separator = separator;
        }

        Collection<String> split(final String value) {
            if (separator == null) {
                return Collections.singleton(value);
            }
            return Arrays.asList(value.split("" + separator));
        }
    }

    public ArrayParameterValidator(SchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator;
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

        if (!parameter.getCollectionFormat().equalsIgnoreCase("multi")) {
            return report.addError(format("Incorrect parameter collection format for parameter '%s'", p.getName()));
        }

        doValidate(values, parameter, report);
        return report;
    }

    @Override
    protected void doValidate(@Nonnull final String value,
                              @Nonnull final SerializableParameter parameter,
                              @Nonnull final MutableValidationReport validationReport) {

        doValidate(CollectionFormat.valueOf(parameter.getCollectionFormat().toUpperCase()).split(value),
                parameter,
                validationReport);

    }

    private void doValidate(@Nonnull final Collection<String> values,
                            @Nonnull final SerializableParameter parameter,
                            @Nonnull final MutableValidationReport validationReport) {
        values.forEach(v ->
                validationReport.addAll(schemaValidator.validate(v, parameter.getItems())));
    }
}
