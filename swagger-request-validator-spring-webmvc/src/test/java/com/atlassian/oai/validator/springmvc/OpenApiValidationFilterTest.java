package com.atlassian.oai.validator.springmvc;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenApiValidationFilterTest {

    @Test
    public void doFilterInternal_wrapsTheServletRequestAndResponseIfNoCors() throws ServletException, IOException {
        // given:
        final OpenApiValidationFilter classUnderTest = new OpenApiValidationFilter(true, true);

        // and:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);
        when(servletRequest.getMethod()).thenReturn("OPTIONS");

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request shall be wrapped and added to the filter chain
        verify(filterChain).doFilter(any(ResettableRequestServletWrapper.class), any(ContentCachingResponseWrapper.class));
        verify(servletRequest).setAttribute("com.atlassian.oai.validator.springmvc.requestValidation", true);
        verify(servletRequest).setAttribute("com.atlassian.oai.validator.springmvc.responseValidation", true);
    }

    @Test
    public void doFilterInternal_noWrappingIfValidationIsDisabled() throws ServletException, IOException {
        // given:
        final OpenApiValidationFilter classUnderTest = new OpenApiValidationFilter(false, false);

        // and:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request wasn't wrapped
        verify(filterChain).doFilter(servletRequest, servletResponse);
        verify(servletRequest).setAttribute("com.atlassian.oai.validator.springmvc.requestValidation", false);
        verify(servletRequest).setAttribute("com.atlassian.oai.validator.springmvc.responseValidation", false);
    }

    @Test
    public void doFilterInternal_wrapsIntoContentCachingRequestWrapperIfFormData() throws ServletException, IOException {
        // given:
        final OpenApiValidationFilter classUnderTest = new OpenApiValidationFilter();

        // and:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);
        when(servletRequest.getContentType()).thenReturn("application/x-www-form-urlencoded");

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request shall be wrapped and added to the filter chain
        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), same(servletResponse));
        verify(servletRequest).setAttribute("com.atlassian.oai.validator.springmvc.requestValidation", true);
        verify(servletRequest).setAttribute("com.atlassian.oai.validator.springmvc.responseValidation", false);
    }

    @Test
    public void doFilterInternal_noWrappingIfAlreadyWrapped() throws ServletException, IOException {
        // given:
        final OpenApiValidationFilter classUnderTest = new OpenApiValidationFilter(true, true);

        // and:
        final ContentCachingRequestWrapper servletRequest = mock(ContentCachingRequestWrapper.class);
        final ContentCachingResponseWrapper servletResponse = mock(OpenApiValidationContentCachingResponseWrapper.class);
        final FilterChain filterChain = mock(FilterChain.class);
        when(servletRequest.getContentType()).thenReturn("application/x-www-form-urlencoded");

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request wasn't wrapped
        verify(filterChain).doFilter(same(servletRequest), same(servletResponse));
        verify(servletRequest).setAttribute("com.atlassian.oai.validator.springmvc.requestValidation", true);
        verify(servletRequest).setAttribute("com.atlassian.oai.validator.springmvc.responseValidation", true);
    }

    @Test
    public void doFilterInternal_noWrappingIfCorsPreflight() throws ServletException, IOException {
        // given:
        final OpenApiValidationFilter classUnderTest = new OpenApiValidationFilter(true, true);

        // and:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);
        when(servletRequest.getHeader("Origin")).thenReturn("https://bitbucket.org");
        when(servletRequest.getHeader("Access-Control-Request-Method")).thenReturn("POST");
        when(servletRequest.getMethod()).thenReturn("OPTIONS");

        // when:
        classUnderTest.doFilterInternal(servletRequest, servletResponse, filterChain);

        // then: the request wasn't wrapped
        verify(filterChain).doFilter(servletRequest, servletResponse);
        verify(servletRequest).setAttribute("com.atlassian.oai.validator.springmvc.requestValidation", false);
        verify(servletRequest).setAttribute("com.atlassian.oai.validator.springmvc.responseValidation", false);
    }
}
