package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.models.parameters.Parameter;

import javax.annotation.Nonnull;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class ParameterValidators {

    private static final List<ParameterValidator> VALIDATORS = asList(
            StringParameterValidator.INSTANCE,
            NumberParameterValidator.INSTANCE,
            IntegerParameterValidator.INSTANCE
    );

    public static ValidationReport validate(final String value, @Nonnull final Parameter parameter) {
        requireNonNull(parameter);
        return VALIDATORS.stream()
                .filter(v -> v.supports(parameter))
                .map(v -> v.validate(value, parameter))
                .reduce(ValidationReport.empty(), ValidationReport::merge);
    }

}
