package com.atlassian.oai.validator.schema.format;

import org.junit.Test;

public class SwaggerV20AttributeTest extends AbstractAttributeTest {

    @Test
    public void testValid() throws Exception {
        super.testValid("formats-valid", "format-valid");
    }

    @Test
    public void testInvalidDate() throws Exception {
        testError("formats-valid", "format-invalid-date",  "instance", true, new String[] {"/birthDate"});
    }

    @Test
    public void testInvalidDateTime() throws Exception {
        testError("formats-valid", "format-invalid-date-time",  "instance", true, new String[] {"/lastLogin"});
    }

    @Test
    public void testInvalidInt32() throws Exception {
        testWarning("formats-valid", "format-invalid-int32",  "key", false, new String[] {"warn.format.int32.overflow"});
    }

    @Test
    public void testInvalidInt64() throws Exception {
        testWarning("formats-valid", "format-invalid-int64",  "key", false, new String[] {"warn.format.int64.overflow"});
    }
}
