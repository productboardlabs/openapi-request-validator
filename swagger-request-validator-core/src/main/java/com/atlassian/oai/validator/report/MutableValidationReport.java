package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.atlassian.oai.validator.report.ValidationReport.Level.ERROR;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Simple container for validation messages to allow as much validation information to be collected as possible
 */
public class MutableValidationReport implements ValidationReport {

    private final List<ValidationReport.Message> messages = new ArrayList<>();

    /**
     * Add a validation error to this report.
     *
     * @param message The validation message to include
     *
     * @return This validation report instance
     */
    public MutableValidationReport addError(@Nonnull final String message) {
        requireNonNull(message, "A validation message is required");
        this.messages.add(new MessageImpl(ERROR, message));
        return this;
    }

    public void addAll(@Nonnull final ValidationReport other) {
        this.messages.addAll(other.getMessages());
    }

    @Override
    public ValidationReport merge(@Nonnull final ValidationReport other) {
        requireNonNull(other, "A validation report is required");

        final MutableValidationReport result = new MutableValidationReport();
        result.messages.addAll(this.getMessages());
        result.messages.addAll(other.getMessages());
        return result;
    }

    @Nonnull
    @Override
    public List<ValidationReport.Message> getMessages() {
        return unmodifiableList(messages);
    }

    private static class MessageImpl implements ValidationReport.Message {
        private final ValidationReport.Level level;
        private final String message;

        public MessageImpl(@Nonnull final ValidationReport.Level level, @Nonnull final String message) {
            this.level = level;
            this.message = message;
        }

        @Override
        public Level getLevel() {
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

}
