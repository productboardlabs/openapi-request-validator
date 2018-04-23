package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.parameter.format.DateFormatValidator;
import com.atlassian.oai.validator.parameter.format.DateTimeFormatValidator;
import com.atlassian.oai.validator.parameter.format.EmailFormatValidator;
import com.atlassian.oai.validator.parameter.format.FormatValidator;
import com.atlassian.oai.validator.parameter.format.IPv4FormatValidator;
import com.atlassian.oai.validator.parameter.format.IPv6FormatValidator;
import com.atlassian.oai.validator.parameter.format.NoOpStringFormatValidator;
import com.atlassian.oai.validator.parameter.format.URIFormatValidator;
import com.atlassian.oai.validator.parameter.format.UUIDFormatValidator;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

public class StringParameterValidator extends BaseParameterValidator {

    private static final Logger log = getLogger(StringParameterValidator.class);

    private final List<FormatValidator<String>> formatValidators;

    public StringParameterValidator(final MessageResolver messages) {
        super(messages);
        formatValidators = asList(
                new DateFormatValidator(messages),
                new DateTimeFormatValidator(messages),
                new UUIDFormatValidator(messages),
                new EmailFormatValidator(messages),
                new IPv4FormatValidator(messages),
                new IPv6FormatValidator(messages),
                new URIFormatValidator(messages)
        );
    }

    @Override
    @Nonnull
    public String supportedParameterType() {
        return "string";
    }

    @Override
    protected ValidationReport doValidate(@Nonnull final String value,
                                          @Nonnull final Parameter parameter) {

        return Stream.of(
                validatePattern(value, parameter),
                validateMaxLength(value, parameter),
                validateMinLength(value, parameter),
                validateFormatIfPresent(value, parameter)
        ).reduce(ValidationReport.empty(), ValidationReport::merge);
    }

    private ValidationReport validatePattern(@Nonnull final String value,
                                             @Nonnull final Parameter parameter) {
        if (parameter.getSchema().getPattern() != null && !value.matches(parameter.getSchema().getPattern())) {
            return ValidationReport.singleton(messages.get("validation.request.parameter.string.patternMismatch",
                    parameter.getName(), parameter.getSchema().getPattern())
            );
        }
        return ValidationReport.empty();
    }

    private ValidationReport validateMaxLength(@Nonnull final String value,
                                               @Nonnull final Parameter parameter) {
        if (parameter.getSchema().getMinLength() != null && value.length() < parameter.getSchema().getMinLength()) {
            return ValidationReport.singleton(messages.get("validation.request.parameter.string.tooShort",
                    parameter.getName(), parameter.getSchema().getMinLength())
            );
        }
        return ValidationReport.empty();
    }

    private ValidationReport validateMinLength(@Nonnull final String value,
                                               @Nonnull final Parameter parameter) {
        if (parameter.getSchema().getMaxLength() != null && value.length() > parameter.getSchema().getMaxLength()) {
            return ValidationReport.singleton(messages.get("validation.request.parameter.string.tooLong",
                    parameter.getName(), parameter.getSchema().getMaxLength())
            );
        }
        return ValidationReport.empty();
    }

    private ValidationReport validateFormatIfPresent(@Nonnull final String value,
                                                     @Nonnull final Parameter parameter) {

        if (parameter.getSchema().getFormat() != null) {
            final FormatValidator<String> formatValidator = formatValidators.stream()
                    .filter(validator -> validator.supports(parameter.getSchema().getFormat()))
                    .findFirst()
                    .orElseGet(() -> {
                        log.warn("Parameter format '{}' currently not supported.", parameter.getSchema().getFormat());
                        return new NoOpStringFormatValidator();
                    });

            return formatValidator.validate(value);
        }
        return ValidationReport.empty();
    }
}
