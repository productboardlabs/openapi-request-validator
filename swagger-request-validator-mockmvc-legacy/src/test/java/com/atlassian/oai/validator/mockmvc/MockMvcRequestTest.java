package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.model.Request;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class MockMvcRequestTest {

    @Test
    public void mapsRequestComponentsCorrectly() throws Exception {
        final MockHttpServletRequest mockHttpServletRequest = MockMvcRequestBuilders
                .get("/path")
                .header("X-My-Header", "foo", "bar")
                .buildRequest(new MockServletConfig().getServletContext());

        final Request classUnderTest = MockMvcRequest.of(mockHttpServletRequest);

        assertThat(classUnderTest.getPath(), is("/path"));
        assertThat(classUnderTest.getMethod(), is(Request.Method.GET));
        assertThat(classUnderTest.getRequestBody().get().hasBody(), is(false));
        assertThat(classUnderTest.getHeaderValues("x-my-header"), contains("foo", "bar"));
        assertThat(classUnderTest.getHeaderValue("x-my-HEADER").isPresent(), is(true));
        assertThat(classUnderTest.getHeaderValue("not-a-header").isPresent(), is(false));
    }

    @Test
    public void mapsQueryParams() throws Exception {
        final MockHttpServletRequest mockHttpServletRequest = MockMvcRequestBuilders
                .get("/path")
                .param("queryParam", "value1")
                .buildRequest(new MockServletConfig().getServletContext());

        final Request classUnderTest = MockMvcRequest.of(mockHttpServletRequest);

        assertThat(classUnderTest.getQueryParameters(), contains("queryParam"));
        assertThat(classUnderTest.getQueryParameterValues("queryParam"), contains("value1"));
    }

    @Test
    public void getRequestBody_returnsEmpty_whenNoBodyInRequest() {
        final MockHttpServletRequest mockHttpServletRequest = MockMvcRequestBuilders
                .get("/path")
                .buildRequest(new MockServletConfig().getServletContext());

        final Request classUnderTest = MockMvcRequest.of(mockHttpServletRequest);

        assertThat(classUnderTest.getRequestBody().get().hasBody(), is(false));
    }

    @Test
    public void getRequestBody_returnsEmpty_whenNoBodyInRequest_usingSpringPre437() throws Exception {
        // In Spring pre 4.3.7 mockHttpServletRequest.getReader() returns null if there is no content.
        // This was changed in 4.3.7 by SPR-15215 to return an empty reader.
        final MockHttpServletRequest mockHttpServletRequest = mock(MockHttpServletRequest.class);
        when(mockHttpServletRequest.getMethod()).thenReturn("GET");
        when(mockHttpServletRequest.getPathInfo()).thenReturn("/");
        when(mockHttpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockHttpServletRequest.getReader()).thenReturn(null);

        final Request classUnderTest = MockMvcRequest.of(mockHttpServletRequest);

        assertThat(classUnderTest.getRequestBody(), is(Optional.empty()));
    }

    @Test
    public void getRequestBody_returnsBody_whenBodyInRequest() throws Exception {
        final MockHttpServletRequest mockHttpServletRequest = MockMvcRequestBuilders
                .get("/path")
                .content("The body")
                .buildRequest(new MockServletConfig().getServletContext());

        final Request classUnderTest = MockMvcRequest.of(mockHttpServletRequest);

        assertThat(classUnderTest.getRequestBody().get().toString(StandardCharsets.UTF_8), is("The body"));
    }

    @Test
    public void getBody_doesntCloseReader() throws Exception {
        final MockHttpServletRequest mockHttpServletRequest = mock(MockHttpServletRequest.class);
        final BufferedReader reader = mock(BufferedReader.class);
        when(mockHttpServletRequest.getMethod()).thenReturn("GET");
        when(mockHttpServletRequest.getPathInfo()).thenReturn("/path");
        when(mockHttpServletRequest.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptySet()));
        when(mockHttpServletRequest.getReader()).thenReturn(reader);

        MockMvcRequest.of(mockHttpServletRequest);

        verify(reader, never()).close();
    }

    @Test
    public void supportsAllRequestMethods() throws Exception {
        captureRequest(get("/path"), Request.Method.GET);
        captureRequest(delete("/path"), Request.Method.DELETE);
        captureRequest(head("/path"), Request.Method.HEAD);
        captureRequest(options("/path"), Request.Method.OPTIONS);
        captureRequest(patch("/path"), Request.Method.PATCH);
        captureRequest(post("/path"), Request.Method.POST);
        captureRequest(put("/path"), Request.Method.PUT);
    }

    private void captureRequest(final MockHttpServletRequestBuilder mockHttpServletRequestBuilder,
                                final Request.Method httpMethod) throws Exception {

        final MockHttpServletRequest mockHttpServletRequest = mockHttpServletRequestBuilder
                .buildRequest(new MockServletConfig().getServletContext());

        final Request classUnderTest = MockMvcRequest.of(mockHttpServletRequest);

        assertThat(classUnderTest.getMethod(), is(httpMethod));
    }
}
