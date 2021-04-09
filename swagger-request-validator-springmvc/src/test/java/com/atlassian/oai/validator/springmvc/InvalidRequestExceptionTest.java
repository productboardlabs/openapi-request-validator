package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class InvalidRequestExceptionTest {

    @Test
    public void getMessage_outputsFormattedReport() {
        final ValidationReport validationReport = ValidationReport.from(
                ValidationReport.Message.create("dummy", "Message").build()
        );

        final InvalidRequestException classUnderTest = new InvalidRequestException(validationReport);
        assertThat(classUnderTest.getMessage(),
                equalTo(JsonValidationReportFormat.getInstance().apply(validationReport)));
    }

    @Test
    public void getMessage_isEmptyInCaseOfNoErrors() {
        final ValidationReport validationReport = ValidationReport.from();

        final InvalidRequestException classUnderTest = new InvalidRequestException(validationReport);
        assertThat(classUnderTest.getMessage(), equalTo("{ }"));
    }

    @Test
    public void getValidationReport() {
        final ValidationReport validationReport = ValidationReport.from(
                ValidationReport.Message.create("dummy", "Message").build()
        );

        final InvalidRequestException classUnderTest = new InvalidRequestException(validationReport);
        assertThat(classUnderTest.getValidationReport(), is(validationReport));
    }
}
