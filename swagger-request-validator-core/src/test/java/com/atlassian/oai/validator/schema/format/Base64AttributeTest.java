package com.atlassian.oai.validator.schema.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.AbstractProcessingReport;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.tree.JsonTree;
import com.github.fge.jsonschema.core.tree.SimpleJsonTree;
import com.github.fge.jsonschema.format.FormatAttribute;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.google.common.io.BaseEncoding;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class Base64AttributeTest {

    private static final Logger log = getLogger(Base64AttributeTest.class);

    private final FormatAttribute attribute = Base64Attribute.getInstance();

    private static boolean isStrictlyValidBase64(final String text) {
        // prior to version 2.7.1 this regex was used to check the string is valid Base64
        return text.matches("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$");
    }

    private TestProcessingReport validate(final String text) throws ProcessingException {
        final JsonTree jsonTree = new SimpleJsonTree(new ObjectMapper().valueToTree(text));
        final MessageBundle messageBundle = MessageBundle.newBuilder().freeze();
        final FullData fullData = new FullData(null, jsonTree);

        final TestProcessingReport processingReport = new TestProcessingReport();
        attribute.validate(processingReport, messageBundle, fullData);
        return processingReport;
    }

    @Test
    public void validate_validBase64() throws ProcessingException {
        // when:
        final TestProcessingReport processingReport = validate("QmFzZTY0");

        // then: 'no error is added to the report'
        assertValidationPassed(processingReport);
    }

    @Test
    public void validate_validBase64_withPadding() throws ProcessingException {
        // when:
        final TestProcessingReport processingReport = validate("Base64==");

        // then: 'no error is added to the report'
        assertValidationPassed(processingReport);
    }

    @Test
    public void validate_validBase64_massTest() throws Exception {
        final Random random = new Random(0); // random generator with fixed seed for deterministic tests

        for (int i = 0; i < 1000; ++i) {
            // setup:
            final byte[] array = new byte[i];
            random.nextBytes(array);
            final String text = Base64.getEncoder().encodeToString(array);

            // when:
            final TestProcessingReport processingReport = validate(text);

            // then: 'no error is added to the report'
            assertValidationPassed(processingReport);
        }
    }

    @Test
    public void validate_invalidBase64_invalidLetter() throws ProcessingException {
        // when:
        final TestProcessingReport processingReport = validate("QmF@ZTY0");

        // then: 'as the string is no Base64 an error is written to the report'
        assertValidationFailed(processingReport, "err.format.base64.invalid");
    }

    @Test
    public void validate_invalidWithMissingPadding() throws Exception {
        final Random random = new Random(1); // random generator with fixed seed for deterministic tests
        int invalidCount = 0;

        for (int i = 0; i < 1000; ++i) {
            // setup:
            final byte[] array = new byte[i];
            random.nextBytes(array);
            final String text = BaseEncoding.base64().omitPadding().encode(array);

            // when:
            final TestProcessingReport processingReport = validate(text);

            // then:
            if (isStrictlyValidBase64(text)) {
                assertValidationPassed(processingReport);
            } else {
                assertValidationFailed(processingReport, "err.format.base64.invalidLength");
                ++invalidCount;
            }
        }

        // safety check: out of all tries ⅔ were invalid
        assertThat(invalidCount, equalTo(666));
    }

    private void assertValidationPassed(final TestProcessingReport processingReport) {
        assertThat(processingReport.getMessages(), is(empty()));
    }

    private void assertValidationFailed(final TestProcessingReport processingReport,
                                        final String expectedKey) {
        assertThat(processingReport.getMessages(), is(not(empty())));
        assertThat(processingReport.containsErrorWithKey(expectedKey), is(true));
    }

    private static class TestProcessingReport extends AbstractProcessingReport {

        private final List<ProcessingMessage> messages = new ArrayList<>();

        @Override
        public void log(final LogLevel level, final ProcessingMessage message) {
            messages.add(message);
        }

        public List<ProcessingMessage> getMessages() {
            return messages;
        }

        boolean containsErrorWithKey(final String key) {
            return messages.stream()
                    .anyMatch(m ->
                            m.getLogLevel() == LogLevel.ERROR &&
                                    m.asJson().get("key").textValue().equals(key)
                    );
        }
    }
}
