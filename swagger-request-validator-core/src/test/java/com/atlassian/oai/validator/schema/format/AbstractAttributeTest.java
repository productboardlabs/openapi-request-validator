package com.atlassian.oai.validator.schema.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

import static com.atlassian.oai.validator.schema.SwaggerV20Library.schemaFactory;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedCollection;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractAttributeTest {

    private static final Logger log = getLogger(AbstractAttributeTest.class);

    private static JsonNode examples;

    static {
        try {
            examples = JsonLoader.fromResource("/schema/format/formats-data.json");
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    void test(final String example, final ExpectedMessage... expectedMsgs) throws Exception {
        final ProcessingReport report = schemaFactory(LogLevel.WARNING, LogLevel.FATAL)
                .getJsonSchema(loadSchema("supported-formats"))
                .validateUnchecked(loadExample(example));

        final Collection<ExpectedMessage> expectedMessages = synchronizedCollection(new ArrayList<>(asList(expectedMsgs)));
        final Collection<ProcessingMessage> unexpectedMessages = synchronizedCollection(new LinkedList<>());

        log.trace("Processing report:");
        report.forEach(pm -> {
            log.trace(pm.toString().replace("\n", "\n\t"));

            final LogLevel logLevel = pm.getLogLevel();
            if (logLevel == LogLevel.INFO || logLevel == LogLevel.DEBUG) {
                return;
            }
            unexpectedMessages.add(pm);
            final Iterator<ExpectedMessage> it = expectedMessages.iterator();
            while (it.hasNext()) {
                final ExpectedMessage expected = it.next();
                if (expected.logLevel != logLevel) {
                    continue;
                }

                final JsonNode msgJson = pm.asJson();
                final boolean matching = expected.criterion.stream()
                        .allMatch(c -> c.matches(msgJson));

                if (matching) {
                    unexpectedMessages.remove(pm);
                    it.remove();
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
                        sb.append("\n\t\t").append(c.key).append(" -> ").append(c.value);
                    });
                    sb.append("\n\t]");
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
        return JsonLoader.fromResource("/schema/format/" + name + ".json");
    }

    static class ExpectedMessage {

        private final LogLevel logLevel;
        private final Collection<Criteria> criterion;

        ExpectedMessage(final LogLevel logLevel, final Criteria... criteria) {
            this.logLevel = logLevel;
            criterion = asList(criteria);
        }
    }

    static class Criteria {

        private final String key;
        private final String value;
        private final boolean pointer;

        Criteria(final String key, final String value) {
            this.key = key;
            this.value = value;
            pointer = false;
        }

        Criteria(final String key, final String value, final boolean pointer) {
            this.key = key;
            this.value = value;
            this.pointer = pointer;
        }

        boolean matches(final JsonNode msgJson) {
            if (msgJson.has(key)) {
                final String value = pointer ?
                        msgJson.get(key).get("pointer").textValue() :
                        msgJson.get(key).textValue();
                return Objects.equals(this.value, value);
            }
            return false;
        }
    }
}
