package com.atlassian.oai.validator.parameter.format;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;

import javax.annotation.Nonnull;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public abstract class BaseDateFormatValidator implements FormatValidator<String> {

    private final MessageResolver messages;

    protected BaseDateFormatValidator(final MessageResolver messages) {
        this.messages = messages;
    }

    @Override
    public void validate(@Nonnull final MutableValidationReport report,
                         @Nonnull final String value) {
        final DateTimeFormatter dateFormatter = getFormatter();
        try {
            dateFormatter.parse(value);
        } catch (final DateTimeParseException e) {
            report.add(messages.get("validation.request.parameter.string." + getMessageKey() + ".invalid", value));
        }
    }

    protected abstract String getMessageKey();

    protected abstract DateTimeFormatter getFormatter();
}
