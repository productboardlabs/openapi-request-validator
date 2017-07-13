package com.atlassian.oai.validator.report;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class MergedValidationReportTest {

    private static final ValidationReport.Message ERROR_MSG = new ImmutableMessage("foo", ValidationReport.Level.ERROR, "A message");
    private static final ValidationReport.Message NON_ERROR_MSG = new ImmutableMessage("foo", ValidationReport.Level.WARN, "A message");

    @Test
    public void hasErrors_returnsFalse_whenNoErrors() {

        final MergedValidationReport classUnderTest = new MergedValidationReport(
                ValidationReport.singleton(NON_ERROR_MSG),
                ValidationReport.singleton(NON_ERROR_MSG)
        );

        assertFalse(classUnderTest.hasErrors());
    }

    @Test
    public void hasErrors_returnsTrue_whenErrors() {
        final MergedValidationReport classUnderTest = new MergedValidationReport(
                ValidationReport.singleton(ERROR_MSG),
                ValidationReport.singleton(NON_ERROR_MSG)
        );

        assertTrue(classUnderTest.hasErrors());
    }

    @Test
    public void getMessages_returnsAllMessages() {
        final ValidationReport.Message message1_1 = mock(ValidationReport.Message.class);
        final ValidationReport.Message message1_2 = mock(ValidationReport.Message.class);
        final ValidationReport.Message message2_1 = mock(ValidationReport.Message.class);
        final ValidationReport.Message message2_2 = mock(ValidationReport.Message.class);
        final ValidationReport.Message message2_3 = mock(ValidationReport.Message.class);

        final ValidationReport validationReport1 = ValidationReport.from(message1_1, message1_2);
        final ValidationReport validationReport2 = ValidationReport.from(message2_1, message2_2, message2_3);

        final MergedValidationReport classUnderTest = new MergedValidationReport(validationReport1, validationReport2);

        final List<ValidationReport.Message> messages = classUnderTest.getMessages();
        assertThat(messages, hasSize(5));
        assertThat(messages, containsInAnyOrder(message1_1, message1_2, message2_1, message2_2, message2_3));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getMessages_result_cantBeModified() {
        final MergedValidationReport classUnderTest = new MergedValidationReport(
                ValidationReport.singleton(ERROR_MSG),
                ValidationReport.singleton(NON_ERROR_MSG)
        );

        classUnderTest.getMessages().add(mock(ValidationReport.Message.class));
    }

    @Test
    public void merge_mergesAllSubReports() {
        final MergedValidationReport mergedReport1 = new MergedValidationReport(
                ValidationReport.singleton(ERROR_MSG),
                ValidationReport.singleton(NON_ERROR_MSG)
        );

        final MergedValidationReport mergedReport2 = new MergedValidationReport(
                mergedReport1,
                ValidationReport.singleton(NON_ERROR_MSG)
        );

        assertNotSame(mergedReport1, mergedReport2);

        final List<ValidationReport.Message> messages = mergedReport2.getMessages();
        assertThat(messages, hasSize(3));
        assertThat(mergedReport2.hasErrors(), is(true));
    }

    @Test
    public void merge_withLotsOfReports_works() {
        final int numMessages = 7500;
        ValidationReport report = ValidationReport.empty();
        for (int i = 0; i < numMessages; i++) {
            report = report.merge(ValidationReport.singleton(ERROR_MSG));
        }
        assertThat(report.hasErrors(), is(true));
        assertThat(report.getMessages(), hasSize(numMessages));
    }
}
