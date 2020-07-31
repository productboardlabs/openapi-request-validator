package com.atlassian.oai.validator.schema.format;

import com.github.fge.jsonschema.core.report.LogLevel;
import org.junit.Test;

public class SwaggerV20AttributeTest extends AbstractAttributeTest {

    @Test
    public void testValid() throws Exception {
        test("format-valid");
    }

    @Test
    public void testInvalidDate() throws Exception {
        test("format-invalid-date",
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/date", true)));
    }

    @Test
    public void testInvalidDateTime() throws Exception {
        test("format-invalid-date-time",
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/date-time", true)));
    }

    @Test
    public void testInvalidIPv4() throws Exception {
        test("format-invalid-ipv4",
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/ipv4", true)));
    }

    @Test
    public void testInvalidIPv6() throws Exception {
        test("format-invalid-ipv6",
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/ipv6", true)));
    }

    @Test
    public void testInvalidUri() throws Exception {
        test("format-invalid-uri",
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/uri", true)));
    }

    @Test
    public void testInvalidUuid() throws Exception {
        test("format-invalid-uuid",
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/uuid", true)));
    }

    @Test
    public void testInvalidInt32() throws Exception {
        test("format-invalid-int32-overflow",
                new ExpectedMessage(LogLevel.ERROR, new Criteria("key", "err.format.int32.overflow")));
    }

    @Test
    public void testInvalidInt64() throws Exception {
        test("format-invalid-int64-overflow",
                new ExpectedMessage(LogLevel.ERROR, new Criteria("key", "err.format.int64.overflow")));
    }

    @Test
    public void testInvalidFloat() throws Exception {
        test("format-invalid-float-overflow",
                new ExpectedMessage(LogLevel.ERROR, new Criteria("key", "err.format.float.overflow")));
    }

    @Test
    public void testInvalidDouble() throws Exception {
        test("format-invalid-double-overflow",
                new ExpectedMessage(LogLevel.ERROR, new Criteria("key", "err.format.double.overflow")));
    }

    @Test
    public void testInvalidBase64() throws Exception {
        test("format-invalid-base64",
                new ExpectedMessage(LogLevel.ERROR, new Criteria("key", "err.format.base64.invalidLength")));
    }

    @Test
    public void testMultipleValidationErrors() throws Exception {
        test("format-invalid-multiple-messages",
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/date", true)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("key", "err.format.int64.overflow"),
                        new Criteria("instance", "/int64", true)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("key", "err.format.base64.invalidLength"),
                        new Criteria("instance", "/byte", true)));
    }

    @Test
    public void testInvalidTypes() throws Exception {
        test("format-invalid-types",
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/int32", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/boolean", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/double", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/email", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/ipv4", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/ipv6", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/uri", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)),
                new ExpectedMessage(LogLevel.ERROR,
                        new Criteria("instance", "/int64", true),
                        new Criteria("keyword", "type", false),
                        new Criteria("domain", "validation", false)));
    }
}
