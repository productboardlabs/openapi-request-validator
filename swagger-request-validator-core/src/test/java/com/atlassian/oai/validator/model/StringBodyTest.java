package com.atlassian.oai.validator.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StringBodyTest {
    @Test
    public void hasBody_falseIfEmpty() {
        assertThat(new StringBody("", UTF_8).hasBody(), is(false));
        assertThat(new StringBody("", UTF_16).hasBody(), is(false));
    }

    @Test
    public void hasBody_trueIfNotEmpty() {
        assertThat(new StringBody("test", UTF_8).hasBody(), is(true));
        assertThat(new StringBody(" ", UTF_8).hasBody(), is(true));
    }

    @Test
    public void toJsonNode_convertsTheContentIntoAJsonNode() throws IOException {
        final JsonNode result = new StringBody("{\"key\":\"value\"}", UTF_16).toJsonNode();
        assertThat(result.toPrettyString(), equalTo("" +
                "{\n" +
                "  \"key\" : \"value\"\n" +
                "}"
        ));
    }

    @Test
    public void toString_convertsTheContentIntoAString() {
        assertThat(new StringBody("123", UTF_8).toString(UTF_8), equalTo("123"));
        assertThat(new StringBody("\u003c", UTF_16).toString(UTF_8), is(new String("\u003c".getBytes(UTF_16), UTF_8)));
    }
}
