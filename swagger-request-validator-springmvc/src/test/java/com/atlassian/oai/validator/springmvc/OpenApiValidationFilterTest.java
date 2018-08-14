package com.atlassian.oai.validator.springmvc;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;

public class OpenApiValidationFilterTest {

    @Test
    public void doFilterInternal_wrapsTheServletRequestAndResponseIfNoCors() throws ServletException, IOException {
        // given:
        final OpenApiValidationFilter classUnderTest = new OpenApiValidationFilter(true, true);

        // and:
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        final FilterChain filterChain = Mockito.mock(FilterChain.class);
        Mockito.when(servletRequest.getMethod()).thenReturn("OPTIONS");

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request shall be wrapped and added to the filter chain
        Mockito.verify(filterChain, times(1))
                .doFilter(any(ResettableRequestServletWrapper.class), any(ContentCachingResponseWrapper.class));
    }

    @Test
    public void doFilterInternal_noWrappingIfValidationIsDisabled() throws ServletException, IOException {
        // given:
        final OpenApiValidationFilter classUnderTest = new OpenApiValidationFilter(false, false);

        // and:
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        final FilterChain filterChain = Mockito.mock(FilterChain.class);

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request wasn't wrapped
        Mockito.verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void doFilterInternal_wrapsTheServletRequestIfContentLengthNotToLong() throws ServletException, IOException {
        // given:
        final OpenApiValidationFilter classUnderTest = new OpenApiValidationFilter();

        // and:
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        final FilterChain filterChain = Mockito.mock(FilterChain.class);
        Mockito.when(servletRequest.getHeader("content-length")).thenReturn(String.valueOf(Integer.MAX_VALUE));

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request shall be wrapped and added to the filter chain
        Mockito.verify(filterChain, times(1))
                .doFilter(any(ResettableRequestServletWrapper.class), same(servletResponse));
    }

    @Test
    public void doFilterInternal_wrapsTheServletRequestIfContentLengthIsInvalid() throws ServletException, IOException {
        // given:
        final OpenApiValidationFilter classUnderTest = new OpenApiValidationFilter();

        // and:
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        final FilterChain filterChain = Mockito.mock(FilterChain.class);
        Mockito.when(servletRequest.getHeader("content-length")).thenReturn("invalid-content-length");

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request shall be wrapped and added to the filter chain
        Mockito.verify(filterChain, times(1))
                .doFilter(any(ResettableRequestServletWrapper.class), same(servletResponse));
    }

    @Test
    public void doFilterInternal_noRequestWrappingIfContentIsToLong() throws ServletException, IOException {
        // given:
        final OpenApiValidationFilter classUnderTest = new OpenApiValidationFilter();

        // and:
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        final FilterChain filterChain = Mockito.mock(FilterChain.class);
        Mockito.when(servletRequest.getHeader("content-length")).thenReturn(String.valueOf(1L + Integer.MAX_VALUE));

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request wasn't wrapped
        Mockito.verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void doFilterInternal_noWrappingIfCorsPreflight() throws ServletException, IOException {
        // given:
        final OpenApiValidationFilter classUnderTest = new OpenApiValidationFilter(true, true);

        // and:
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
