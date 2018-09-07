package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class InvalidResponseExceptionTest {

    @Test
    public void getMessage_joinsTheValidationReportMessages() {
        final ValidationReport validationReport = ValidationReport.from(
                ValidationReport.Message.create("dummy", "Message").build()
        );

        final InvalidResponseException classUnderTest = new InvalidResponseException(validationReport);
        assertThat(classUnderTest.getMessage(),
                equalTo(JsonValidationReportFormat.getInstance().apply(validationReport)));
    }

    @Test
    public void getMessage_isEmptyInCaseOfNoErrors() {
        final ValidationReport validationReport = ValidationReport.from();

        final InvalidResponseException classUnderTest = new InvalidResponseException(validationReport);
        assertThat(classUnderTest.getMessage(), equalTo("{ }"));
    }

    @Test
    public void getValidationReport() {
        final ValidationReport validationReport = ValidationReport.from(
                ValidationReport.Message.create("dummy", "Message").build()
        );

        final InvalidResponseException classUnderTest = new InvalidResponseException(validationReport);
        assertThat(classUnderTest.getValidationReport(), is(validationReport));
    }
}
