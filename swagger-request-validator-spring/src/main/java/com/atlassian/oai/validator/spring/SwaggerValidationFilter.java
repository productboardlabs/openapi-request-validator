package com.atlassian.oai.validator.spring;

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
 * successful validation against the Swagger definition. The controller can then access it again.
 * <p>
 * Asynchronous requests are not supported.
 */
public class SwaggerValidationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse, final FilterChain filterChain)
            throws ServletException, IOException {
        final HttpServletRequest requestToUse = wrapValidatableServletRequest(servletRequest);
        filterChain.doFilter(requestToUse, servletResponse);
    }

    private HttpServletRequest wrapValidatableServletRequest(final HttpServletRequest servletRequest) {
        // wrap only validatable requests
        final boolean doValidationStep = servletRequest.getContentLengthLong() <= Integer.MAX_VALUE &&
                !isAsyncDispatch(servletRequest);
        return doValidationStep ? new ResettableRequestServletWrapper(servletRequest) : servletRequest;
    }
}
