package com.atlassian.oai.validator.schema.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.format.FormatAttribute;
import com.github.fge.jsonschema.format.draftv3.DateAttribute;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.atlassian.oai.validator.schema.SwaggerV20Library.schemaFactory;
import static org.junit.Assert.fail;

public abstract class AbstractAttributeTest {

    public FormatAttribute attr = DateAttribute.getInstance();

    private static JsonNode examples;

    static {
        try {
            examples = JsonLoader.fromResource("/schema/formats-data.json");
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    void testValid(final String schema, final String example) throws Exception {
        final ProcessingReport report = schemaFactory(LogLevel.WARNING, LogLevel.FATAL)
                .getJsonSchema(loadSchema(schema))
                .validateUnchecked(loadExample(example));

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

    void testWarning(final String schema, final String example, final String key, final boolean pointer, final String[] expected) throws Exception {
        final ProcessingReport report = schemaFactory(LogLevel.WARNING, LogLevel.FATAL)
                .getJsonSchema(loadSchema(schema))
                .validateUnchecked(loadExample(example));

        if (!report.isSuccess()) {
            final StringBuilder builder = new StringBuilder("Report contains unexpected errors: [");
            report.forEach(pm -> {
                builder.append('\n').append(pm.toString().replace("\n", "\n\t"));
            });
            builder.append("\n]");
            fail(builder.toString());
        }

        checkMessages(report, "warning", key, pointer, expected);
    }

    void testError(final String schema, final String example, final String key, final boolean pointer, final String[] expected) throws Exception {
        final ProcessingReport report = schemaFactory(LogLevel.WARNING, LogLevel.FATAL)
                .getJsonSchema(loadSchema(schema))
                .validateUnchecked(loadExample(example));

        if (report.isSuccess()) {
            fail("Expected validation failure.");
        }

        checkMessages(report, "error", key, pointer, expected);
    }

    private void checkMessages(final ProcessingReport report, final String level, final String key, final boolean pointer, final String[] expected) {
        final StringBuilder builder = new StringBuilder("Report missing expected errors. Found errors: [");
        final Set<String> values = new HashSet<>();
        report.forEach(pm -> {
            builder.append('\n').append(pm.toString().replace("\n", "\n\t"));
            final JsonNode msgJson = pm.asJson();

            boolean ignore = false;
            if (level != null && msgJson.has("level")) {
                final String l = msgJson.get("level").textValue();
                ignore = !level.equals(l);
            }

            if (!ignore && msgJson.has(key)) {
                values.add(pointer ? ((ObjectNode) msgJson.get(key)).get("pointer").textValue() : msgJson.get(key).textValue());
            }
        });
        builder.append("\n]");

        for (String k : expected) {
            if (!values.contains(k)) {
                fail(builder.toString());
            }
        }
    }

    JsonNode loadExample(final String name) throws Exception {
        return examples.get(name);
    }

    JsonNode loadSchema(final String name) throws Exception {
        return JsonLoader.fromResource("/schema/" + name + ".json");
    }
}
