package com.atlassian.oai.validator.schema.format;

import com.github.fge.jsonschema.core.report.LogLevel;
import org.junit.Test;

public class SwaggerV20AttributeTest extends AbstractAttributeTest {

    @Test
    public void testValid() throws Exception {
        test("formats-valid", "format-valid");
    }

    @Test
    public void testInvalidDate() throws Exception {
        test("formats-valid", "format-invalid-date",
                new ExpectedMessage(LogLevel.ERROR, new Criteria("instance", "/birthDate", true)));
    }

    @Test
    public void testInvalidDateTime() throws Exception {
        test("formats-valid", "format-invalid-date-time",
                new ExpectedMessage(LogLevel.ERROR, new Criteria("instance", "/lastLogin", true)));
    }

    @Test
    public void testInvalidInt32() throws Exception {
        test("formats-valid", "format-invalid-int32",
                new ExpectedMessage(LogLevel.WARNING, new Criteria("key", "warn.format.int32.overflow")));
    }

    @Test
    public void testInvalidInt64() throws Exception {
        test("formats-valid", "format-invalid-int64",
                new ExpectedMessage(LogLevel.WARNING, new Criteria("key", "warn.format.int64.overflow")));
    }

    @Test
    public void testInvalidFloat() throws Exception {
        test("formats-valid", "format-invalid-float",
                new ExpectedMessage(LogLevel.WARNING, new Criteria("key", "warn.format.float.overflow")));
    }

    @Test
    public void testInvalidDouble() throws Exception {
        test("formats-valid", "format-invalid-double",
                new ExpectedMessage(LogLevel.WARNING, new Criteria("key", "warn.format.double.overflow")));
    }

    @Test
    public void testInvalidBase64() throws Exception {
        test("formats-valid", "format-invalid-base64",
                new ExpectedMessage(LogLevel.ERROR, new Criteria("key", "err.format.base64.invalid")));
    }

    @Test
    public void testMultipleValidationErrors() throws Exception {
        test("formats-valid", "format-invalid-multiple-messages",
                new ExpectedMessage(LogLevel.ERROR, new Criteria("instance", "/birthDate", true)),
                new ExpectedMessage(LogLevel.WARNING,
                        new Criteria("key", "warn.format.int64.overflow"),
                        new Criteria("instance", "/id", true)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("key", "err.format.base64.invalid"),
                        new Criteria("instance", "/encoded", true)));
    }

    @Test
    public void testInvalidTypes() throws Exception {
        test("formats-valid", "format-invalid-types",
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/age", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/archive", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/dbl", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/email", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/age", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/id", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)));
    }
}
