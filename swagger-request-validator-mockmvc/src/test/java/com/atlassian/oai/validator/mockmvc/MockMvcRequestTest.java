package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.model.Request;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 *
 */
public class MockMvcRequestTest {

    @Test
    public void mapsRequestComponentsCorrectly() throws Exception {
        final MockHttpServletRequest mockHttpServletRequest = MockMvcRequestBuilders
                .get("/path")
                .header("X-My-Header", "foo", "bar")
                .buildRequest(new MockServletConfig().getServletContext());

        final MockMvcRequest classUnderTest = new MockMvcRequest(mockHttpServletRequest);

        assertThat(classUnderTest.getPath(), is("/path"));
        assertThat(classUnderTest.getMethod(), is(Request.Method.GET));
        assertThat(classUnderTest.getBody().isPresent(), is(false));
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

        final MockMvcRequest classUnderTest = new MockMvcRequest(mockHttpServletRequest);

        assertThat(classUnderTest.getQueryParameters(), contains("queryParam"));
        assertThat(classUnderTest.getQueryParameterValues("queryParam"), contains("value1"));

    }

    @Test
    public void getBody_returnsEmpty_whenNoBodyInRequest() throws Exception {
        final MockHttpServletRequest mockHttpServletRequest = MockMvcRequestBuilders
                .get("/path")
                .buildRequest(new MockServletConfig().getServletContext());

        final MockMvcRequest classUnderTest = new MockMvcRequest(mockHttpServletRequest);

        assertThat(classUnderTest.getBody(), is(Optional.empty()));
    }

    @Test
    public void getBody_returnsBody_whenBodyInRequest() throws Exception {
        final MockHttpServletRequest mockHttpServletRequest = MockMvcRequestBuilders
                .get("/path")
                .content("The body")
                .buildRequest(new MockServletConfig().getServletContext());

        final MockMvcRequest classUnderTest = new MockMvcRequest(mockHttpServletRequest);

        assertThat(classUnderTest.getBody().get(), is("The body"));
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

        final MockMvcRequest classUnderTest = new MockMvcRequest(mockHttpServletRequest);

        assertThat(classUnderTest.getMethod(), is(httpMethod));
    }
}
