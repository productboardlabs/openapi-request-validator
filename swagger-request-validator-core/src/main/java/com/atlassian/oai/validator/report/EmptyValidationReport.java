package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class EmptyValidationReport implements ValidationReport {

    static final ValidationReport EMPTY_REPORT = new EmptyValidationReport();

    private EmptyValidationReport() {
        // there is no need for more than one EmptyValidationReport besides this constant one above
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
    public ValidationReport merge(@Nonnull final ValidationReport other) {
        return other;
    }
}
