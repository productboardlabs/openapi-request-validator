package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.util.ContentTypeUtils;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.atlassian.oai.validator.springmvc.ResponseUtils.getCachingResponse;
import static jakarta.servlet.DispatcherType.ASYNC;
import static org.apache.commons.lang3.ClassUtils.getPackageName;

/**
 * A filter for wrapping the {@link HttpServletRequest} and {@link HttpServletResponse}.
 * <p>
 * Wrapping is necessary for the validation.<br>
 * The Swagger Request Validator needs the pure request body for its validation. Additionally the Spring
 * {@link org.springframework.web.bind.annotation.RestController} / {@link org.springframework.stereotype.Controller}
 * needs the pure request body to unmarshal the JSON / form data.
 * <p>
 * But a {@link jakarta.servlet.ServletInputStream} can only be read once and needs to be rewind after
 * successful validation against the OpenAPI / Swagger definition. So the controller can then access it again.
 * <p>
 * The same applies to response validation and writing to the {@link jakarta.servlet.ServletOutputStream}.
 */
public class OpenApiValidationFilter extends OncePerRequestFilter {
    static final String ATTRIBUTE_REQUEST_VALIDATION = getPackageName(OpenApiValidationFilter.class) + ".requestValidation";
    static final String ATTRIBUTE_RESPONSE_VALIDATION = getPackageName(OpenApiValidationFilter.class) + ".responseValidation";

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
        if (!isAsyncStarted(requestToUse)) {
            final OpenApiValidationContentCachingResponseWrapper cachingResponse = getCachingResponse(responseToUse);
            if (cachingResponse != null) {
                cachingResponse.copyBodyToResponse();
            }
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    private HttpServletRequest wrapValidatableServletRequest(final HttpServletRequest servletRequest) {
        // set validation information used by the interceptor
        final boolean doValidationStep = validateRequests &&
                !CorsUtils.isPreFlightRequest(servletRequest) &&
                servletRequest.getDispatcherType() != ASYNC;

        servletRequest.setAttribute(ATTRIBUTE_REQUEST_VALIDATION, doValidationStep);

        // do not wrap requests that aren't validated
        if (!doValidationStep) {
            return servletRequest;
        }

        // Wrap form requests into a ContentCachingRequestWrapper. The servlet container parses the body into
        // the parameter map. Springs ContentCachingRequestWrapper is able to recreate the already read body.
        // This body will then be used by the swagger-request-validator for validation.
        if (ContentTypeUtils.isFormDataContentType(servletRequest.getContentType())) {
            // do not re-wrap already wrapped requests
            return (servletRequest instanceof ContentCachingRequestWrapper) ? servletRequest
                    : new ContentCachingRequestWrapper(servletRequest);
        }
        return new ResettableRequestServletWrapper(servletRequest);
    }

    private HttpServletResponse wrapValidatableServletResponse(final HttpServletRequest servletRequest,
                                                               final HttpServletResponse servletResponse) {
        // set validation information used by the interceptor
        final boolean doValidationStep = validateResponses &&
                !CorsUtils.isPreFlightRequest(servletRequest);
        servletRequest.setAttribute(ATTRIBUTE_RESPONSE_VALIDATION, doValidationStep);

        // do not wrap responses that aren't validated
        if (!doValidationStep) {
            return servletResponse;
        }

        // do not re-wrap already wrapped responses
        return getCachingResponse(servletResponse) != null ? servletResponse
                : new OpenApiValidationContentCachingResponseWrapper(servletResponse);
    }

}
