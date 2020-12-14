package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.UrlPathHelper;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.atlassian.oai.validator.springmvc.OpenApiValidationFilter.ATTRIBUTE_REQUEST_VALIDATION;
import static com.atlassian.oai.validator.springmvc.OpenApiValidationFilter.ATTRIBUTE_RESPONSE_VALIDATION;
import static java.util.Objects.requireNonNull;
import static javax.servlet.DispatcherType.ASYNC;

/**
 * An Interceptor which validates incoming requests against the defined OpenAPI / Swagger specification.
 *
 * <p>You can customize logging output of interceptor by implementing
 * interface {@link ValidationReportHandler} and providing instance
 * to constructor.</p>
 */
public class OpenApiValidationInterceptor extends HandlerInterceptorAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(OpenApiValidationInterceptor.class);

    protected final OpenApiValidationService openApiValidationService;
    private final ValidationReportHandler validationReportHandler;

    public OpenApiValidationInterceptor(@Nonnull final EncodedResource apiSpecification) throws IOException {
        this(new OpenApiValidationService(apiSpecification, new UrlPathHelper()));
    }

    public OpenApiValidationInterceptor(@Nonnull final OpenApiInteractionValidator validator) {
        this(new OpenApiValidationService(validator, new UrlPathHelper()));
    }

    public OpenApiValidationInterceptor(@Nonnull final OpenApiInteractionValidator validator,
                                        @Nonnull final ValidationReportHandler validationReportHandler) {
        this(new OpenApiValidationService(validator, new UrlPathHelper()), validationReportHandler);
    }

    public OpenApiValidationInterceptor(@Nonnull final OpenApiValidationService openApiValidationService) {
        this(openApiValidationService, new DefaultValidationReportHandler());
    }

    public OpenApiValidationInterceptor(
            @Nonnull final OpenApiValidationService openApiValidationService,
            @Nonnull final ValidationReportHandler validationReportHandler) {
        requireNonNull(openApiValidationService, "openApiValidationService must not be null");
        requireNonNull(validationReportHandler, "validationReportHandler must not be null");
        this.openApiValidationService = openApiValidationService;
        this.validationReportHandler = validationReportHandler;
    }

    private static boolean doValidationStep(final HttpServletRequest servletRequest, final String attributeName) {
        return Boolean.TRUE.equals(servletRequest.getAttribute(attributeName));
    }

    private static boolean doRequestValidationStep(final HttpServletRequest servletRequest) {
        return (servletRequest instanceof ContentCachingRequestWrapper || servletRequest instanceof ResettableRequestServletWrapper) &&
                doValidationStep(servletRequest, ATTRIBUTE_REQUEST_VALIDATION) && servletRequest.getDispatcherType() != ASYNC;
    }

    private static boolean doResponseValidationStep(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse) {
        return (servletResponse instanceof ContentCachingResponseWrapper) &&
                doValidationStep(servletRequest, ATTRIBUTE_RESPONSE_VALIDATION) && !servletRequest.isAsyncStarted();
    }

    /**
     * Validates the given requests. If a request is defined but invalid against the OpenAPI / Swagger specification
     * an {@link InvalidRequestException} will be thrown leading to an error response.
     * <p>
     * Only wrapped {@link HttpServletRequest} can be validated. Wrapping is done within the
     * {@link OpenApiValidationFilter}.
     *
     * @param servletRequest the {@link HttpServletRequest} to validate
     * @param servletResponse the servlet response
     * @param handler a handler
     *
     * @return {@code true} if the request is valid against or not defined in the API specification or
     * the servlet is not a {@link ResettableRequestServletWrapper}
     *
     * @throws Exception if the request is invalid against the API specification or the requests body
     * can't be read
     */
    @Override
    public boolean preHandle(final HttpServletRequest servletRequest,
                             final HttpServletResponse servletResponse,
                             final Object handler) throws Exception {
        if (!doRequestValidationStep(servletRequest)) {
            LOG.debug("OpenAPI request validation skipped");
            return true;
        }

        // validate the request
        final String requestLoggingKey = servletRequest.getMethod() + "#" + servletRequest.getRequestURI();
        LOG.debug("OpenAPI request validation: {}", requestLoggingKey);

        final Request request = openApiValidationService.buildRequest(servletRequest);
        final ValidationReport validationReport = openApiValidationService.validateRequest(request);

        validationReportHandler.handleRequestReport(requestLoggingKey, validationReport);

        // reset the requests servlet input stream after reading it on former step
        if (servletRequest instanceof ResettableRequestServletWrapper) {
            ((ResettableRequestServletWrapper) servletRequest).resetInputStream();
        }
        return true;
    }

    /**
     * Validates the given response. If a request is defined but its response is invalid against
     * the OpenAPI / Swagger specification an {@link InvalidResponseException} will be thrown leading
     * to an error response.
     * <p>
     * Only wrapped {@link HttpServletResponse} can be validated. Wrapping is done within the
     * {@link OpenApiValidationFilter}.
     *
     * @param servletRequest the servlet request
     * @param servletResponse the {@link HttpServletResponse} to validate
     * @param handler a handler
     * @param modelAndView a model and view
     */
    @Override
    public void postHandle(final HttpServletRequest servletRequest,
                           final HttpServletResponse servletResponse,
                           final Object handler,
                           final ModelAndView modelAndView) {
        if (!doResponseValidationStep(servletRequest, servletResponse)) {
            LOG.debug("OpenAPI response validation skipped");
            return;
        }

        // validate the response
        final ContentCachingResponseWrapper cachedResponse = (ContentCachingResponseWrapper) servletResponse;
        final String requestLoggingKey = servletRequest.getMethod() + "#" + servletRequest.getRequestURI();
        LOG.debug("OpenAPI response validation: {}", requestLoggingKey);

        final Response response = openApiValidationService.buildResponse(cachedResponse);
        final ValidationReport validationReport = openApiValidationService.validateResponse(servletRequest, response);

        try {
            validationReportHandler.handleResponseReport(requestLoggingKey, validationReport);
        } catch (final InvalidResponseException e) {
            // as an exception will rewrite the current, cached response it has to be reset
            cachedResponse.reset();
            throw e;
        }
    }
}
