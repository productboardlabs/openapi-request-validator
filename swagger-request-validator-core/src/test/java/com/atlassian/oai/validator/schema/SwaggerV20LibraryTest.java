package com.atlassian.oai.validator.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import io.swagger.util.Json;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.atlassian.oai.validator.schema.SwaggerV20Library.schemaFactory;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SwaggerV20LibraryTest {

    private static final String[] TEST_CASE_FILES = {
            "discriminator-valid-allOf",
            "discriminator-invalid-allOf-notRequired",
            "discriminator-invalid-allOf-emptyProperty",
            "discriminator-invalid-allOf-nonExistentProperty",
            "discriminator-invalid-allOf-nonStringType",
            "nullable-valid"
    };

    @Parameters(name = "{1}: {2} WITH {3} SHOULD {4}")
    public static Iterable<Object[]> params() {
        return stream(TEST_CASE_FILES)
                .flatMap(file -> loadTests(file).stream())
                .collect(toList());
    }

    @Parameterized.Parameter(0)
    public String testCaseFile;

    @Parameterized.Parameter(1)
    public String keywordUnderTest;

    @Parameterized.Parameter(2)
    public String schemaDescription;

    @Parameterized.Parameter(3)
    public String testDescription;

    @Parameterized.Parameter(4)
    public String passFailMsg;

    @Parameterized.Parameter(5)
    public JsonNode schemaNode;

    @Parameterized.Parameter(6)
    public TestDetails testDetails;

    private static List<Object[]> loadTests(final String testCaseFile) {
        try {
            final JsonNode testCase = loadTestCase(testCaseFile);
            final List<Object[]> result = new ArrayList<>();
            final Iterator<JsonNode> tests = testCase.get("tests").elements();
            while (tests.hasNext()) {
                final JsonNode t = tests.next();
                final TestDetails testDetails = Json.mapper().treeToValue(t, TestDetails.class);
                result.add(new Object[]{
                        testCaseFile,
                        testCase.get("keyword").textValue(),
                        testCase.get("description").textValue(),
                        testDetails.description,
                        testDetails.shouldPass ? "pass" : "fail",
                        testCase.get("schema"),
                        testDetails
                });
            }
            return result;
        } catch (final Exception e) {
            e.printStackTrace();
            return emptyList();
        }
    }

    @Test
    public void test() throws Exception {
        final JsonSchema schema = schemaFactory().getJsonSchema(schemaNode);
        final ProcessingReport report = schema.validateUnchecked(testDetails.example);
        if (testDetails.shouldPass) {
            assertPass(report);
        } else {
            assertFail(report, testDetails.expectedKeys);
        }
    }

    public static class TestDetails {
        public String description;
        public boolean shouldPass;
        public JsonNode example;
        public String[] expectedKeys = {};
    }

    private static JsonNode loadTestCase(final String name) throws Exception {
        return JsonLoader.fromResource("/schema/" + name + ".json");
    }

    private static void assertFail(final ProcessingReport report, final String... expectedMsgs) {
        if (report.isSuccess()) {
            fail("Expected validation failure.");
        }

        final StringBuilder builder = new StringBuilder("Report missing expected errors. Found errors: [");
        final Set<String> keys = new HashSet<>();
        report.forEach(pm -> {
            builder.append('\n').append(pm.toString().replace("\n", "\n\t"));
            final JsonNode msgJson = pm.asJson();
            if (msgJson.has("key")) {
                keys.add(msgJson.get("key").textValue());
            } else if (msgJson.has("keyword")) {
                keys.add(format("%s.%s.%s",
                        msgJson.get("level").textValue(),
                        msgJson.get("domain").textValue(),
                        msgJson.get("keyword").textValue())
                );
            }
        });
        builder.append("\n]");

        for (final String key : expectedMsgs) {
            if (!keys.contains(key)) {
                fail(builder.toString());
            }
        }
    }

    private static void assertPass(final ProcessingReport report) {
        if (report.isSuccess()) {
            return;
        }
        final StringBuilder builder = new StringBuilder("Report contains unexpected errors: [");
        report.forEach(pm -> {
            builder.append('\n').append(pm.toString().replace("\n", "\n\t"));
        });
        builder.append("\n]");
        fail(builder.toString());
    }

}
