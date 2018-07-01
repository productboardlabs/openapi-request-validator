package com.atlassian.oai.validator.springmvc;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

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
public class SwaggerValidationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SwaggerValidationFilter.class);

    @Override
    protected void doFilterInternal(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse, final FilterChain filterChain)
            throws ServletException, IOException {
        final HttpServletRequest requestToUse = wrapValidatableServletRequest(servletRequest);
        filterChain.doFilter(requestToUse, servletResponse);
    }

    private HttpServletRequest wrapValidatableServletRequest(final HttpServletRequest servletRequest) {
        // wrap only validatable requests
        final long contentLengthLong = getContentLength(servletRequest);
        final boolean doValidationStep = contentLengthLong <= Integer.MAX_VALUE &&
                !CorsUtils.isPreFlightRequest(servletRequest);
        return doValidationStep ? new ResettableRequestServletWrapper(servletRequest) : servletRequest;
    }

    private long getContentLength(final HttpServletRequest servletRequest) {
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
