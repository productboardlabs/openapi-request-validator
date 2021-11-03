package com.atlassian.oai.validator.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Test;

import java.util.Optional;

import static com.atlassian.oai.validator.util.HttpParsingUtils.extractMultipartBoundary;
import static com.atlassian.oai.validator.util.HttpParsingUtils.isMultipartContentTypeAcceptedByConsumer;
import static com.atlassian.oai.validator.util.HttpParsingUtils.parseUrlEncodedFormDataBodyAsJson;
import static com.atlassian.oai.validator.util.HttpParsingUtils.parseUrlEncodedFormDataBodyAsJsonNode;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadRawRequest;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class HttpParsingUtilsTest {

    @Test
    public void isMultipartContentTypeAcceptedByConsumer_accepted_whenSame() {
        assertTrue(isMultipartContentTypeAcceptedByConsumer(
                "multipart/form-data; boundary=---------------------------735323031399963166993862150",
                "multipart/form-data; boundary=---------------------------735323031399963166993862150"
        ));
    }

    @Test
    public void isMultipartContentTypeAcceptedByConsumer_rejected_whenDifferent() {
        assertFalse(isMultipartContentTypeAcceptedByConsumer(
                "multipart/form-data; boundary=---------------------------735323031399963166993862150",
                "multipart/form-data; boundary=---------------------------foobar"
        ));

        assertFalse(isMultipartContentTypeAcceptedByConsumer(
                "multipart/form-data; boundary=---------------------------foobar",
                "multipart/form-data; boundary=---------------------------735323031399963166993862150"
        ));
    }

    @Test
    public void isMultipartContentTypeAcceptedByConsumer_rejected_whenSameButNotMultipart() {
        assertFalse(isMultipartContentTypeAcceptedByConsumer(
                "text/plain",
                "text/plain"
        ));
    }

    @Test
    public void isMultipartContentTypeAcceptedByConsumer_accepted_whenWithBoundary() {
        assertTrue(isMultipartContentTypeAcceptedByConsumer(
                "multipart/form-data; boundary=---------------------------foobar",
                "multipart/form-data"
        ));
    }

    @Test
    public void extractMultipartBoundary_extractsBoundary() {
        assertEquals(extractMultipartBoundary("multipart/form-data; boundary=foobar").get(), "foobar");
    }

    @Test
    public void extractMultipartBoundary_returnsEmpty_ifNoBoundary() {
        assertEquals(extractMultipartBoundary("multipart/form-data"), Optional.empty());
    }

    @Test
    public void parseToJsonNode_successfullyParsesData() {
        final JsonNode tree = parseUrlEncodedFormDataBodyAsJsonNode(loadRawRequest("formdata-request"));

        assertThat(tree.has("string"), is(true));
        assertThat(tree.get("string"), instanceOf(TextNode.class));

        assertThat(tree.has("double"), is(true));
        assertThat(tree.get("double"), instanceOf(DoubleNode.class));

        assertThat(tree.has("bool"), is(true));
        assertThat(tree.get("bool"), instanceOf(BooleanNode.class));

        assertThat(tree.has("stringArray"), is(true));
        assertThat(tree.get("stringArray"), instanceOf(ArrayNode.class));

        assertThat(tree.has("numArray"), is(true));
        final JsonNode numArray = tree.get("numArray");
        assertThat(numArray, instanceOf(ArrayNode.class));
        assertThat((Iterable<JsonNode>) () -> numArray.elements(), everyItem(instanceOf(LongNode.class)));

        assertThat(tree.has("solo"), is(true));
        assertThat(tree.get("solo"), instanceOf(NullNode.class));
    }

    @Test
    public void parseToJson_successfullyParsesData() throws Exception {
        final String json = parseUrlEncodedFormDataBodyAsJson(loadRawRequest("formdata-request"));

        // on first deserialization the num array will be of type LongNode, on second it will be IntNode - force Long to get equality between
        // the result and the expected value
        final JsonNode tree = new ObjectMapper()
                .configure(DeserializationFeature.USE_LONG_FOR_INTS, true)
                .readTree(json);
        final JsonNode expected = parseUrlEncodedFormDataBodyAsJsonNode(loadRawRequest("formdata-request"));

        assertThat(tree, equalTo(expected));
    }
}
