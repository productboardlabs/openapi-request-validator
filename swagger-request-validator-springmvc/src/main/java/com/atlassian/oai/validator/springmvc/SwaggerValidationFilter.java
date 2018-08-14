package com.atlassian.oai.validator.springmvc;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @deprecated Replaced with {@link OpenApiValidationFilter}
 */
@Deprecated
public class SwaggerValidationFilter extends OncePerRequestFilter {

    private final OpenApiValidationFilter delegate;

    public SwaggerValidationFilter() {
        this(true, false);
    }

    public SwaggerValidationFilter(final boolean validateRequests,
                                   final boolean validateResponses) {
        delegate = new OpenApiValidationFilter(validateRequests, validateResponses);
    }

    @Override
    public void doFilterInternal(final HttpServletRequest servletRequest,
                                 final HttpServletResponse servletResponse,
                                 final FilterChain filterChain) throws ServletException, IOException {
        delegate.doFilterInternal(servletRequest, servletResponse, filterChain);
    }
}
