package com.atlassian.oai.validator.report;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A {@link ValidationReport} serving as container for multiple {@link ValidationReport}s.
 * <p>
 * This {@link MergedValidationReport} is immutable.
 */
public class MergedValidationReport implements ValidationReport {

    private final ImmutableList<ValidationReport.Message> messages;

    MergedValidationReport(final ValidationReport validationReport1, final ValidationReport validationReport2) {
        this.messages = new ImmutableList
                .Builder<ValidationReport.Message>()
                .addAll(validationReport1.getMessages())
                .addAll(validationReport2.getMessages())
                .build();
    }

    @Nonnull
    @Override
    public List<ValidationReport.Message> getMessages() {
        return messages;
    }

    @Override
    public boolean hasErrors() {
        return messages.stream().anyMatch(m -> m.getLevel() == Level.ERROR);
    }
}
