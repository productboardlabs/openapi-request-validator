package com.atlassian.oai.validator.schema.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.format.FormatAttribute;
import com.github.fge.jsonschema.format.draftv3.DateAttribute;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.atlassian.oai.validator.schema.SwaggerV20Library.schemaFactory;
import static org.junit.Assert.fail;

public class DateAttributeTest {

    public FormatAttribute attr = DateAttribute.getInstance();

    private static JsonNode examples;

    static {
        try {
            examples = JsonLoader.fromResource("/schema/formats-data.json");
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testValid() throws Exception {
        final String schema = "formats-valid";
        final String example = "format-valid";

        final ProcessingReport report = schemaFactory()
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

    @Test
    public void testInvalid() throws Exception {
        final String schema = "formats-valid";
        final String example = "format-invalid-date";

        final ProcessingReport report = schemaFactory(LogLevel.WARNING, LogLevel.FATAL)
                .getJsonSchema(loadSchema(schema))
                .validateUnchecked(loadExample(example));

        if (report.isSuccess()) {
            fail("Expected validation failure.");
        }

        final String[] expected = {"/birthDate"};

        final StringBuilder builder = new StringBuilder("Report missing expected errors. Found errors: [");
        final Set<String> instances = new HashSet<>();
        report.forEach(pm -> {
            builder.append('\n').append(pm.toString().replace("\n", "\n\t"));
            final JsonNode msgJson = pm.asJson();
            if (msgJson.has("level")) {
                final String level = msgJson.get("level").textValue();
                if ("error".equals(level) && msgJson.has("instance")) {
                    instances.add(((ObjectNode) msgJson.get("instance")).get("pointer").textValue());
                }
            }
        });
        builder.append("\n]");

        for (String key : expected) {
            if (!instances.contains(key)) {
                fail(builder.toString());
            }
        }
    }

    private JsonNode loadExample(final String name) throws Exception {
        return examples.get(name);
    }

    private JsonNode loadSchema(final String name) throws Exception {
        return JsonLoader.fromResource("/schema/" + name + ".json");
    }
}
