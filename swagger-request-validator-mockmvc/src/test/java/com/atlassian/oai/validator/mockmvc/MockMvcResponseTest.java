package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.model.Response;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

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
    public void canGetBody() throws Exception {
        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        mockHttpServletResponse.getWriter().append("The Body");
        mockHttpServletResponse.setContentLength("The Body".length());
        final Response mockMvcResponse = MockMvcResponse.of(mockHttpServletResponse);

        assertThat(mockMvcResponse.getBody().isPresent(), is(true));
        assertThat(mockMvcResponse.getBody().get(), is("The Body"));
    }

    @Test
    public void getBodyIsEmptyIfThereIsNoContent() throws Exception {
        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        final Response mockMvcResponse = MockMvcResponse.of(mockHttpServletResponse);

        assertThat(mockMvcResponse.getBody().isPresent(), is(false));
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
