package com.atlassian.oai.validator.wiremock;

import com.github.tomakehurst.wiremock.http.Request;
import org.junit.Test;
import org.mockito.Mockito;

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

}