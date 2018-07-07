package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Format a {@link ValidationReport} instance into String representation for use in e.g. logs or exceptions.
 */
public class ValidationReportFormatter {

    /**
     * Format the given report in a nice human-readable String representation suitable for logging etc.
     *
     * @param report The report to format
     *
     * @return A String representation of the given report
     */
    @Nonnull
    public static String format(@Nullable final ValidationReport report) {
        return SimpleValidationReportFormat.getInstance().apply(report);
    }

    private ValidationReportFormatter() { }
}
