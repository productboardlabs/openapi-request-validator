package com.atlassian.oai.validator.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class EmptyValidationReportTest {

    private ValidationReport classUnderTest = new EmptyValidationReport();

    @Test
    public void test_hasErrors() {
        assertFalse(classUnderTest.hasErrors());
    }

    @Test
    public void test_getMessages() {
        assertTrue(classUnderTest.getMessages().isEmpty());
    }

    @Test
    public void test_getMessages_cantBeModified() {
        assertThrows(UnsupportedOperationException.class, () -> classUnderTest.getMessages().add(mock(ValidationReport.Message.class)));
    }
}
