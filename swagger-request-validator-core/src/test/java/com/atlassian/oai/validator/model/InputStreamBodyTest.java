package com.atlassian.oai.validator.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class InputStreamBodyTest {
    private static InputStreamBody inputStreamBody(final byte[] bytes) {
        return new InputStreamBody(new ByteArrayInputStream(bytes));
    }

    private static InputStreamBody inputStreamBody(final String string) {
        return inputStreamBody(string.getBytes());
    }

    @Test
    public void hasBody_falseIfEmpty() {
        assertThat(inputStreamBody("").hasBody(), is(false));
        assertThat(inputStreamBody(new byte[0]).hasBody(), is(false));
    }

    @Test
    public void hasBody_trueIfNotEmpty() {
        assertThat(inputStreamBody("test").hasBody(), is(true));
        assertThat(inputStreamBody(" ").hasBody(), is(true));
        assertThat(inputStreamBody(new byte[]{1}).hasBody(), is(true));
    }

    @Test
    public void toJsonNode_convertsTheContentIntoAJsonNode() throws IOException {
        final JsonNode result = inputStreamBody("{\"key\":\"value\"}").toJsonNode();
        assertThat(result.toPrettyString(), equalTo("" +
                "{\n" +
                "  \"key\" : \"value\"\n" +
                "}"
        ));
    }

    @Test
    public void toJsonNode_doesWorkEvenAfterPeekingTheInputStream() throws IOException {
        final Body body = inputStreamBody("{\"key\":\"value\"}");
        assertThat(body.hasBody(), is(true));
        assertThat(body.toJsonNode().toString(), equalTo("{\"key\":\"value\"}"));
    }

    @Test
    public void toString_convertsTheContentIntoAString() throws IOException {
        assertThat(inputStreamBody("123").toString(UTF_8), equalTo("123"));
    }

    @Test
    public void toString_doesWorkEvenAfterPeekingTheInputStream() throws IOException {
        final Body body = inputStreamBody("123");
        assertThat(body.hasBody(), is(true));
        assertThat(body.toString(UTF_8), equalTo("123"));
    }
}
