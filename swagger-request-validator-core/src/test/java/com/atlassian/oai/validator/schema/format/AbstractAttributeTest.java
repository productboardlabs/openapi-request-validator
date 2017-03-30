package com.atlassian.oai.validator.schema.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.format.FormatAttribute;
import com.github.fge.jsonschema.format.draftv3.DateAttribute;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

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

    void test(final String schema, final String example, final ExpectedMessage... expectedMsgs) throws Exception {
        final ProcessingReport report = schemaFactory(LogLevel.WARNING, LogLevel.FATAL)
                .getJsonSchema(loadSchema(schema))
                .validateUnchecked(loadExample(example));

        final Collection<ExpectedMessage> expectedMessages = Collections.synchronizedCollection(new ArrayList<>(Arrays.asList(expectedMsgs)));
        final Collection<ProcessingMessage> unexpectedMessages = Collections.synchronizedCollection(new LinkedList<>());
        report.forEach(pm -> {
            final LogLevel logLevel = pm.getLogLevel();
            if (logLevel != LogLevel.INFO && logLevel != logLevel.DEBUG) {
                unexpectedMessages.add(pm);
                final Iterator<ExpectedMessage> it = expectedMessages.iterator();
                while (it.hasNext()) {
                    final ExpectedMessage expected = it.next();
                    if (expected.logLevel == logLevel) {
                        final JsonNode msgJson = pm.asJson();

                        boolean matching = true;
                        for (Criteria c : expected.criterion) {
                            if (msgJson.has(c.key)) {
                                final String value = c.pointer ? ((ObjectNode) msgJson.get(c.key)).get("pointer").textValue() : msgJson.get(c.key).textValue();
                                matching &= Objects.equals(c.value, value);
                            } else {
                                matching = false;
                            }
                        }

                        if (matching) {
                            unexpectedMessages.remove(pm);
                            it.remove();
                        }
                    }
                }
            }
        });

        if (!expectedMessages.isEmpty() || !unexpectedMessages.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            if (!unexpectedMessages.isEmpty()) {
                sb.append("\nReport contains unexpected messages: [");
                unexpectedMessages.forEach(unexpected -> {
                    sb.append('\n').append(unexpected.toString().replace("\n", "\n\t"));
                });
                sb.append("\n]");
            }
            if (!expectedMessages.isEmpty()) {
                sb.append("\nMissing messages from report: [");
                expectedMessages.forEach(expected -> {
                    sb.append("\n\t").append(expected.logLevel).append(": [");
                    expected.criterion.forEach(c -> {
                        sb.append("\n\t\t").append(c.key).append(" -> ").append(c.value).append('\n');
                    });
                    sb.append("\t]");
                });
                sb.append("\n]");
            }
            fail(sb.toString());
        }
    }

    JsonNode loadExample(final String name) throws Exception {
        return examples.get(name);
    }

    JsonNode loadSchema(final String name) throws Exception {
        return JsonLoader.fromResource("/schema/" + name + ".json");
    }

    static class ExpectedMessage {

        private final LogLevel logLevel;
        private final Collection<Criteria> criterion;

        public ExpectedMessage(final LogLevel logLevel, final Criteria... criteria) {
            this.logLevel = logLevel;
            this.criterion = Arrays.asList(criteria);
        }
    }

    static class Criteria {

        private final String key;
        private final String value;
        private final boolean pointer;

        public Criteria(final String key, final String value) {
            this.key = key;
            this.value = value;
            this.pointer = false;
        }

        public Criteria(final String key, final String value, final boolean pointer) {
            this.key = key;
            this.value = value;
            this.pointer = pointer;
        }
    }
}
