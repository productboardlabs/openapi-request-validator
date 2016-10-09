package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.parameter.format.DateFormatValidator;
import com.atlassian.oai.validator.parameter.format.DateTimeFormatValidator;
import com.atlassian.oai.validator.parameter.format.FormatValidator;
import com.atlassian.oai.validator.parameter.format.NoOpStringFormatValidator;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;
import io.swagger.models.parameters.SerializableParameter;

import javax.annotation.Nonnull;
import java.util.List;

import static java.util.Arrays.asList;

public class StringParameterValidator extends BaseParameterValidator {

    private final List<FormatValidator<String>> formatValidators;

    public StringParameterValidator(final MessageResolver messages) {
        super(messages);
        formatValidators = asList(
            new DateFormatValidator(messages),
            new DateTimeFormatValidator(messages)
        );
    }

    @Override
    @Nonnull
    public String supportedParameterType() {
        return "string";
    }

    @Override
    protected void doValidate(
            @Nonnull final String value,
            @Nonnull final SerializableParameter parameter,
            @Nonnull final MutableValidationReport report) {

        if (parameter.getPattern() != null && !value.matches(parameter.getPattern())) {
            report.add(messages.get("validation.request.parameter.string.patternMismatch",
                parameter.getName(), parameter.getPattern())
            );
        }

        if (parameter.getMinLength() != null && value.length() < parameter.getMinLength()) {
            report.add(messages.get("validation.request.parameter.string.tooShort",
                parameter.getName(), parameter.getMinLength())
            );
        }

        if (parameter.getMaxLength() != null && value.length() > parameter.getMaxLength()) {
            report.add(messages.get("validation.request.parameter.string.tooLong",
                parameter.getName(), parameter.getMaxLength())
            );
        }

        validateFormatIfPresent(value, parameter, report);
    }

    private void validateFormatIfPresent(@Nonnull final String value,
                                         @Nonnull final SerializableParameter parameter,
                                         @Nonnull final MutableValidationReport report) {

        if (parameter.getFormat() != null) {
            FormatValidator<String> formatValidator = formatValidators.stream()
                    .filter(validator -> validator.supports(parameter.getFormat()))
                    .findFirst()
                    .orElse(new NoOpStringFormatValidator());

            formatValidator.validate(report, value);
        }
    }
}
