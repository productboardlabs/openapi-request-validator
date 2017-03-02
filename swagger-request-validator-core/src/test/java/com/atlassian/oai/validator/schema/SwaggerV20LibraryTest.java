package com.atlassian.oai.validator.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.atlassian.oai.validator.schema.SwaggerV20Library.schemaFactory;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SwaggerV20LibraryTest {

    @Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][] {
            // Name, Schema, Example, Expected
            {"discriminator_shouldPass_whenValid",
                "discriminator-valid", "discriminator-valid",
                null
            },
            {"discriminator_shouldFail_whenInvalidDiscriminatorValue",
                "discriminator-valid", "discriminator-invalid-badDiscriminator",
                new String[]{"err.swaggerv2.discriminator.invalid"}
            },
            {"discriminator_shouldFail_whenMissingDiscriminator",
                "discriminator-valid", "discriminator-invalid-missingDiscriminator",
                new String[]{"err.swaggerv2.discriminator.missing"}
            },
            {"discriminator_shouldFail_whenEmptyDiscriminator",
                "discriminator-valid", "discriminator-invalid-emptyDiscriminator",
                new String[]{"err.swaggerv2.discriminator.missing"}
            },
            {"discriminator_shouldFail_whenNonStringDiscriminator",
                "discriminator-valid", "discriminator-invalid-nonStringDiscriminator",
                new String[]{"err.swaggerv2.discriminator.nonText"}
            },
            {"discriminator_shouldFail_whenDoesntMatchSubSchema",
                "discriminator-valid", "discriminator-invalid-doesntMatchSubSchema",
                new String[]{"err.swaggerv2.discriminator.fail"}
            },
            {"discriminator_shouldFail_whenEmptyValue",
                "discriminator-invalid-empty", "discriminator-valid",
                new String[]{"err.swaggerv2.discriminator.empty"}
            },
            {"discriminator_shouldFail_whenNotAProperty",
                "discriminator-invalid-noProperty", "discriminator-valid",
                new String[]{"err.swaggerv2.discriminator.noProperty"}
            },
            {"discriminator_shouldFail_whenNotAStringProperty",
                "discriminator-invalid-wrongType", "discriminator-valid",
                new String[]{"err.swaggerv2.discriminator.wrongType"}
            },
            {"discriminator_shouldFail_whenPropertyNotMarkedAsRequired",
                "discriminator-invalid-notRequired", "discriminator-valid",
                new String[]{"err.swaggerv2.discriminator.notRequired"}
            },
        });
    }

    private static JsonNode examples;

    static {
        try {
            examples = JsonLoader.fromResource("/schema/examples.json");
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private final String name;
    private final String schema;
    private final String example;
    private final String[] expectedMsgs;

    public SwaggerV20LibraryTest(final String name, final String schema, final String example, final String[] expectedMsgs) {
        this.name = name;
        this.schema = schema;
        this.example = example;
        this.expectedMsgs = expectedMsgs;
    }

    @Test
    public void test() throws Exception {

        final ProcessingReport report = schemaFactory()
                .getJsonSchema(loadSchema(schema))
                .validateUnchecked(loadExample(example));

        if (expectedMsgs == null) {
            assertPass(report);
        } else {
            assertFail(report, expectedMsgs);
        }
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
            }
        });
        builder.append("\n]");

        for (String key : expectedMsgs) {
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

    private JsonNode loadExample(final String name) throws Exception {
        return examples.get(name);
    }

    private JsonNode loadSchema(final String name) throws Exception {
        return JsonLoader.fromResource("/schema/" + name + ".json");
    }

}
