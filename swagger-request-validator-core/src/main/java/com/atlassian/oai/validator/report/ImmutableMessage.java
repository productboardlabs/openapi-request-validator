package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;

class ImmutableMessage implements ValidationReport.Message {

    private final ValidationReport.Level level;
    private final String message;

    ImmutableMessage(@Nonnull final ValidationReport.Level level, @Nonnull final String message) {
        this.level = level;
        this.message = message;
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
