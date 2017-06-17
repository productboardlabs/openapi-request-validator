package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;

public class InvalidRequestExceptionTest {

    @Test
    public void test_getMessage() {
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);
        final ValidationReport.Message message1 = Mockito.mock(ValidationReport.Message.class);
        final ValidationReport.Message message2 = Mockito.mock(ValidationReport.Message.class);
        Mockito.when(validationReport.getMessages()).thenReturn(Arrays.asList(message1, message2));
        Mockito.when(message1.getMessage()).thenReturn("Error message 1");
        Mockito.when(message2.getMessage()).thenReturn("Error message 2");

        final InvalidRequestException exception = new InvalidRequestException(validationReport);
        Assert.assertThat(exception.getMessage(), equalTo("Error message 1, Error message 2"));
    }

    @Test
    public void test_getMessage_emptyErrors() {
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);
        Mockito.when(validationReport.getMessages()).thenReturn(Collections.emptyList());

        final InvalidRequestException exception = new InvalidRequestException(validationReport);
        Assert.assertThat(exception.getMessage(), equalTo(""));
    }
}
