package com.atlassian.oai.validator.report;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class EmptyValidationReportTest {

    private ValidationReport classUnderTest = new EmptyValidationReport();

    @Test
    public void test_hasErrors() {
        Assert.assertFalse(classUnderTest.hasErrors());
    }

    @Test
    public void test_getMessages() {
        Assert.assertTrue(classUnderTest.getMessages().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getMessages_cantBeModified() {
        classUnderTest.getMessages().add(mock(ValidationReport.Message.class));
    }
}
