package com.atlassian.oai.validator.schema;

import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import io.swagger.util.Json;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(Parameterized.class)
public class ProcessingMessageConverterTest {

    private final ProcessingMessageConverter classUnderTest =
            new ProcessingMessageConverter(new MessageResolver());

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Object[][] params() {
        return new Object[][]{
                {"Simple processing message", "simple-message"},
                {"Processing message with a pointer", "message-with-pointer"},
                {"Processing message with nested reports", "nested-reports"}
        };
    }

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String testCaseFile;

    @Test
    public void run() throws Exception {
        final TestCase testCase = load(testCaseFile);
        final Message message = classUnderTest.toValidationReportMessage(testCase.input, null, "prefix");

        assertThat(message, is(notNullValue()));

        final String expected = testCase.expected.toPrettyString();
        final String actual = JsonValidationReportFormat.getInstance().apply(message);
        JSONAssert.assertEquals(actual, expected, actual, true);
    }

    private static TestCase load(final String name) throws Exception {
        final JsonNode testCase = JsonLoader.fromResource("/schema/messages/" + name + ".json");
        return Json.mapper().treeToValue(testCase, TestCase.class);
    }

    private static class TestCase {
        public JsonNode input;
        public JsonNode expected;
    }

}