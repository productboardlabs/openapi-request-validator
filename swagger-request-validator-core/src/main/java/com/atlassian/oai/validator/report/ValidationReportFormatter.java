package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Format a {@link ValidationReport} instance in a nice String representation for use in e.g. logs or exceptions.
 */
public class ValidationReportFormatter {

    /**
     * Format the given report in a nice String representation
     *
     * @param report The report to format
     *
     * @return A String representation of the given report
     */
    @Nonnull
    public static String format(@Nullable final ValidationReport report) {
        if (report == null) {
            return "Validation report is null.";
        }
        final StringBuilder b = new StringBuilder();
        if (!report.hasErrors()) {
            b.append("No validation errors.");
        } else {
            b.append("Validation failed.");
        }
        report.getMessages().forEach(m -> b.append("\n[").append(m.getLevel()).append("] ").append(m.getMessage().replace("\n", "\n\t")));
        return b.toString();
    }

}
