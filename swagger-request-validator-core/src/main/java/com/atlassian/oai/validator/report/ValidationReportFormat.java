package com.atlassian.oai.validator.report;

public interface ValidationReportFormat {

    /**
     * Format the given validation report as a {@link String} suitable for logging etc.
     *
     * @param report The report to format
     *
     * @return A string representation of the given report
     */
    String apply(ValidationReport report);

}
