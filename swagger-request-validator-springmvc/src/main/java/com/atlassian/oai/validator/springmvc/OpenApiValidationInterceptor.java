package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import static com.atlassian.oai.validator.report.ValidationReport.MessageContext.Location.REQUEST;
import static com.atlassian.oai.validator.report.ValidationReport.MessageContext.Location.RESPONSE;
import static java.util.stream.Collectors.joining;

/**
 * An Interceptor which validates incoming requests against the defined OpenAPI / Swagger specification.
 */
public class OpenApiValidationInterceptor extends HandlerInterceptorAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(OpenApiValidationInterceptor.class);
    private static final String DELIMITER = ",";

    protected final OpenApiValidationService openApiValidationService;

    public OpenApiValidationInterceptor(final EncodedResource apiSpecification) throws IOException {
        this(new OpenApiValidationService(apiSpecification));
    }

    public OpenApiValidationInterceptor(final OpenApiInteractionValidator validator) {
        this(new OpenApiValidationService(validator));
    }

    OpenApiValidationInterceptor(final OpenApiValidationService openApiValidationService) {
        this.openApiValidationService = openApiValidationService;
    }

    /**
     * Validates the given requests. If a request is defined but invalid against the OpenAPI / Swagger specification
     * an {@link InvalidRequestException} will be thrown leading to an error response.
     * <p>
     * Only {@link ResettableRequestServletWrapper} can be validated. Wrapping is done within the
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
        // only wrapped servlet requests can be validated - see: OpenApiValidationFilter
        if (!(servletRequest instanceof ResettableRequestServletWrapper)) {
            LOG.debug("OpenAPI request validation disabled");
            return true;
        }

        // validate the request
        final ResettableRequestServletWrapper resettableRequest = (ResettableRequestServletWrapper) servletRequest;
        final String requestLoggingKey = servletRequest.getMethod() + "#" + servletRequest.getRequestURI();
        LOG.debug("OpenAPI validation: {}", requestLoggingKey);

        final Request request = openApiValidationService.buildRequest(resettableRequest);
        final ValidationReport validationReport = openApiValidationService.validateRequest(request);

        processApiValidationReport(REQUEST, validationReport, requestLoggingKey);

        // reset the requests servlet input stream after reading it on former step
        resettableRequest.resetInputStream();
        return true;
    }

    /**
     * Validates the given response. If a request is defined but its response is invalid against
     * the OpenAPI / Swagger specification an {@link InvalidResponseException} will be thrown leading
     * to an error response.
     * <p>
     * Only {@link ContentCachingResponseWrapper} can be validated. Wrapping is done within the
     * {@link OpenApiValidationFilter}.
     *
     * @param servletRequest the servlet request
     * @param servletResponse the {@link HttpServletResponse} to validate
     * @param handler a handler
     * @param modelAndView a model and view
     *
     * @throws Exception if the response is invalid against the API specification or the response body can't be read
     */
    @Override
    public void postHandle(final HttpServletRequest servletRequest,
                           final HttpServletResponse servletResponse,
                           final Object handler,
                           final ModelAndView modelAndView) throws Exception {
        // only cached servlet responses can be validated - see: OpenApiValidationFilter
        if (!(servletResponse instanceof ContentCachingResponseWrapper)) {
            LOG.debug("OpenAPI response validation disabled");
            return;
        }

        // validate the response
        final ContentCachingResponseWrapper cachedResponse = (ContentCachingResponseWrapper) servletResponse;
        final String requestLoggingKey = servletRequest.getMethod() + "#" + servletRequest.getRequestURI();
        LOG.debug("OpenAPI response validation: {}", requestLoggingKey);

        final Response response = openApiValidationService.buildResponse(cachedResponse);
        final ValidationReport validationReport = openApiValidationService.validateResponse(servletRequest, response);

        try {
            processApiValidationReport(RESPONSE, validationReport, requestLoggingKey);
        } catch (final InvalidResponseException e) {
            // as an exception will rewrite the current, cached response it has to be reset
            cachedResponse.reset();
            throw e;
        }
    }

    /**
     * Method which gives you simple way how to override logging of validation issues.
     *
     * @param location request or response
     * @param validationReport result of validation
     * @param loggingKey method and request path unique string
     */
    protected void processApiValidationReport(final ValidationReport.MessageContext.Location location,
                                              final ValidationReport validationReport,
                                              final String loggingKey) {
        final Set<Level> validationLevels = validationReport.sortedValidationLevels();

        if (validationLevels.contains(Level.ERROR)) {
            final RuntimeException validationException = createValidationException(validationReport, location);
            logApiValidation(LOG::error, validationLevels, location, loggingKey, validationException.getMessage());
            throw validationException;
        } else if (validationLevels.contains(Level.INFO) || validationLevels.contains(Level.WARN) || validationLevels.contains(Level.IGNORE)) {
            final String messages = validationReport
                    .getMessages()
                    .stream()
                    .map(Objects::toString)
                    .collect(joining(DELIMITER));
            logApiValidation(LOG::info, validationLevels, location, loggingKey, messages);
        } else {
            LOG.debug("OpenAPI validation: {} - The {} is valid.", loggingKey, location);
        }
    }

    private void logApiValidation(final BiConsumer<String, String[]> logConsumer,
                                  final Set<Level> validationLevels,
                                  final ValidationReport.MessageContext.Location location,
                                  final String loggingKey,
                                  final String message) {
        final String logTemplate = "OpenAPI {} levels={} key={} message={}";
        final String joinedLevels = validationLevels
                .stream()
                .map(Objects::toString)
                .collect(joining(DELIMITER));

        logConsumer.accept(logTemplate, new String[] {
                location.toString(), joinedLevels, loggingKey, message
        });
    }

    public RuntimeException createValidationException(
            final ValidationReport validationReport,
            final ValidationReport.MessageContext.Location location
    ) {
        if (location == REQUEST) {
            return new InvalidRequestException(validationReport);
        } else {
            return new InvalidResponseException(validationReport);
        }
    }
}
