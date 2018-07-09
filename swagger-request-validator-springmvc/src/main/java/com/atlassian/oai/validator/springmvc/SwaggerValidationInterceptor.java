package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * An Interceptor which validates incoming requests against the defined swagger interface.
 */
public class SwaggerValidationInterceptor extends HandlerInterceptorAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(SwaggerValidationInterceptor.class);

    private final SwaggerRequestValidationService swaggerRequestValidationService;

    public SwaggerValidationInterceptor(final EncodedResource swaggerInterface) throws IOException {
        this(new SwaggerRequestValidationService(swaggerInterface));
    }

    SwaggerValidationInterceptor(final SwaggerRequestValidationService swaggerRequestValidationService) {
        this.swaggerRequestValidationService = swaggerRequestValidationService;
    }

    /**
     * Validates the given requests. If a request is defined but invalid against the Swagger schema
     * an {@link InvalidRequestException} will be thrown leading to an error response.
     * <p>
     * Only {@link ResettableRequestServletWrapper} can be validated. Wrapping is done within the
     * {@link SwaggerValidationFilter}.
     *
     * @param servletRequest  the {@link HttpServletRequest} to validate
     * @param servletResponse the servlet response
     * @param handler         a handler
     * @return {@code true} if the request is valid against or not defined in the Swagger schema or
     * the servlet is not a {@link ResettableRequestServletWrapper}
     * @throws Exception if the request is invalid against the Swagger schema or the requests body
     *                   can't be read
     */
    @Override
    public boolean preHandle(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse, final Object handler) throws Exception {
        // only wrapped servlet requests can be validated - see: SwaggerValidationFilter
        if (!(servletRequest instanceof ResettableRequestServletWrapper)) {
            return true;
        }

        // validate the request
        final ResettableRequestServletWrapper resettableRequest = (ResettableRequestServletWrapper) servletRequest;
        final String requestLoggingKey = servletRequest.getMethod() + "#" + servletRequest.getRequestURI();
        LOG.debug("Swagger request validation: {}", requestLoggingKey);

        final Request request = swaggerRequestValidationService.buildRequest(resettableRequest);
        final ValidationReport validationReport = swaggerRequestValidationService.validateRequest(request);
        if (!validationReport.hasErrors()) {
            LOG.debug("Swagger request validation: {} - The request is valid.", requestLoggingKey);
        } else if (!swaggerRequestValidationService.isDefinedSwaggerRequest(validationReport)) {
            LOG.info("Swagger request validation: {} - The request is not defined in the Swagger schema. Ignoring it.",
                    requestLoggingKey);
        } else {
            final InvalidRequestException invalidRequestException = new InvalidRequestException(validationReport);
            LOG.info("Swagger request validation: {} - The request is invalid: {}", requestLoggingKey,
                    invalidRequestException.getMessage());
            throw invalidRequestException;
        }

        // reset the requests servlet input stream after reading it on former step
        resettableRequest.resetInputStream();
        return true;
    }

    /**
     * Validates the given response. If a request is defined but its response is invalid against
     * the Swagger schema an {@link InvalidResponseException} will be thrown leading to an error
     * response.
     * <p>
     * Only {@link ContentCachingResponseWrapper} can be validated. Wrapping is done within the
     * {@link SwaggerValidationFilter}.
     *
     * @param servletRequest  the servlet request
     * @param servletResponse the {@link HttpServletResponse} to validate
     * @param handler         a handler
     * @param modelAndView    a model and view
     * @throws Exception if the response is invalid against the Swagger schema or the response body
     *                   can't be read
     */
    @Override
    public void postHandle(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse,
                           final Object handler, final ModelAndView modelAndView) throws Exception {
        // only cached servlet responses can be validated - see: SwaggerValidationFilter
        if (!(servletResponse instanceof ContentCachingResponseWrapper)) {
            return;
        }

        // validate the response
        final ContentCachingResponseWrapper cachedResponse = (ContentCachingResponseWrapper) servletResponse;
        final String requestLoggingKey = servletRequest.getMethod() + "#" + servletRequest.getRequestURI();
        LOG.debug("Swagger response validation: {}", requestLoggingKey);

        final Response response = swaggerRequestValidationService.buildResponse(cachedResponse);
        final ValidationReport validationReport = swaggerRequestValidationService.validateResponse(servletRequest, response);
        if (!validationReport.hasErrors()) {
            LOG.debug("Swagger response validation: {} - The response is valid.", requestLoggingKey);
        } else if (!swaggerRequestValidationService.isDefinedSwaggerRequest(validationReport)) {
            LOG.info("Swagger response validation: {} - The request is not defined in the Swagger schema. Ignoring it.",
                    requestLoggingKey);
        } else {
            final InvalidResponseException invalidResponseException = new InvalidResponseException(validationReport);
            LOG.info("Swagger response validation: {} - The response is invalid: {}", requestLoggingKey,
                    invalidResponseException.getMessage());
            // as an exception will rewrite the current, cached response it has to be reset
            cachedResponse.reset();
            throw invalidResponseException;
        }
    }
}
