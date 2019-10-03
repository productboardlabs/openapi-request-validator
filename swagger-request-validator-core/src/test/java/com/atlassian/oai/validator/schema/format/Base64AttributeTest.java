package com.atlassian.oai.validator.schema.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.tree.JsonTree;
import com.github.fge.jsonschema.format.FormatAttribute;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;
import org.junit.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class Base64AttributeTest {

    private final FormatAttribute attribute = Base64Attribute.getInstance();

    @Test
    public void validate_validBase64() throws ProcessingException {
        // setup:
        final String base64String = "QmFzZTY0";

        // and:
        final ProcessingReport processingReport = mock(ProcessingReport.class);
        final JsonTree jsonTree = mock(JsonTree.class);
        final JsonNode jsonNode = mock(JsonNode.class);
        final MessageBundle messageBundle = MessageBundle.newBuilder().freeze();
        final FullData fullData = new FullData(null, jsonTree);

        // and:
        when(jsonTree.getNode()).thenReturn(jsonNode);
        when(jsonNode.textValue()).thenReturn(base64String);

        // when:
        attribute.validate(processingReport, messageBundle, fullData);

        // then: 'no error is added to the report'
        verifyNoMoreInteractions(processingReport);
    }

    @Test
    public void validate_invalidBase64() throws ProcessingException {
        // setup:
        final String noBase64String = "QmFz@ZTY0";

        // and:
        final ProcessingReport processingReport = mock(ProcessingReport.class);
        final JsonTree jsonTree = mock(JsonTree.class);
        final JsonNode jsonNode = mock(JsonNode.class);
        final MessageBundle messageBundle = MessageBundle.newBuilder().freeze();
        final FullData fullData = new FullData(null, jsonTree);

        // and:
        when(jsonTree.getNode()).thenReturn(jsonNode);
        when(jsonNode.textValue()).thenReturn(noBase64String);

        // when:
        attribute.validate(processingReport, messageBundle, fullData);

        // then: 'as the string is no Base64 an error is written to the report'
        verify(processingReport).error(any(ProcessingMessage.class));
    }
}
