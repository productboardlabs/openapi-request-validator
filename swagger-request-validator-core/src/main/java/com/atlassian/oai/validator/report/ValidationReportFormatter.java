package com.atlassian.oai.validator.report;

public class ValidationReportFormatter {

    public static String toString(final ValidationReport report) {
        if (report == null) {
            return "null";
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
