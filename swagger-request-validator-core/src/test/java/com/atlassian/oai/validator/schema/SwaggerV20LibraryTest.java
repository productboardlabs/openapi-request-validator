package com.atlassian.oai.validator.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.swagger.util.Json;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.atlassian.oai.validator.schema.SwaggerV20Library.schemaFactory;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static org.junit.Assert.fail;

/**
 * These tests are data driven from files in `/schema/*.json`.
 * <p>
 * Each test file contains:
 * <ul>
 *     <li>The keyword being exercised</li>
 *     <li>A description of the schema being tested</li>
 *     <li>The schema itself</li>
 *     <li>One of more tests to execute against the schema (for validation tests)</li>
 * </ul>
 */
@RunWith(Enclosed.class)
public class SwaggerV20LibraryTest {

    /**
     * Tests that exercise the validation of the schema itself
     */
    @RunWith(Parameterized.class)
    public static class SyntaxTests {
        private static final String[] TEST_CASE_FILES = {
                "discriminator-invalid-allOf-notRequired",
                "discriminator-invalid-allOf-emptyPropertyName",
                "discriminator-invalid-allOf-nullPropertyName",
                "discriminator-invalid-allOf-missingPropertyName",
                "discriminator-invalid-allOf-nonExistentProperty",
                "discriminator-invalid-allOf-nonStringType",
                "discriminator-invalid-allOf-nonObjectMapping",
                "discriminator-invalid-allOf-invalidMappingRefs",
                "discriminator-invalid-oneOf-subSchemaMissingProperty",
                "discriminator-invalid-oneOf-notRequired",
        };

        @Parameters(name = "{0}: {1} SHOULD {2}")
        public static Iterable<Object[]> params() {
            return () -> stream(TEST_CASE_FILES)
                    .map(SyntaxTests::loadTest)
                    .iterator();
        }

        @Parameterized.Parameter(0)
        public String keywordUnderTest;

        @Parameterized.Parameter(1)
        public String schemaDescription;

        @Parameterized.Parameter(2)
        public String passFailMsg;

        @Parameterized.Parameter(3)
        public TestCase testCase;

        private static Object[] loadTest(final String testCaseFile) {
            try {
                final TestCase testCase = Json.mapper().treeToValue(loadTestCase(testCaseFile), TestCase.class);
                return new Object[]{
                        testCase.keyword,
                        testCase.description,
                        testCase.shouldPass ? "pass" : "fail",
                        testCase
                };
            } catch (final Exception e) {
                fail(e.getMessage());
            }
            return new Object[]{};
        }

        @Test
        public void test() throws Exception {
            final JsonSchema schema = schemaFactory().getJsonSchema(testCase.schema);
            final ProcessingReport report = schema.validateUnchecked(Json.mapper().createObjectNode());
            if (testCase.shouldPass) {
                assertPass(report);
            } else {
                assertFail(report, testCase.expectedKeys);
            }
        }

    }

    /**
     * Tests that exercise validation of incoming objects against a <em>valid</em> schema
     */
    @RunWith(Parameterized.class)
    public static class ValidationTests {
        private static final String[] TEST_CASE_FILES = {
                "discriminator-valid-allOf",
                "discriminator-valid-allOf-withAllOfComposition",
                "discriminator-valid-allOf-withArrays",
                "discriminator-valid-oneOf",
                // TODO: #289 - Use the discriminator to select between overlapping options
                // "discriminator-valid-oneOf-withOverlappingSchema",
                "discriminator-valid-anyOf",
                "nullable-valid"
        };

        private static final JsonSchemaFactory FACTORY = schemaFactory();

        @Parameters(name = "{0}: {1} WITH {2} SHOULD {3}")
        public static Iterable<Object[]> params() {
            return () -> stream(TEST_CASE_FILES)
                    .flatMap(ValidationTests::loadTests)
                    .iterator();
        }

        @Parameterized.Parameter(0)
        public String keywordUnderTest;

        @Parameterized.Parameter(1)
        public String schemaDescription;

        @Parameterized.Parameter(2)
        public String testDescription;

        @Parameterized.Parameter(3)
        public String passFailMsg;

        @Parameterized.Parameter(4)
        public JsonSchema schema;

        @Parameterized.Parameter(5)
        public TestDetails testDetails;

        private static Object[] buildCaseFromTest(final TestDetails t, final TestCase testCase,
                                                  final JsonSchema schema, final String nameSuffix) {
            return new Object[]{
                    testCase.keyword,
                    testCase.description,
                    t.description,
                    (t.shouldPass ? "pass" : "fail") + nameSuffix,
                    schema,
                    t
            };
        }

        private static Stream<Object[]> loadTests(final String testCaseFile) {
            try {
                final TestCase testCase = Json.mapper().treeToValue(loadTestCase(testCaseFile), TestCase.class);
                final JsonSchema schema = FACTORY.getJsonSchema(testCase.schema);
                // Run each test two times, with the same schema, to ensure that the schema itself isn't mangled
                return Stream.concat(
                        testCase.tests
                                .stream()
                                .map(t -> buildCaseFromTest(t, testCase, schema, "")),
                        testCase.tests
                                .stream()
                                .map(t -> buildCaseFromTest(t, testCase, schema, " again"))
                );
            } catch (final Exception e) {
                fail(e.getMessage());
            }
            return Stream.empty();
        }

        @Test
        public void test() {
            final ProcessingReport report = schema.validateUnchecked(testDetails.example);
            if (testDetails.shouldPass) {
                assertPass(report);
            } else {
                assertFail(report, testDetails.expectedKeys);
            }
        }
    }

    @Ignore("Not actually tests")
    public static class TestCase {
        public String keyword;
        public String description;
        public boolean shouldPass = true;
        public String[] expectedKeys = {};
        public List<TestDetails> tests = emptyList();
        public JsonNode schema;
    }

    @Ignore("Not actually tests")
    public static class TestDetails {
        public String description = "Any example";
        public boolean shouldPass;
        public JsonNode example = Json.mapper().createObjectNode();
        public String[] expectedKeys = {};
    }

    private static JsonNode loadTestCase(final String name) throws Exception {
        return JsonLoader.fromResource("/schema/" + name + ".json");
    }

    private static void assertFail(final ProcessingReport report, final String... expectedMsgs) {
        if (report.isSuccess()) {
            fail("Expected validation failure.");
        }

        final StringBuilder builder = new StringBuilder("Report missing expected errors. \nFound errors: [");
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
        builder.append("\nKeys:\n");
        keys.forEach(k -> builder.append("- ").append(k).append("\n"));

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
