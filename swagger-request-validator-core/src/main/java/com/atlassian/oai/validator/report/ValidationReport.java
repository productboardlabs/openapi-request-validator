package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * A report of validation errors that occurred during validation.
 * <p>
 * A report consists of a collection of messages with a given level.
 * Any message with a level of {@link Level#ERROR} indicates a validation failure.
 */
public interface ValidationReport {

    /**
     * The validation level
     */
    enum Level {
        ERROR
    }

    /**
     * A single message in the validation report
     */
    interface Message {
        String getMessage();
        Level getLevel();
    }

    ValidationReport EMPTY_REPORT = new ValidationReport(){

        @Override
        public boolean hasErrors() {
            return false;
        }

        @Override
        public List<Message> getMessages() {
            return Collections.emptyList();
        }

        @Override
        public ValidationReport merge(ValidationReport other) {
            return other;
        }
    };

    /**
     * Return an empty report.
     *
     * @return an immutable empty report
     */
    static ValidationReport empty() {
        return EMPTY_REPORT;
    }

    /**
     * Return an unmodifiable report that contains a single error message.
     *
     * @param message The error message to add to the report
     *
     * @return An unmodifiable validation report with a single error message
     */
    static ValidationReport singleton(final String message) {
        return new ValidationReport() {

            @Override
            public boolean hasErrors() {
                return true;
            }

            @Nonnull
            @Override
            public List<Message> getMessages() {
                return Collections.singletonList(new ImmutableMessage(Level.ERROR, message));
            }

            @Override
            public ValidationReport merge(@Nonnull ValidationReport other) {
                final MutableValidationReport result = new MutableValidationReport();
                result.addAll(this);
                result.addAll(other);
                return result;
            }
        };
    }

    /**
     * Return an unmodifiable report that contains a single message.
     *
     * @param message The message to add to the report
     *
     * @return An unmodifiable validation report with a single message
     */
    static ValidationReport singleton(final Message message) {
        return new ValidationReport() {

            @Override
            public boolean hasErrors() {
                return true;
            }

            @Nonnull
            @Override
            public List<Message> getMessages() {
                return Collections.singletonList(message);
            }

            @Override
            public ValidationReport merge(@Nonnull ValidationReport other) {
                final MutableValidationReport result = new MutableValidationReport();
                result.addAll(this);
                result.addAll(other);
                return result;
            }
        };
    }

    /**
     * Return if this validation report contains errors.
     *
     * @return <code>true</code> if a validation error exists; <code>false</code> otherwise.
     */
    default boolean hasErrors() {
        return getMessages().stream().anyMatch(m -> m.getLevel() == Level.ERROR);
    }

    /**
     * Get the validation messages on this report.
     *
     * @return The messages recorded on this report
     */
    @Nonnull
    List<Message> getMessages();

    /**
     * Merge the validation messages from the given report with this one, and return a
     * new report with the merged messaged.
     *
     * @param other The validation report to merge with this one
     *
     * @return A new report that contains all the messages from this report and the other report
     */
    ValidationReport merge(@Nonnull ValidationReport other);

}
