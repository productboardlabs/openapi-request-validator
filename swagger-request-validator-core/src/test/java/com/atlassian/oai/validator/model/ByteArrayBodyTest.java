package com.atlassian.oai.validator.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ByteArrayBodyTest {
    @Test
    public void hasBody_falseIfEmpty() {
        assertThat(new ByteArrayBody(new byte[0]).hasBody(), is(false));
        assertThat(new ByteArrayBody("".getBytes()).hasBody(), is(false));
    }

    @Test
    public void hasBody_trueIfNotEmpty() {
        assertThat(new ByteArrayBody(new byte[]{1}).hasBody(), is(true));
        assertThat(new ByteArrayBody("1".getBytes()).hasBody(), is(true));
    }

    @Test
    public void toJsonNode_convertsTheContentIntoAJsonNode() throws IOException {
        final JsonNode result = new ByteArrayBody("{\"key\":\"value\"}".getBytes()).toJsonNode();
        assertThat(result.toPrettyString(), equalTo("" +
                "{\n" +
                "  \"key\" : \"value\"\n" +
                "}"
        ));
    }

    @Test
    public void toString_convertsTheContentIntoAString() {
        assertThat(new ByteArrayBody("123".getBytes()).toString(UTF_8), equalTo("123"));
    }
}
