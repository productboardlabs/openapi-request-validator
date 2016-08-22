package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Simple container for validation messages to allow as much validation information to be collected as possible
 */
public class MutableValidationReport implements ValidationReport {

    private final List<ValidationReport.Message> messages = new ArrayList<>();

    /**
     * Add a validation message to this report.
     *
     * @param message The validation message to include
     *
     * @return This validation report instance
     */
    public MutableValidationReport add(@Nullable final Message message) {
        if (message != null) {
            this.messages.add(message);
        }
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

}
