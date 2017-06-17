package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

class SwaggerRequestValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(SwaggerRequestValidationService.class);

    private static final String MESSAGE_REQUEST_PATH_MISSING = "validation.request.path.missing";

    private final SwaggerRequestResponseValidator requestValidator;

    SwaggerRequestValidationService(final EncodedResource restInterface) throws IOException {
        this(SwaggerRequestResponseValidator
                .createFor(readReader(restInterface.getReader()))
                .build());
    }

    SwaggerRequestValidationService(final SwaggerRequestResponseValidator requestValidator) {
        this.requestValidator = requestValidator;
    }

    private static String readReader(final Reader reader) {
        final Scanner s = new Scanner(reader).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static String getCompleteRequestUri(final HttpServletRequest servletRequest) {
        if (StringUtils.isBlank(servletRequest.getQueryString())) {
            return servletRequest.getRequestURI();
        }
        return servletRequest.getRequestURI() + "?" + servletRequest.getQueryString();
    }

    private static Request buildRequest(final HttpServletRequest servletRequest) throws IOException {
        final Request.Method method = Request.Method.valueOf(servletRequest.getMethod());
        final String requestUrl = getCompleteRequestUri(servletRequest);
        final UriComponents uriComponents = UriComponentsBuilder
                .fromUriString(requestUrl)
                .build();
        final String path = uriComponents.getPath();
        final String body = readReader(servletRequest.getReader());
        final SimpleRequest.Builder builder = new SimpleRequest.Builder(method, path)
                .withBody(body);
        for (final Map.Entry<String, List<String>> entry : uriComponents.getQueryParams().entrySet()) {
            builder.withQueryParam(entry.getKey(), entry.getValue());
        }
        for (final String headerName : Collections.list(servletRequest.getHeaderNames())) {
            builder.withHeader(headerName, Collections.list(servletRequest.getHeaders(headerName)));
        }
        return builder.build();
    }

    /**
     * @param servletRequest the {@link HttpServletRequest} to validate against the Swagger schema
     * @return the {@link ValidationReport} for the validated {@link HttpServletRequest}
     */
    ValidationReport validateRequest(final HttpServletRequest servletRequest) throws IOException {
        try {
            final Request request = buildRequest(servletRequest);
            return requestValidator.validateRequest(request);
        } catch (final RuntimeException e) {
            // if you encounter this error please create an issue on: https://bitbucket.org/atlassian/swagger-request-validator
            LOG.error("Unexpected error during request validation.", e);
            throw e;
        }
    }

    /**
     * @param validationReport the {@link ValidationReport}
     * @return {@code true} if the validated request is defined in the Swagger schema, otherwise {@code false}
     */
    boolean isDefinedSwaggerRequest(final ValidationReport validationReport) {
        for (final ValidationReport.Message message : validationReport.getMessages()) {
            if (MESSAGE_REQUEST_PATH_MISSING.equals(message.getKey())) {
                return false;
            }
        }
        return true;
    }
}
