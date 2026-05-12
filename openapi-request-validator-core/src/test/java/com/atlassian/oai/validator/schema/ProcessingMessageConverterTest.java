package com.atlassian.oai.validator.schema;

import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import io.swagger.util.Json;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ProcessingMessageConverterTest {

    private final ProcessingMessageConverter classUnderTest =
            new ProcessingMessageConverter(new MessageResolver());

    static Stream<TestData> params() {
        return Stream.of(
                new TestData("Simple processing message", "simple-message"),
                new TestData("Processing message with a pointer", "message-with-pointer"),
                new TestData("Processing message with nested reports", "nested-reports")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("params")
    public void run(final TestData testData) throws Exception {
        final TestCase testCase = load(testData.testCaseFile());
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

    record TestData(String name, String testCaseFile) {}

    private static class TestCase {
        public JsonNode input;
        public JsonNode expected;
    }

}