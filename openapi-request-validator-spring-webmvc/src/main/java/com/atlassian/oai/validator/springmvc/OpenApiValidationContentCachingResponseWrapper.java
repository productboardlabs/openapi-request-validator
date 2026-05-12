package com.atlassian.oai.validator.springmvc;

import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Wrapper which makes sure that we do not flush ContentCachingResponseWrapper from another filter.
 * With async processing (and not only that) our ContentCachingResponseWrapper does not have
 * to be on the top of the ResponseWrapper chain.
 */
public class OpenApiValidationContentCachingResponseWrapper extends ContentCachingResponseWrapper {
    public OpenApiValidationContentCachingResponseWrapper(final HttpServletResponse response) {
        super(response);
    }
}
