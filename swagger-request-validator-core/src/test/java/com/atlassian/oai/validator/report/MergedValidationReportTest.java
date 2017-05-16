package com.atlassian.oai.validator.report;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MergedValidationReportTest {

    private ValidationReport classUnderTest;

    private ValidationReport validationReport1;
    private ValidationReport validationReport2;

    @Before
    public void setUp() {
        this.validationReport1 = mock(ValidationReport.class);
        this.validationReport2 = mock(ValidationReport.class);
        this.classUnderTest = new MergedValidationReport(validationReport1, validationReport2);
    }

    @Test
    public void test_hasErrors_noErrors() {
        when(validationReport1.hasErrors()).thenReturn(false);
        when(validationReport2.hasErrors()).thenReturn(false);

        Assert.assertFalse(classUnderTest.hasErrors());
    }

    @Test
    public void test_hasErrors_withErrors() {
        when(validationReport1.hasErrors()).thenReturn(false);
        when(validationReport2.hasErrors()).thenReturn(true);

        Assert.assertTrue(classUnderTest.hasErrors());
    }

    @Test
    public void test_getMessages() {
        final ValidationReport.Message message1_1 = mock(ValidationReport.Message.class);
        final ValidationReport.Message message1_2 = mock(ValidationReport.Message.class);
        final ValidationReport.Message message2_1 = mock(ValidationReport.Message.class);
        final ValidationReport.Message message2_2 = mock(ValidationReport.Message.class);
        final ValidationReport.Message message2_3 = mock(ValidationReport.Message.class);
        when(validationReport1.getMessages()).thenReturn(Arrays.asList(message1_1, message1_2));
        when(validationReport2.getMessages()).thenReturn(Arrays.asList(message2_1, message2_2, message2_3));

        final List<ValidationReport.Message> messages = classUnderTest.getMessages();
        Assert.assertThat(messages, hasSize(5));
        Assert.assertThat(messages, containsInAnyOrder(message1_1, message1_2, message2_1, message2_2, message2_3));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getMessages_cantBeModified() {
        classUnderTest.getMessages().add(mock(ValidationReport.Message.class));
    }

    @Test
    public void test_merge() {
        final ValidationReport validationReport3 = mock(ValidationReport.class);
        final ValidationReport mergedValidationReport = classUnderTest.merge(validationReport3);

        // the merged ValidationReport is a new created one
        Assert.assertNotSame(mergedValidationReport, validationReport1);
        Assert.assertNotSame(mergedValidationReport, validationReport2);
        Assert.assertNotSame(mergedValidationReport, validationReport3);

        // indirect check that the new created ValidationReport contains all other reports - by getting
        // the messages which will collect all messages of all containing ValidationReports
        final ValidationReport.Message message1 = mock(ValidationReport.Message.class);
        final ValidationReport.Message message2 = mock(ValidationReport.Message.class);
        final ValidationReport.Message message3 = mock(ValidationReport.Message.class);
        when(validationReport1.getMessages()).thenReturn(Arrays.asList(message1));
        when(validationReport2.getMessages()).thenReturn(Arrays.asList(message2));
        when(validationReport3.getMessages()).thenReturn(Arrays.asList(message3));

        final List<ValidationReport.Message> messages = mergedValidationReport.getMessages();
        Assert.assertThat(messages, hasSize(3));
        Assert.assertThat(messages, containsInAnyOrder(message1, message2, message3));
    }
}
