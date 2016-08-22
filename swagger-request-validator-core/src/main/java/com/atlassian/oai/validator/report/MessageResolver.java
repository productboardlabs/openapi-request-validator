package com.atlassian.oai.validator.report;

import java.util.ResourceBundle;

import static java.lang.String.format;

public class MessageResolver {

    private final ResourceBundle messages = ResourceBundle.getBundle("messages");

    public ValidationReport.Message get(final String key, final Object... args) {
        return new ImmutableMessage(ValidationReport.Level.ERROR, format(messages.getString(key), args));
    }

}
