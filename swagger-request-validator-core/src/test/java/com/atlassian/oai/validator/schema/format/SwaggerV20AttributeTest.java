package com.atlassian.oai.validator.schema.format;

import com.github.fge.jsonschema.core.report.LogLevel;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SwaggerV20AttributeTest extends AbstractAttributeTest {

    @Test
    public void testValid() throws Exception {
        test("formats-valid", "format-valid", Collections.emptySet());
    }

    @Test
    public void testInvalidDate() throws Exception {
        final List<ExpectedMessage> expected = new LinkedList<>();
        expected.add(new ExpectedMessage(LogLevel.ERROR, new Criteria("instance", "/birthDate", true)));

        test("formats-valid", "format-invalid-date", expected);
    }

    @Test
    public void testInvalidDateTime() throws Exception {
        final List<ExpectedMessage> expected = new LinkedList<>();
        expected.add(new ExpectedMessage(LogLevel.ERROR, new Criteria("instance", "/lastLogin", true)));

        test("formats-valid", "format-invalid-date-time", expected);
    }

    @Test
    public void testInvalidInt32() throws Exception {
        final List<ExpectedMessage> expected = new LinkedList<>();
        expected.add(new ExpectedMessage(LogLevel.WARNING, new Criteria("key", "warn.format.int32.overflow")));

        test("formats-valid", "format-invalid-int32", expected);
    }

    @Test
    public void testInvalidInt64() throws Exception {
        final List<ExpectedMessage> expected = new LinkedList<>();
        expected.add(new ExpectedMessage(LogLevel.WARNING, new Criteria("key", "warn.format.int64.overflow")));

        test("formats-valid", "format-invalid-int64", expected);
    }

    @Test
    public void testInvalidFloat() throws Exception {
        final List<ExpectedMessage> expected = new LinkedList<>();
        expected.add(new ExpectedMessage(LogLevel.WARNING, new Criteria("key", "warn.format.float.overflow")));

        test("formats-valid", "format-invalid-float", expected);
    }

    @Test
    public void testInvalidDouble() throws Exception {
        final List<ExpectedMessage> expected = new LinkedList<>();
        expected.add(new ExpectedMessage(LogLevel.WARNING, new Criteria("key", "warn.format.double.overflow")));

        test("formats-valid", "format-invalid-double", expected);
    }

    @Test
    public void testInvalidBase64() throws Exception {
        final List<ExpectedMessage> expected = new LinkedList<>();
        expected.add(new ExpectedMessage(LogLevel.ERROR, new Criteria("key", "err.format.base64.invalid")));

        test("formats-valid", "format-invalid-base64", expected);
    }

    @Test
    public void testMultipleValidationErrors() throws Exception {
        final List<ExpectedMessage> expected = new LinkedList<>();

        final List<Criteria> criterion1 = new LinkedList<>();
        criterion1.add(new Criteria("instance", "/birthDate", true));
        expected.add(new ExpectedMessage(LogLevel.ERROR, criterion1));

        final List<Criteria> criterion2 = new LinkedList<>();
        criterion2.add(new Criteria("key", "warn.format.int64.overflow"));
        criterion2.add(new Criteria("instance", "/id", true));
        expected.add(new ExpectedMessage(LogLevel.WARNING, criterion2));

        final List<Criteria> criterion3 = new LinkedList<>();
        criterion3.add(new Criteria("key", "err.format.base64.invalid"));
        criterion3.add(new Criteria("instance", "/encoded", true));
        expected.add(new ExpectedMessage(LogLevel.ERROR, criterion3));

        test("formats-valid", "format-invalid-multiple-messages", expected);
    }

    @Test
    public void testInvalidTypes() throws Exception {
        final List<ExpectedMessage> expected = new LinkedList<>();

        final List<Criteria> criterion1 = new LinkedList<>();
        criterion1.add(new Criteria("instance", "/age", true));
        criterion1.add(new Criteria("keyword", "type", false));
        criterion1.add(new Criteria("domain", "validation", false));
        expected.add(new ExpectedMessage(LogLevel.ERROR, criterion1));

        final List<Criteria> criterion2 = new LinkedList<>();
        criterion2.add(new Criteria("instance", "/archive", true));
        criterion2.add(new Criteria("keyword", "type", false));
        criterion2.add(new Criteria("domain", "validation", false));
        expected.add(new ExpectedMessage(LogLevel.ERROR, criterion2));

        final List<Criteria> criterion3 = new LinkedList<>();
        criterion3.add(new Criteria("instance", "/dbl", true));
        criterion3.add(new Criteria("keyword", "type", false));
        criterion3.add(new Criteria("domain", "validation", false));
        expected.add(new ExpectedMessage(LogLevel.ERROR, criterion3));

        final List<Criteria> criterion4 = new LinkedList<>();
        criterion4.add(new Criteria("instance", "/email", true));
        criterion4.add(new Criteria("keyword", "type", false));
        criterion4.add(new Criteria("domain", "validation", false));
        expected.add(new ExpectedMessage(LogLevel.ERROR, criterion4));

        final List<Criteria> criterion5 = new LinkedList<>();
        criterion5.add(new Criteria("instance", "/id", true));
        criterion5.add(new Criteria("keyword", "type", false));
        criterion5.add(new Criteria("domain", "validation", false));
        expected.add(new ExpectedMessage(LogLevel.ERROR, criterion5));

        test("formats-valid", "format-invalid-types", expected);
    }
}
