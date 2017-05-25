package com.atlassian.oai.validator.wiremock;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class WireMockRequestTest {

    @Test
    public void getPath_returnsCorrectPath_whenQueryStringPresent() {
        final Request request = Mockito.mock(Request.class);
        when(request.getUrl()).thenReturn("/some/path?foo=bar&foop=barf");

        final WireMockRequest classUnderTest = new WireMockRequest(request);

        assertThat(classUnderTest.getPath(), is("/some/path"));
    }

    @Test
    public void getPath_returnsCorrectPath_whenQueryStringNotPresent() {
        final Request request = Mockito.mock(Request.class);
        when(request.getUrl()).thenReturn("/some/path?");

        final WireMockRequest classUnderTest = new WireMockRequest(request);

        assertThat(classUnderTest.getPath(), is("/some/path"));
    }

    @Test
    public void getQueryParamValues_returnsEmpty_whenNoParam() {
        final Request request = Mockito.mock(Request.class);
        when(request.getUrl()).thenReturn("/some/path?foo=bar&foop=barf");

        final WireMockRequest classUnderTest = new WireMockRequest(request);

        assertThat(classUnderTest.getQueryParameterValues("none"), empty());
    }

    @Test
    public void getQueryParamValues_returnsValue_whenParam() {
        final Request request = Mockito.mock(Request.class);
        when(request.getUrl()).thenReturn("/some/path?foo=bar&foop=barf");

        final WireMockRequest classUnderTest = new WireMockRequest(request);

        assertThat(classUnderTest.getQueryParameterValues("foo"), contains("bar"));
        assertThat(classUnderTest.getQueryParameterValues("foop"), contains("barf"));
    }

    @Test
    public void getQueryParamValues_returnsEmptyStringValue_whenQueryParamHasNoValue() {
        final Request request = Mockito.mock(Request.class);
        when(request.getUrl()).thenReturn("/some/path?foo");
        final WireMockRequest classUnderTest = new WireMockRequest(request);

        assertThat(classUnderTest.getQueryParameterValues("foo"), contains(""));
    }

    @Test
    public void getHeaderValues_doesCaseInsensitiveLookup() {
        final Request request = Mockito.mock(Request.class);
        when(request.getUrl()).thenReturn("/some/path");
        when(request.getHeaders()).thenReturn(
                new HttpHeaders(new HttpHeader("X-My-Header", "foop", "barf"))
        );

        final WireMockRequest classUnderTest = new WireMockRequest(request);

        assertThat(classUnderTest.getHeaderValues("x-my-header"), contains("foop", "barf"));
        assertThat(classUnderTest.getHeaderValue("x-MY-header").isPresent(), is(true));

        assertThat(classUnderTest.getHeaderValues("not-a-header").isEmpty(), is(true));
        assertThat(classUnderTest.getHeaderValue("not-a-header").isPresent(), is(false));
    }

    @Test
    public void supportsAllRequestMethods() {
        Arrays.stream(RequestMethod.values()).forEach(m -> {
            // Wiremock supports an "any" method for matching,
            // but will not be present in requests validated in the filter
            if (m.getName().equalsIgnoreCase(RequestMethod.ANY.getName())) {
                return;
            }

            final Request request = Mockito.mock(Request.class);
            when(request.getUrl()).thenReturn("/some/path");
            when(request.getMethod()).thenReturn(m);

            final WireMockRequest classUnderTest = new WireMockRequest(request);
            assertThat(classUnderTest.getMethod(),
                    is(com.atlassian.oai.validator.model.Request.Method.valueOf(m.getName())));
        });
    }

}