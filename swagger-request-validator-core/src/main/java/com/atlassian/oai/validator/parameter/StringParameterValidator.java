package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.parameter.format.DateFormatValidator;
import com.atlassian.oai.validator.parameter.format.DateTimeFormatValidator;
import com.atlassian.oai.validator.parameter.format.FormatValidator;
import com.atlassian.oai.validator.parameter.format.NoOpStringFormatValidator;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;
import com.atlassian.oai.validator.report.ValidationReport;
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
    protected ValidationReport doValidate(
            @Nonnull final String value,
            @Nonnull final SerializableParameter parameter) {

        final MutableValidationReport report = new MutableValidationReport();
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

        return report.merge(validateFormatIfPresent(value, parameter));
    }

    private ValidationReport validateFormatIfPresent(@Nonnull final String value,
                                                     @Nonnull final SerializableParameter parameter) {

        if (parameter.getFormat() != null) {
            final FormatValidator<String> formatValidator = formatValidators.stream()
                    .filter(validator -> validator.supports(parameter.getFormat()))
                    .findFirst()
                    .orElse(new NoOpStringFormatValidator());

            return formatValidator.validate(value);
        }
        return ValidationReport.empty();
    }
}
