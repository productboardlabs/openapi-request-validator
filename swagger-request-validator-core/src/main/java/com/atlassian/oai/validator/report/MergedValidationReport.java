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

    private final List<ValidationReport> validationReports;

    MergedValidationReport(final ValidationReport validationReport1, final ValidationReport validationReport2) {
        this.validationReports = ImmutableList.of(validationReport1, validationReport2);
    }

    @Nonnull
    @Override
    public List<ValidationReport.Message> getMessages() {
        final ImmutableList.Builder<ValidationReport.Message> builder = ImmutableList.builder();
        validationReports.forEach(report -> builder.addAll(report.getMessages()));
        return builder.build();
    }

    @Override
    public boolean hasErrors() {
        return validationReports.stream().anyMatch(report -> report.hasErrors());
    }
}
