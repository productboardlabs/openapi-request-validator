package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.SerializableParameter;

import javax.annotation.Nonnull;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public final class ParameterValidators {

    private static final List<ParameterValidator> VALIDATORS = asList(
            StringParameterValidator.INSTANCE,
            NumberParameterValidator.INSTANCE,
            IntegerParameterValidator.INSTANCE
    );

    private final ArrayParameterValidator arrayValidator;

    public ParameterValidators(final SchemaValidator schemaValidator) {
        this.arrayValidator = new ArrayParameterValidator(schemaValidator);
    }

    public ValidationReport validate(final String value, @Nonnull final Parameter parameter) {
        requireNonNull(parameter);

        if ((parameter instanceof SerializableParameter) &&
                ((SerializableParameter)parameter).getType().equalsIgnoreCase("array")) {
            return arrayValidator.validate(value, parameter);
        }

        return VALIDATORS.stream()
                .filter(v -> v.supports(parameter))
                .map(v -> v.validate(value, parameter))
                .reduce(ValidationReport.empty(), ValidationReport::merge);
    }

}
