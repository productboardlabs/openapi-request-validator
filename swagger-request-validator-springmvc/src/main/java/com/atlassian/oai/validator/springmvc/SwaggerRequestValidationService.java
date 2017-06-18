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

    /**
     * @param servletRequest the {@link HttpServletRequest}
     * @return the build {@link Request} created out of given {@link HttpServletRequest}
     * @throws IOException if the request body can't be read
     */
    Request buildRequest(final HttpServletRequest servletRequest) throws IOException {
        final Request.Method method = Request.Method.valueOf(servletRequest.getMethod());
        final String requestUri = getCompleteRequestUri(servletRequest);
        final UriComponents uriComponents = UriComponentsBuilder
                .fromUriString(requestUri)
                .build();
        final String path = uriComponents.getPath();
        final SimpleRequest.Builder builder = new SimpleRequest.Builder(method, path);
        final String body = readReader(servletRequest.getReader());
        if (servletRequest.getContentLength() < 0 && (body == null || body.isEmpty())) {
            // The content length suggests that there is no body - but this is only
            // a hint that there might be no body. But if the body itself is empty
            // (or null) it was truly not set.
        } else {
            // yes, there was a body - even though it might be empty
            builder.withBody(body);
        }
        for (final Map.Entry<String, List<String>> entry : uriComponents.getQueryParams().entrySet()) {
            builder.withQueryParam(entry.getKey(), entry.getValue());
        }
        for (final String headerName : Collections.list(servletRequest.getHeaderNames())) {
            builder.withHeader(headerName, Collections.list(servletRequest.getHeaders(headerName)));
        }
        return builder.build();
    }

    /**
     * @param request the {@link Request} to validate against the Swagger schema
     * @return the {@link ValidationReport} for the validated {@link Request}
     */
    ValidationReport validateRequest(final Request request) {
        return requestValidator.validateRequest(request);
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
