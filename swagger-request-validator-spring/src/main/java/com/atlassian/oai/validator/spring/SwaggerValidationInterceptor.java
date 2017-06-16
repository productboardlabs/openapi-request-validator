package com.atlassian.oai.validator.spring;

import com.atlassian.oai.validator.report.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

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
        final String requestUri = servletRequest.getRequestURI();
        LOG.debug("Swagger validation: {}", requestUri);

        final ValidationReport validationReport = swaggerRequestValidationService
                .validateRequest(resettableRequest);
        if (!validationReport.hasErrors()) {
            LOG.debug("Swagger validation: {} - The request is valid.", requestUri);
        } else if (!swaggerRequestValidationService.isDefinedSwaggerRequest(validationReport)) {
            LOG.info("Swagger validation: {} - The request is not defined in the Swagger schema. Ignoring it.",
                    requestUri);
        } else {
            final InvalidRequestException invalidRequestException = new InvalidRequestException(validationReport);
            LOG.info("Swagger validation: {} - The REST request is invalid: {}", requestUri,
                    invalidRequestException.getMessage());
            throw invalidRequestException;
        }

        // reset the requests servlet input stream after reading it on former step
        resettableRequest.resetInputStream();
        return true;
    }
}
