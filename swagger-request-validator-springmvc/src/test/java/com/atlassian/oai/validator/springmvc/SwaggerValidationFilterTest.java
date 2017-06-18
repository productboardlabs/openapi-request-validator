package com.atlassian.oai.validator.springmvc;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.times;

public class SwaggerValidationFilterTest {

    private SwaggerValidationFilter classUnderTest = new SwaggerValidationFilter();

    @Test
    public void doFilterInternal_wrapsTheServletRequest() throws ServletException, IOException {
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        final FilterChain filterChain = Mockito.mock(FilterChain.class);
        Mockito.when(servletRequest.getContentLengthLong()).thenReturn(24L);

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request shall be wrapped and added to the filter chain
        Mockito.verify(filterChain, times(1))
                .doFilter(any(ResettableRequestServletWrapper.class), same(servletResponse));
    }

    @Test
    public void doFilterInternal_noWrappingIfContentIsToLong() throws ServletException, IOException {
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        final FilterChain filterChain = Mockito.mock(FilterChain.class);
        Mockito.when(servletRequest.getContentLengthLong()).thenReturn(1L + Integer.MAX_VALUE);

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request wasn't wrapped
        Mockito.verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void doFilterInternal_noWrappingIfAsyncRequest() throws ServletException, IOException {
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        final FilterChain filterChain = Mockito.mock(FilterChain.class);
        Mockito.when(servletRequest.getContentLengthLong()).thenReturn(24L);
        Mockito.when(servletRequest.getAttribute(WebAsyncUtils.WEB_ASYNC_MANAGER_ATTRIBUTE)).thenReturn(null);
        // long-wided way to fake an async request
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final WebAsyncManager webAsyncManager = invocationOnMock.getArgumentAt(1, WebAsyncManager.class);
                final Field field = WebAsyncManager.class.getDeclaredField("concurrentResult");
                ReflectionUtils.makeAccessible(field);
                field.set(webAsyncManager, "Async");
                return null;
            }
        }).when(servletRequest).setAttribute(eq(WebAsyncUtils.WEB_ASYNC_MANAGER_ATTRIBUTE), any(WebAsyncManager.class));

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request wasn't wrapped
        Mockito.verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }
}
