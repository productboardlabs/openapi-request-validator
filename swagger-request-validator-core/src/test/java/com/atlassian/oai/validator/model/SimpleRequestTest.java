package com.atlassian.oai.validator.model;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class SimpleRequestTest {

    @Test
    public void header_areNotMandatory() {
        final Request request = SimpleRequest.Builder.get("/path").build();

        assertThat(request.getHeaders().isEmpty(), is(true));
        assertThat(request.getHeaderValues("foo"), empty());
        assertThat(request.getHeaderValue("foo").isPresent(), is(false));
    }

    @Test
    public void header_namesAreCaseInsensitive() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withHeader("foo")
                .withHeader("Foo", "bar0")
                .withHeader("fOO", "bar1", "bar2")
                .withHeader("FOO", Arrays.asList("bar3", "bar4"))
                .build();

        assertThat(request.getHeaders().keySet(), containsInAnyOrder("foo"));
        assertThat(request.getHeaders().get("foo"), containsInAnyOrder("", "bar0", "bar1", "bar2", "bar3", "bar4"));
        assertThat(request.getHeaders().get("FoO"), containsInAnyOrder("", "bar0", "bar1", "bar2", "bar3", "bar4"));
        assertThat(request.getHeaderValues("FOO"), containsInAnyOrder("", "bar0", "bar1", "bar2", "bar3", "bar4"));
        assertThat(request.getHeaderValues("foo"), containsInAnyOrder("", "bar0", "bar1", "bar2", "bar3", "bar4"));
        assertThat(request.getHeaderValue("Foo").get(), equalTo("")); // the first set value to "foo"
    }

    @Test
    public void header_unsetValuesConsideredAsEmpty() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withHeader("foo")
                .build();

        assertThat(request.getHeaders().keySet(), containsInAnyOrder("foo"));
        assertThat(request.getHeaders().get("foo"), containsInAnyOrder(""));
        assertThat(request.getHeaderValues("foo"), containsInAnyOrder(""));
        assertThat(request.getHeaderValue("foo").get(), isEmptyString());
    }

    @Test
    public void header_valuesCanBeEmpty() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withHeader("foo", "")
                .build();

        assertThat(request.getHeaders().keySet(), containsInAnyOrder("foo"));
        assertThat(request.getHeaders().get("foo"), containsInAnyOrder(""));
        assertThat(request.getHeaderValues("foo"), containsInAnyOrder(""));
        assertThat(request.getHeaderValue("foo").get(), isEmptyString());
    }

    @Test
    public void header_valuesCanBeNull() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withHeader("foo", (String[]) null)
                .build();

        assertThat(request.getHeaders().keySet(), containsInAnyOrder("foo"));
        assertThat(request.getHeaders().get("foo"), containsInAnyOrder(""));
        assertThat(request.getHeaderValues("foo"), containsInAnyOrder(""));
        assertThat(request.getHeaderValue("foo").get(), isEmptyString());
    }
    
    @Test
    public void header_valuesCanHaveOneValue() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withHeader("foo", "bar")
                .build();

        assertThat(request.getHeaders().keySet(), containsInAnyOrder("foo"));
        assertThat(request.getHeaders().get("foo"), containsInAnyOrder("bar"));
        assertThat(request.getHeaderValues("foo"), containsInAnyOrder("bar"));
        assertThat(request.getHeaderValue("foo").get(), equalTo("bar"));
    }

    @Test
    public void header_valuesCanHaveMultipleValues() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withHeader("fOO", "bar1", "bar2")
                .build();

        assertThat(request.getHeaders().keySet(), containsInAnyOrder("fOO"));
        assertThat(request.getHeaders().get("foo"), containsInAnyOrder("bar1", "bar2"));
        assertThat(request.getHeaderValues("foo"), containsInAnyOrder("bar1", "bar2"));
        assertThat(request.getHeaderValue("foo").get(), equalTo("bar1"));
    }

    @Test
    public void header_dateValueIsNotSplit() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withHeader("fOO", "Sun, 06 Nov 1994 08:49:37 GMT")
                .build();

        assertThat(request.getHeaders().keySet(), containsInAnyOrder("fOO"));
        assertThat(request.getHeaders().get("foo"), containsInAnyOrder("Sun, 06 Nov 1994 08:49:37 GMT"));
        assertThat(request.getHeaderValues("foo"), containsInAnyOrder("Sun, 06 Nov 1994 08:49:37 GMT"));
        assertThat(request.getHeaderValue("foo").get(), equalTo("Sun, 06 Nov 1994 08:49:37 GMT"));
    }

    @Test
    public void header_doesNotSplit() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withHeader("fOO", "Sun, 06 Nov 1994 08:49:37 GMT, trolls, Tue, 17 Jul 2018 11:10:04 GMT, goblins")
                .build();

        assertThat(request.getHeaders().keySet(), containsInAnyOrder("fOO"));
        assertThat(request.getHeaders().get("foo"),
                containsInAnyOrder("Sun, 06 Nov 1994 08:49:37 GMT, trolls, Tue, 17 Jul 2018 11:10:04 GMT, goblins"));
    }

    @Test
    public void queryParameter_areNotMandatory() {
        final Request request = SimpleRequest.Builder.get("/path").build();

        assertThat(request.getQueryParameters(), empty());
        assertThat(request.getQueryParameterValues("foo"), empty());
    }

    @Test
    public void queryParameter_namesCanBeCaseSensitive() {
        final Request request = new SimpleRequest.Builder(Request.Method.GET, "/path", true)
                .withQueryParam("foo")
                .withQueryParam("Foo", "bar0")
                .withQueryParam("fOO", "bar1", "bar2")
                .withQueryParam("FOO", Arrays.asList("bar3", "bar4"))
                .build();

        assertThat(request.getQueryParameters(), containsInAnyOrder("foo", "Foo", "fOO", "FOO"));
        assertThat(request.getQueryParameterValues("foo"), empty());
        assertThat(request.getQueryParameterValues("Foo"), containsInAnyOrder("bar0"));
        assertThat(request.getQueryParameterValues("fOO"), containsInAnyOrder("bar1", "bar2"));
        assertThat(request.getQueryParameterValues("FOO"), containsInAnyOrder("bar3", "bar4"));
        assertThat(request.getQueryParameterValues("FoO"), empty()); // was not set
    }

    @Test
    public void queryParameter_namesCanBeCaseInsensitive_whichIsTheDefault() {
        final Request request = new SimpleRequest.Builder(Request.Method.GET, "/path")
                .withQueryParam("foo")
                .withQueryParam("Foo", "bar0")
                .withQueryParam("fOO", "bar1", "bar2")
                .withQueryParam("FOO", Arrays.asList("bar3", "bar4"))
                .build();

        assertThat(request.getQueryParameters(), containsInAnyOrder("foo"));
        assertThat(request.getQueryParameterValues("foo"), containsInAnyOrder("bar0", "bar1", "bar2", "bar3", "bar4"));
        assertThat(request.getQueryParameterValues("FoO"), containsInAnyOrder("bar0", "bar1", "bar2", "bar3", "bar4"));
    }

    @Test
    public void queryParameter_valuesMightNotBeSet() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withQueryParam("foo")
                .build();

        assertThat(request.getQueryParameters(), containsInAnyOrder("foo"));
        assertThat(request.getQueryParameterValues("foo"), empty());
    }

    @Test
    public void queryParameter_valuesCanBeEmpty() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withQueryParam("foo", "")
                .build();

        assertThat(request.getQueryParameters(), containsInAnyOrder("foo"));
        assertThat(request.getQueryParameterValues("foo"), containsInAnyOrder(""));
    }

    @Test
    public void queryParameter_valuesCanBeNull() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withQueryParam("foo", (String[]) null)
                .build();

        assertThat(request.getQueryParameters(), containsInAnyOrder("foo"));
        assertThat(request.getQueryParameterValues("foo"), empty());
    }

    @Test
    public void queryParameter_valuesCanHaveOneValue() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withQueryParam("foo", "bar")
                .build();

        assertThat(request.getQueryParameters(), containsInAnyOrder("foo"));
        assertThat(request.getQueryParameterValues("foo"), containsInAnyOrder("bar"));
    }

    @Test
    public void queryParameter_valuesCanHaveMultipleValues() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withQueryParam("foo", "bar1", "bar2")
                .build();

        assertThat(request.getQueryParameters(), containsInAnyOrder("foo"));
        assertThat(request.getQueryParameterValues("foo"), containsInAnyOrder("bar1", "bar2"));
    }

    @Test
    public void body_isNotMandatory_andDoesNotNeedToBeSet() {
        final Request request = SimpleRequest.Builder.get("/path")
                .build();

        assertThat(request.getRequestBody().isPresent(), is(false));
    }

    @Test
    public void bodyString_isNotMandatory_andCanBeSetAsNull() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withBody((String) null)
                .build();

        assertThat(request.getRequestBody().isPresent(), is(false));
    }

    @Test
    public void bodyStringWithCharset_isNotMandatory_andCanBeSetAsNull() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withBody(null, null)
                .build();

        assertThat(request.getRequestBody().isPresent(), is(false));
    }

    @Test
    public void bodyStringWithCharset_isNotMandatory_evenWithASetCharset() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withBody(null, StandardCharsets.UTF_8)
                .build();

        assertThat(request.getRequestBody().isPresent(), is(false));
    }

    @Test
    public void bodyByteArray_isNotMandatory_andCanBeSetAsNull() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withBody((byte[]) null)
                .build();

        assertThat(request.getRequestBody().isPresent(), is(false));
    }

    @Test
    public void bodyInputStream_isNotMandatory_andCanBeSetAsNull() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withBody((InputStream) null)
                .build();

        assertThat(request.getRequestBody().isPresent(), is(false));
    }

    @Test
    public void bodyBody_isNotMandatory_andCanBeSetAsNull() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withBody((Body) null)
                .build();

        assertThat(request.getRequestBody().isPresent(), is(false));
    }

    @Test
    public void body_canBeSetAsString() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withBody("")
                .build();

        assertThat(request.getRequestBody().get(), instanceOf(StringBody.class));
    }

    @Test
    public void body_canBeSetAsStringAndCharset() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withBody("", StandardCharsets.UTF_16BE)
                .build();

        assertThat(request.getRequestBody().get(), instanceOf(StringBody.class));
    }

    @Test
    public void body_canBeSetAsString_andTheCharsetIsDeterminedByTheContentTypeHeader() throws IOException {
        final Request request = SimpleRequest.Builder.get("/path")
                .withBody("\u003c")
                .withContentType("text/plain;charset=utf-16")
                .build();

        assertThat(request.getRequestBody().get().toString(StandardCharsets.UTF_16), is("\u003c"));
        assertThat(request.getRequestBody().get().toString(StandardCharsets.UTF_8),
                is(new String("\u003c".getBytes(StandardCharsets.UTF_16), StandardCharsets.UTF_8)));
    }

    @Test
    public void body_canBeSetAsString_andTheCharsetIsUTF8DefinedIfContentTypeHeaderNotDefined() throws IOException {
        final Request request = SimpleRequest.Builder.get("/path")
                .withBody("\u003c")
                .build();

        assertThat(request.getRequestBody().get().toString(StandardCharsets.UTF_8),
                is(new String("\u003c".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));
    }

    @Test
    public void body_canBeSetAsByteArray() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withBody(new byte[0])
                .build();

        assertThat(request.getRequestBody().get(), instanceOf(ByteArrayBody.class));
    }

    @Test
    public void body_canBeSetAsInputStream() {
        final Request request = SimpleRequest.Builder.get("/path")
                .withBody(new ByteArrayInputStream(new byte[0]))
                .build();

        assertThat(request.getRequestBody().get(), instanceOf(InputStreamBody.class));
    }

    @Test
    public void body_canBeSetAsBody() {
        final Body body = mock(Body.class);
        final Request request = SimpleRequest.Builder.get("/path")
                .withBody(body)
                .build();

        assertThat(request.getRequestBody().get(), is(body));
    }

    @Test(expected = NullPointerException.class)
    public void method_isMandatory_1() {
        new SimpleRequest.Builder((String) null, "/path");
    }

    @Test(expected = NullPointerException.class)
    public void method_isMandatory_2() {
        new SimpleRequest.Builder((Request.Method) null, "/path");
    }

    @Test
    public void method_allSupportedHttpMethodsCanBeSet_asEnum() {
        Arrays.stream(Request.Method.values()).forEach(method -> {
            final Request request = new SimpleRequest.Builder(method, "/path").build();

            assertThat(request.getMethod(), is(method));
        });
    }

    @Test
    public void method_allSupportedHttpMethodsCanBeSet_asCaseInsensitiveString_1() {
        Arrays.stream(Request.Method.values()).forEach(method -> {
            final String methodAsString = StringUtils.capitalize(method.name().toLowerCase());
            final Request request = new SimpleRequest.Builder(methodAsString, "/path").build();

            assertThat(request.getMethod(), is(method));
        });
    }

    @Test
    public void method_allSupportedHttpMethodsCanBeSet_asCaseInsensitiveString_2() {
        Arrays.stream(Request.Method.values()).forEach(method -> {
            final String methodAsString = method.name().toLowerCase();
            final Request request = new SimpleRequest.Builder(methodAsString, "/path").build();

            assertThat(request.getMethod(), is(method));
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void method_notSupportedHttpMethodsCanNotBeSet() {
        new SimpleRequest.Builder("NOT", "/path");
    }

    @Test
    public void method_convenienceMethods() {
        assertThat(SimpleRequest.Builder.get("").build().getMethod(), is(Request.Method.GET));
        assertThat(SimpleRequest.Builder.post("").build().getMethod(), is(Request.Method.POST));
        assertThat(SimpleRequest.Builder.put("").build().getMethod(), is(Request.Method.PUT));
        assertThat(SimpleRequest.Builder.patch("").build().getMethod(), is(Request.Method.PATCH));
        assertThat(SimpleRequest.Builder.delete("").build().getMethod(), is(Request.Method.DELETE));
        assertThat(SimpleRequest.Builder.head("").build().getMethod(), is(Request.Method.HEAD));
        assertThat(SimpleRequest.Builder.options("").build().getMethod(), is(Request.Method.OPTIONS));
        assertThat(SimpleRequest.Builder.trace("").build().getMethod(), is(Request.Method.TRACE));

        // if this test fails, the supported HTTP methods have changed
        //     in case a new HTTP method was added please add a new convenience method to the builder, too
        //     in case a HTTP method was dropped please remove the according convenience method from the builder
        assertThat(Arrays.asList(Request.Method.values()), containsInAnyOrder(
                Request.Method.GET, Request.Method.POST, Request.Method.PUT, Request.Method.PATCH,
                Request.Method.DELETE, Request.Method.HEAD, Request.Method.OPTIONS, Request.Method.TRACE
        ));
    }

    @Test(expected = NullPointerException.class)
    public void path_isMandatory() {
        new SimpleRequest.Builder("GET", null);
    }

    @Test
    public void path_canBeEmpty() {
        final Request request = SimpleRequest.Builder.get("").build();

        assertThat(request.getPath(), isEmptyString());
    }

    @Test
    public void path_canBeSet() {
        final Request request = SimpleRequest.Builder.get("/path/is/set").build();

        assertThat(request.getPath(), equalTo("/path/is/set"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getQueryParameterValues_isUnmodifiable() {
        SimpleRequest.Builder.get("/path").build().getQueryParameterValues("foo").add("bar");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getQueryParameters_isUnmodifiable() {
        SimpleRequest.Builder.get("/path").build().getQueryParameters().add("bar");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getHeaders_isUnmodifiable() {
        SimpleRequest.Builder.get("/path").build().getHeaders().put("foo", Arrays.asList("bar"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getHeaderValues_isUnmodifiable() {
        SimpleRequest.Builder.get("/path").build().getHeaderValues("foo").add("bar");
    }
}
