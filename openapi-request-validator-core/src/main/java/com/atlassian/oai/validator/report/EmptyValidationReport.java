package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * An empty {@link ValidationReport} which contains no {@link ValidationReport.Message}.
 * An {@link EmptyValidationReport} can't have any error by definition.
 * <p>
 * This {@link EmptyValidationReport} is immutable.
 */
public class EmptyValidationReport implements ValidationReport {

    EmptyValidationReport() {
    }

    @Override
    public boolean hasErrors() {
        return false;
    }

    @Nonnull
    @Override
    public List<Message> getMessages() {
        return Collections.emptyList();
    }

    @Override
    public ValidationReport withAdditionalContext(final MessageContext context) {
        return this;
    }
}
