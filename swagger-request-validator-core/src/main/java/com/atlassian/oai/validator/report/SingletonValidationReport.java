package com.atlassian.oai.validator.report;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A {@link ValidationReport} containing a single {@link ValidationReport.Message}.
 * <p>
 * This {@link SingletonValidationReport} is immutable.
 */
public class SingletonValidationReport implements ValidationReport {

    private final List<ValidationReport.Message> singletonMessage;

    SingletonValidationReport(final ValidationReport.Message message) {
        this.singletonMessage = ImmutableList.of(message);
    }

    @Nonnull
    @Override
    public List<Message> getMessages() {
        return singletonMessage;
    }
}
