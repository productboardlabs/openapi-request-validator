package com.atlassian.oai.validator.report;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SingletonValidationReportTest {

    private ValidationReport classUnderTest;

    private ValidationReport.Message message;

    @Before
    public void setUp() {
        this.message = mock(ValidationReport.Message.class);
        this.classUnderTest = new SingletonValidationReport(message);
    }

    private void assertHasErrors(final ValidationReport.Level level, final boolean expectedResult) {
        when(message.getLevel()).thenReturn(level);
        Assert.assertEquals(expectedResult, classUnderTest.hasErrors());
    }

    @Test
    public void test_hasErrors() {
        assertHasErrors(ValidationReport.Level.ERROR, true);
        assertHasErrors(ValidationReport.Level.WARN, false);
        assertHasErrors(ValidationReport.Level.INFO, false);
        assertHasErrors(ValidationReport.Level.IGNORE, false);
    }

    @Test
    public void test_getMessages() {
        final List<ValidationReport.Message> messages = classUnderTest.getMessages();

        Assert.assertThat(messages, hasSize(1));
        Assert.assertThat(messages, contains(message));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getMessages_cantBeModified() {
        classUnderTest.getMessages().add(mock(ValidationReport.Message.class));
    }
}
