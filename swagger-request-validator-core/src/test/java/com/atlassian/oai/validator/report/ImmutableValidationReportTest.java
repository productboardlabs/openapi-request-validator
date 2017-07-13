package com.atlassian.oai.validator.report;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImmutableValidationReportTest {

    private ImmutableValidationReport classUnderTest;

    private ValidationReport.Message message;

    @Before
    public void setUp() {
        this.message = mock(ValidationReport.Message.class);
        this.classUnderTest = new ImmutableValidationReport(message);
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

        assertThat(messages, hasSize(1));
        assertThat(messages, contains(message));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getMessages_cantBeModified() {
        classUnderTest.getMessages().add(mock(ValidationReport.Message.class));
    }

    @Test
    public void test_acceptsNull() {
        assertThat(new ImmutableValidationReport((ValidationReport.Message) null), notNullValue());
    }

    @Test
    public void test_filtersNullValues_fromVarargsCtor() {
        assertThat(
                new ImmutableValidationReport(
                        mock(ValidationReport.Message.class),
                        mock(ValidationReport.Message.class),
                        null).getMessages(),
                hasSize(2)
        );
    }

    private void assertHasErrors(final ValidationReport.Level level, final boolean expectedResult) {
        when(message.getLevel()).thenReturn(level);
        Assert.assertEquals(expectedResult, classUnderTest.hasErrors());
    }
}
