package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Body;
import com.atlassian.oai.validator.model.ByteArrayBody;
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
import java.util.List;
import java.util.Map;
import java.io.InputStream;
import java.util.function.Supplier;

import static com.atlassian.oai.validator.springmvc.OpenApiValidationFilter.ATTRIBUTE_REQUEST_VALIDATION;
import static com.atlassian.oai.validator.springmvc.OpenApiValidationFilter.ATTRIBUTE_RESPONSE_VALIDATION;
import static com.atlassian.oai.validator.springmvc.ResponseUtils.getCachingResponse;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ClassUtils.getPackageName;

/**
 * An Interceptor which validates incoming requests against the defined OpenAPI / Swagger specification.
 *
 * <p>You can customize logging output of interceptor by implementing
 * interface {@link ValidationReportHandler} and providing instance
 * to constructor.</p>
 */
public class OpenApiValidationInterceptor extends HandlerInterceptorAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(OpenApiValidationInterceptor.class);
    private static final String ATTRIBUTE_ALREADY_SET_HEADERS = getPackageName(OpenApiValidationInterceptor.class) + ".alreadySetHeaders";

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

    private static boolean skipValidationStep(final HttpServletRequest servletRequest, final String attributeName) {
        return !Boolean.TRUE.equals(servletRequest.getAttribute(attributeName));
    }

    private static String buildRequestLoggingKey(final HttpServletRequest servletRequest) {
        return servletRequest.getMethod() + "#" + servletRequest.getRequestURI();
    }

    private void validateRequest(final HttpServletRequest servletRequest, final Supplier<Body> bodySupplier) {
        final String requestLoggingKey = buildRequestLoggingKey(servletRequest);
        LOG.debug("OpenAPI request validation: {}", requestLoggingKey);

        final Request request = openApiValidationService.buildRequest(servletRequest, bodySupplier);
        final ValidationReport validationReport = openApiValidationService.validateRequest(request);

        validationReportHandler.handleRequestReport(requestLoggingKey, validationReport);
    }

    private void validateResponse(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse,
                                  final ContentCachingResponseWrapper cachedResponse) {
        final String requestLoggingKey = buildRequestLoggingKey(servletRequest);
        LOG.debug("OpenAPI response validation: {}", requestLoggingKey);

        final Response response = openApiValidationService.buildResponse(cachedResponse);
        final ValidationReport validationReport = openApiValidationService.validateResponse(servletRequest, response);

        try {
            validationReportHandler.handleResponseReport(requestLoggingKey, validationReport);
        } catch (final InvalidResponseException e) {
            // as an exception will rewrite the current, cached response it has to be reset
            cachedResponse.reset();

            // Add all headers back to the response, which were already present at the response before the request has
            // been processed.
            // These headers are considered as meta-data that were not added as part of the actual request-process,
            // e.g. CORS or security headers.
            final Map<String, List<String>> alreadySetHeaders = (Map<String, List<String>>) servletRequest.getAttribute(ATTRIBUTE_ALREADY_SET_HEADERS);
            openApiValidationService.addHeadersToResponse(servletResponse, alreadySetHeaders);

            throw e;
        }
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
        if (!skipValidationStep(servletRequest, ATTRIBUTE_RESPONSE_VALIDATION)) {
            // save already set headers for the upcoming response validation
            final Map<String, List<String>> alreadySetHeaders = openApiValidationService.resolveHeadersOnResponse(servletResponse);
            servletRequest.setAttribute(ATTRIBUTE_ALREADY_SET_HEADERS, alreadySetHeaders);
        }

        if (skipValidationStep(servletRequest, ATTRIBUTE_REQUEST_VALIDATION)) {
            LOG.debug("OpenAPI request validation skipped for this request");
        } else {
            if (servletRequest instanceof ResettableRequestServletWrapper) {
                final InputStream inputStream = servletRequest.getInputStream();
                final Supplier<Body> bodySupplier = () -> new ResettableInputStreamBody((ResettableRequestServletWrapper.CachingServletInputStream) inputStream);
                validateRequest(servletRequest, bodySupplier);
                // reset the request's servlet input stream after reading it on validation
                ((ResettableRequestServletWrapper) servletRequest).resetInputStream();
            } else if (servletRequest instanceof ContentCachingRequestWrapper) {
                final Supplier<Body> bodySupplier = () -> new ByteArrayBody(((ContentCachingRequestWrapper) servletRequest).getContentAsByteArray());
                validateRequest(servletRequest, bodySupplier);
            } else {
                LOG.debug("OpenAPI request validation skipped: unsupported HttpServletRequest type");
            }
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
        if (skipValidationStep(servletRequest, ATTRIBUTE_RESPONSE_VALIDATION)) {
            LOG.debug("OpenAPI response validation skipped for this request");
        } else {
            final ContentCachingResponseWrapper cachedResponse = getCachingResponse(servletResponse);
            if (cachedResponse != null) {
                validateResponse(servletRequest, servletResponse, cachedResponse);
            } else {
                LOG.debug("OpenAPI response validation skipped: unsupported HttpServletResponse type");
            }
        }
    }
}
