package com.atlassian.oai.validator.springmvc;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A filter which wraps the {@link HttpServletRequest} into a {@link ResettableRequestServletWrapper}
 * which has the ability to reset its {@link javax.servlet.ServletInputStream}.
 * <p>
 * Wrapping is necessary for the validation.<br>
 * The Swagger Request Validator needs the pure request body for its validation. Additionally the Spring
 * {@link org.springframework.web.bind.annotation.RestController} / {@link org.springframework.stereotype.Controller}
 * needs the pure request body to unmarshal the JSON.
 * <p>
 * But a {@link javax.servlet.ServletInputStream} can only be read once and needs to be rewind after
 * successful validation against the Swagger definition. So the controller can then access it again.
 */
public class OpenApiValidationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(OpenApiValidationFilter.class);

    private final boolean validateRequests;
    private final boolean validateResponses;

    /**
     * Creates a {@link OpenApiValidationFilter} which validates incoming requests.
     */
    public OpenApiValidationFilter() {
        this(true, false);
    }

    /**
     * Creates a {@link OpenApiValidationFilter} which validates incoming requests and / or responses.
     *
     * @param validateRequests  will enable request validation if {@code true}
     * @param validateResponses will enable response validation if {@code true}
     */
    public OpenApiValidationFilter(final boolean validateRequests, final boolean validateResponses) {
        this.validateRequests = validateRequests;
        this.validateResponses = validateResponses;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse, final FilterChain filterChain)
            throws ServletException, IOException {
        final HttpServletRequest requestToUse = wrapValidatableServletRequest(servletRequest);
        final HttpServletResponse responseToUse = wrapValidatableServletResponse(servletRequest, servletResponse);
        filterChain.doFilter(requestToUse, responseToUse);

        // in case the response was cached it has to be written to the original response
        if (responseToUse instanceof ContentCachingResponseWrapper) {
            ((ContentCachingResponseWrapper) responseToUse).copyBodyToResponse();
        }
    }

    private HttpServletRequest wrapValidatableServletRequest(final HttpServletRequest servletRequest) {
        // wrap only validatable requests
        final boolean doValidationStep = validateRequests &&
                getContentLength(servletRequest) <= Integer.MAX_VALUE &&
                !CorsUtils.isPreFlightRequest(servletRequest);
        return doValidationStep ? new ResettableRequestServletWrapper(servletRequest) : servletRequest;
    }

    private HttpServletResponse wrapValidatableServletResponse(final HttpServletRequest servletRequest,
                                                               final HttpServletResponse servletResponse) {
        // wrap only validatable responses
        final boolean doValidationStep = validateResponses &&
                !CorsUtils.isPreFlightRequest(servletRequest);
        return doValidationStep ? new ContentCachingResponseWrapper(servletResponse) : servletResponse;
    }

    private static long getContentLength(final HttpServletRequest servletRequest) {
        final String contentLength = servletRequest.getHeader("content-length");
        if (StringUtils.isNotBlank(contentLength)) {
            try {
                return Long.parseLong(contentLength);
            } catch (final NumberFormatException e) {
                // either no valid content-length was set or the content-length exceeded Long.MAX_VALUE
                LOG.warn("Invalid content-length header value on request: '" + contentLength + "'");
            }
        }
        return -1L;
    }
}
