package com.atlassian.oai.validator.springmvc;

import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.times;

public class SwaggerValidationFilterTest {

    private SwaggerValidationFilter classUnderTest = new SwaggerValidationFilter();

    @Test
    public void doFilterInternal_wrapsTheServletRequest() throws ServletException, IOException {
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        final FilterChain filterChain = Mockito.mock(FilterChain.class);
        Mockito.when(servletRequest.getMethod()).thenReturn("OPTIONS");

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
    public void doFilterInternal_noWrappingIfCorsPreflight() throws ServletException, IOException {
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        final FilterChain filterChain = Mockito.mock(FilterChain.class);
        Mockito.when(servletRequest.getHeader("Origin")).thenReturn("https://bitbucket.org");
        Mockito.when(servletRequest.getHeader("Access-Control-Request-Method")).thenReturn("POST");
        Mockito.when(servletRequest.getMethod()).thenReturn("OPTIONS");

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request wasn't wrapped
        Mockito.verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }
}
