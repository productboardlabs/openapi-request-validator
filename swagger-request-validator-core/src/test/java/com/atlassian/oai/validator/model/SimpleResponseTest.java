package com.atlassian.oai.validator.model;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertThat;

public class SimpleResponseTest {

    @Test
    public void header_areNotMandatory() {
        final Response response = SimpleResponse.Builder.ok().build();

        assertThat(response.getHeaderValues("foo"), empty());
        assertThat(response.getHeaderValue("foo").isPresent(), is(false));
    }

    @Test
    public void header_namesAreCaseInsensitive() {
        final Response response = SimpleResponse.Builder.ok()
                .withHeader("foo")
                .withHeader("Foo", "bar0")
                .withHeader("fOO", "bar1", "bar2")
                .withHeader("FOO", Arrays.asList("bar3", "bar4"))
                .build();

        assertThat(response.getHeaderValues("FOO"), containsInAnyOrder("", "bar0", "bar1", "bar2", "bar3", "bar4"));
        assertThat(response.getHeaderValues("foo"), containsInAnyOrder("", "bar0", "bar1", "bar2", "bar3", "bar4"));
        assertThat(response.getHeaderValue("Foo").get(), equalTo("")); // the first set value to "foo"
    }

    @Test
    public void header_unsetValuesConsideredAsEmpty() {
        final Response response = SimpleResponse.Builder.ok()
                .withHeader("foo")
                .build();

        assertThat(response.getHeaderValues("foo"), containsInAnyOrder(""));
        assertThat(response.getHeaderValue("foo").get(), isEmptyString());
    }

    @Test
    public void header_valuesCanBeEmpty() {
        final Response response = SimpleResponse.Builder.ok()
                .withHeader("foo", "")
                .build();

        assertThat(response.getHeaderValues("foo"), containsInAnyOrder(""));
        assertThat(response.getHeaderValue("foo").get(), isEmptyString());
    }

    @Test
    public void header_valuesCanHaveOneValue() {
        final Response response = SimpleResponse.Builder.ok()
                .withHeader("foo", "bar")
                .build();

        assertThat(response.getHeaderValues("foo"), containsInAnyOrder("bar"));
        assertThat(response.getHeaderValue("foo").get(), equalTo("bar"));
    }

    @Test
    public void header_valuesCanHaveMultipleValues() {
        final Response response = SimpleResponse.Builder.ok()
                .withHeader("fOO", "bar1", "bar2")
                .build();

        assertThat(response.getHeaderValues("foo"), containsInAnyOrder("bar1", "bar2"));
        assertThat(response.getHeaderValue("foo").get(), equalTo("bar1"));
    }

    @Test
    public void body_isNotMandatory_andDoesNotNeedToBeSet() {
        final Response response = SimpleResponse.Builder.ok()
                .build();

        assertThat(response.getResponseBody().isPresent(), is(false));
    }

    @Test
    public void bodyString_isNotMandatory_andCanBeSetAsNull() {
        final Response response = SimpleResponse.Builder.ok()
                .withBody((String) null)
                .build();

        assertThat(response.getResponseBody().isPresent(), is(false));
    }

    @Test
    public void bodyStringWithCharset_isNotMandatory_andCanBeSetAsNull() {
        final Response response = SimpleResponse.Builder.ok()
                .withBody(null, null)
                .build();

        assertThat(response.getResponseBody().isPresent(), is(false));
    }

    @Test
    public void bodyStringWithCharset_isNotMandatory_evenWithASetCharset() {
        final Response response = SimpleResponse.Builder.ok()
                .withBody(null, StandardCharsets.UTF_8)
                .build();

        assertThat(response.getResponseBody().isPresent(), is(false));
    }

    @Test
    public void bodyByteArray_isNotMandatory_andCanBeSetAsNull() {
        final Response response = SimpleResponse.Builder.ok()
                .withBody((byte[]) null)
                .build();

        assertThat(response.getResponseBody().isPresent(), is(false));
    }

    @Test
    public void bodyInputStream_isNotMandatory_andCanBeSetAsNull() {
        final Response response = SimpleResponse.Builder.ok()
                .withBody((InputStream) null)
                .build();

        assertThat(response.getResponseBody().isPresent(), is(false));
    }

    @Test
    public void body_canBeSetAsString() {
        final Response response = SimpleResponse.Builder.ok()
                .withBody("")
                .build();

        assertThat(response.getResponseBody().get(), instanceOf(StringBody.class));
    }

    @Test
    public void body_canBeSetAsStringAndCharset() {
        final Response response = SimpleResponse.Builder.ok()
                .withBody("", StandardCharsets.UTF_16BE)
                .build();

        assertThat(response.getResponseBody().get(), instanceOf(StringBody.class));
    }

    @Test
    public void body_canBeSetAsString_andTheCharsetIsDeterminedByTheContentTypeHeader() throws IOException {
        final Response response = SimpleResponse.Builder.ok()
                .withBody("\u003c")
                .withContentType("text/plain;charset=utf-16")
                .build();

        assertThat(response.getResponseBody().get().toString(StandardCharsets.UTF_16), is("\u003c"));
        assertThat(response.getResponseBody().get().toString(StandardCharsets.UTF_8),
                is(new String("\u003c".getBytes(StandardCharsets.UTF_16), StandardCharsets.UTF_8)));
    }

    @Test
    public void body_canBeSetAsByteArray() {
        final Response response = SimpleResponse.Builder.ok()
                .withBody(new byte[0])
                .build();

        assertThat(response.getResponseBody().get(), instanceOf(ByteArrayBody.class));
    }

    @Test
    public void body_canBeSetAsInputStream() {
        final Response response = SimpleResponse.Builder.ok()
                .withBody(new ByteArrayInputStream(new byte[0]))
                .build();

        assertThat(response.getResponseBody().get(), instanceOf(InputStreamBody.class));
    }

    @Test
    public void status_isSet() {
        final Response response = SimpleResponse.Builder.status(101).build();

        assertThat(response.getStatus(), equalTo(101));
    }

    @Test
    public void status_convenienceMethods() {
        assertThat(SimpleResponse.Builder.ok().build().getStatus(), is(200));
        assertThat(SimpleResponse.Builder.noContent().build().getStatus(), is(204));
        assertThat(SimpleResponse.Builder.badRequest().build().getStatus(), is(400));
        assertThat(SimpleResponse.Builder.notFound().build().getStatus(), is(404));
        assertThat(SimpleResponse.Builder.serverError().build().getStatus(), is(500));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getHeaderValues_isUnmodifiable() {
        SimpleResponse.Builder.ok().build().getHeaderValues("foo").add("bar");
    }
}
