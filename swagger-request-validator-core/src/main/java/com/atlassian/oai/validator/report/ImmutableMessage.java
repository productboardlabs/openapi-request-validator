package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

class ImmutableMessage implements ValidationReport.Message {

    private final String key;
    private final ValidationReport.Level level;
    private final String message;

    ImmutableMessage(@Nonnull final String key, @Nonnull final ValidationReport.Level level, @Nonnull final String message) {
        this.key = requireNonNull(key, "A key is required");
        this.level = requireNonNull(level, "A level is required");
        this.message = requireNonNull(message, "A message is required");
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public ValidationReport.Level getLevel() {
        return level;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return level + " - " + message.replace("\n", "\n\t");
    }

}
