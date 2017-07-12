package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.model.Request;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class WireMockRequestTest {

    @Test
    public void getPath_returnsCorrectPath_whenQueryStringPresent() {
        final com.github.tomakehurst.wiremock.http.Request originalRequest = Mockito
                .mock(com.github.tomakehurst.wiremock.http.Request.class);
        when(originalRequest.getMethod()).thenReturn(RequestMethod.DELETE);
        when(originalRequest.getUrl()).thenReturn("/some/path?foo=bar&foop=barf");
        when(originalRequest.getHeaders()).thenReturn(
                new HttpHeaders(new HttpHeader("X-My-Header", "foop", "barf"))
        );

        final Request result = WireMockRequest.of(originalRequest);

        assertThat(result.getPath(), is("/some/path"));
        assertThat(result.getQueryParameters(), containsInAnyOrder("foo", "foop"));
        assertThat(result.getHeaders().keySet(), containsInAnyOrder("X-My-Header"));
    }

    @Test
    public void supportsAllRequestMethods() {
        Arrays.stream(RequestMethod.values()).forEach(m -> {
            // Wiremock supports an "any" method for matching,
            // but will not be present in requests validated in the filter
            if (m.getName().equalsIgnoreCase(RequestMethod.ANY.getName())) {
                return;
            }

            final com.github.tomakehurst.wiremock.http.Request request = Mockito
                    .mock(com.github.tomakehurst.wiremock.http.Request.class);
            when(request.getUrl()).thenReturn("/some/path");
            when(request.getMethod()).thenReturn(m);
            when(request.getHeaders()).thenReturn(new HttpHeaders());

            final Request result = WireMockRequest.of(request);
            assertThat(result.getMethod(), is(Request.Method.valueOf(m.getName())));
        });
    }
}