package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.model.Response;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MockMvcResponseTest {

    @Test(expected = NullPointerException.class)
    public void mockHttpServletResponseIsRequired() throws Exception {
        final MockHttpServletResponse mockHttpServletResponse = null;
        MockMvcResponse.of(mockHttpServletResponse);
    }

    @Test
    public void canGetResponseBody() throws Exception {
        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        mockHttpServletResponse.getWriter().append("The Body");
        mockHttpServletResponse.setContentLength("The Body".length());
        final Response mockMvcResponse = MockMvcResponse.of(mockHttpServletResponse);

        assertThat(mockMvcResponse.getResponseBody().isPresent(), is(true));
        assertThat(mockMvcResponse.getResponseBody().get().toString(StandardCharsets.UTF_8), is("The Body"));
    }

    @Test
    public void getResponseBodyIsEmptyIfThereIsNoContent() throws Exception {
        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        final Response mockMvcResponse = MockMvcResponse.of(mockHttpServletResponse);

        assertThat(mockMvcResponse.getResponseBody().get().hasBody(), is(false));
    }

    @Test
    public void canGetStatus() throws Exception {
        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        mockHttpServletResponse.setStatus(404);
        final Response mockMvcResponse = MockMvcResponse.of(mockHttpServletResponse);

        assertThat(mockMvcResponse.getStatus(), is(404));
    }

    @Test
    public void canGetHeaderValues() throws Exception {
        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        mockHttpServletResponse.addHeader("X-My-Header", "foo");
        mockHttpServletResponse.addHeader("X-My-Header", "bar");
        final Response mockMvcResponse = MockMvcResponse.of(mockHttpServletResponse);

        assertThat(mockMvcResponse.getHeaderValues("X-My-Header"), contains("foo", "bar"));
    }
}
