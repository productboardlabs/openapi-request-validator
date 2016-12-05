package com.atlassian.oai.validator.report;

import org.junit.Test;

import static com.atlassian.oai.validator.report.ValidationReport.Level.ERROR;
import static com.atlassian.oai.validator.report.ValidationReport.Level.INFO;
import static com.atlassian.oai.validator.report.ValidationReport.Level.WARN;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ValidationReportFormatterTest {

    @Test
    public void format_withNull_returnsNullReportMessage() {
        final MutableValidationReport report = null;
        assertThat(ValidationReportFormatter.format(report), is("Validation report is null."));
    }

    @Test
    public void format_noErrors_returnsNoErrorMessage() {
        final MutableValidationReport report = new MutableValidationReport();
        assertThat(ValidationReportFormatter.format(report), is("No validation errors."));
    }

    @Test
    public void format_withErrors_returnsFormattedMessages() {
        final MutableValidationReport report = new MutableValidationReport();
        report.add(new ImmutableMessage("key1", ERROR, "message 1"));
        report.add(new ImmutableMessage("key2", WARN, "message 2"));
        report.add(new ImmutableMessage("key3", INFO, "message 3"));

        final String expected =
                "Validation failed.\n" +
                "[ERROR] message 1\n" +
                "[WARN] message 2\n" +
                "[INFO] message 3";

        assertThat(ValidationReportFormatter.format(report), is(expected));
    }

    @Test
    public void formatMessage_withAdditionalInfo_includesBulletPointInfo() {
        final ValidationReport.Message msg =
                new ImmutableMessage("key1", ERROR, "message 1",
                        "additional info 1", "additional info 2", null, "additional info \nwith a linebreak");

        final String expected =
                "[ERROR] message 1\n" +
                "\t* additional info 1\n" +
                "\t* additional info 2\n" +
                "\t* additional info \n" +
                "\t\twith a linebreak";

        assertThat(ValidationReportFormatter.formatMessage(msg), is(expected));
    }
}