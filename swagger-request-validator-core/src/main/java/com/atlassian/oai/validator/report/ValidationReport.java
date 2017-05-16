package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;
import java.util.List;

import static java.util.Objects.requireNonNull;

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
        ERROR,
        WARN,
        INFO,
        IGNORE
    }

    /**
     * A single message in the validation report
     */
    interface Message {

        String getKey();

        String getMessage();

        Level getLevel();

        List<String> getAdditionalInfo();
    }

    /**
     * Return an empty report.
     *
     * @return an immutable empty report
     */
    static ValidationReport empty() {
        return new EmptyValidationReport();
    }

    /**
     * Return an unmodifiable report that contains a single message.
     *
     * @param message The message to add to the report
     *
     * @return An unmodifiable validation report with a single message
     */
    static ValidationReport singleton(final Message message) {
        return new SingletonValidationReport(message);
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
     * Merges the given validation report with this one, and return a new, unmodifiable report
     * containing the messages from both reports.
     *
     * @param other The validation report to merge with this one
     *
     * @return A new, unmodifiable validation report containing all the messages from this report
     * and the other report
     */
    default ValidationReport merge(@Nonnull ValidationReport other) {
        requireNonNull(other, "A validation report is required");
        return new MergedValidationReport(this, other);
    }
}
