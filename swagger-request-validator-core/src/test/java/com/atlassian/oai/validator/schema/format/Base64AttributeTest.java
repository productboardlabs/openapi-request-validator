package com.atlassian.oai.validator.schema.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.tree.JsonTree;
import com.github.fge.jsonschema.core.tree.SimpleJsonTree;
import com.github.fge.jsonschema.format.FormatAttribute;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.google.common.io.BaseEncoding;
import org.junit.Test;

import java.util.Base64;
import java.util.Random;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class Base64AttributeTest {

    private final FormatAttribute attribute = Base64Attribute.getInstance();

    private static boolean isStrictlyValidBase64(final String text) {
        // prior to version 2.7.1 this regex was used to check the string is valid Base64
        return text.matches("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$");
    }

    private ProcessingReport validate(final String text) throws ProcessingException {
        final JsonTree jsonTree = new SimpleJsonTree(new ObjectMapper().valueToTree(text));
        final MessageBundle messageBundle = MessageBundle.newBuilder().freeze();
        final FullData fullData = new FullData(null, jsonTree);

        final ProcessingReport processingReport = mock(ProcessingReport.class);
        attribute.validate(processingReport, messageBundle, fullData);
        return processingReport;
    }

    @Test
    public void validate_validBase64() throws ProcessingException {
        // when:
        final ProcessingReport processingReport = validate("QmFzZTY0");

        // then: 'no error is added to the report'
        verifyNoMoreInteractions(processingReport);
    }

    @Test
    public void validate_validBase64_withPadding() throws ProcessingException {
        // when:
        final ProcessingReport processingReport = validate("Base64==");

        // then: 'no error is added to the report'
        verifyNoMoreInteractions(processingReport);
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
            final ProcessingReport processingReport = validate(text);

            // then: 'no error is added to the report'
            verifyNoMoreInteractions(processingReport); //
        }
    }

    @Test
    public void validate_invalidBase64_invalidLetter() throws ProcessingException {
        // when:
        final ProcessingReport processingReport = validate("QmF@ZTY0");

        // then: 'as the string is no Base64 an error is written to the report'
        verify(processingReport).error(any(ProcessingMessage.class));
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
            final ProcessingReport processingReport = validate(text);

            // then:
            if (isStrictlyValidBase64(text)) {
                verifyNoMoreInteractions(processingReport);
            } else {
                verify(processingReport).error(any(ProcessingMessage.class));
                ++invalidCount;
            }
        }

        // safety check: out of all tries ⅔ were invalid
        assertThat(invalidCount, equalTo(666));
    }
}
