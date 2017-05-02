package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Simple container for validation messages to allow as much validation information to be collected as possible
 */
public class MutableValidationReport implements ValidationReport {

    private final List<ValidationReport.Message> messages = new ArrayList<>();

    MutableValidationReport(final Message message) {
        this.messages.add(message);
    }

    @Nonnull
    @Override
    public List<ValidationReport.Message> getMessages() {
        return unmodifiableList(messages);
    }

    @Override
    public ValidationReport merge(@Nonnull final ValidationReport other) {
        requireNonNull(other, "A validation report is required");
        this.messages.addAll(other.getMessages());
        return this;
    }

    /**
     * Merge method especially optimized for {@link MutableValidationReport}.
     * @see ValidationReport#merge(ValidationReport)
     *
     * @param other an other {@link MutableValidationReport}
     * @return this {@link ValidationReport}
     */
    public ValidationReport merge(@Nonnull final MutableValidationReport other) {
        requireNonNull(other, "A validation report is required");
        this.messages.addAll(other.messages);
        return this;
    }

    /**
     * Merge method especially optimized for {@link EmptyValidationReport}.
     * @see ValidationReport#merge(ValidationReport)
     *
     * @param other an {@link EmptyValidationReport}
     * @return this {@link ValidationReport}
     */
    public ValidationReport merge(@Nonnull final EmptyValidationReport other) {
        // nothing to do as an empty validation report contains no messages
        return this;
    }
}
