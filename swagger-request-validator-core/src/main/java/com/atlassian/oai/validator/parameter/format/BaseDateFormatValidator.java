package com.atlassian.oai.validator.parameter.format;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;

import javax.annotation.Nonnull;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public abstract class BaseDateFormatValidator implements FormatValidator<String> {

    private final MessageResolver messages;

    protected BaseDateFormatValidator(MessageResolver messages) {
        this.messages = messages;
    }

    @Override
    public boolean canValidate(String format) {
        return format.equals("date");
    }

    @Override
    public void validate(@Nonnull final MutableValidationReport report,
                         @Nonnull final String value) {
        DateTimeFormatter dateFormatter = getFormatter();
        try {
            dateFormatter.parse(value);
        } catch (DateTimeParseException e) {
            report.add(messages.get("validation.request.parameter.string.date.invalid", value));
        }
    }

    protected abstract DateTimeFormatter getFormatter();
}
