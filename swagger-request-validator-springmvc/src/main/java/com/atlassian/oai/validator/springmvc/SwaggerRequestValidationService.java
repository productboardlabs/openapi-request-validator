package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class SwaggerRequestValidationService {

    private final SwaggerRequestResponseValidator swaggerRequestResponseValidator;

    SwaggerRequestValidationService(final EncodedResource restInterface) throws IOException {
        this(SwaggerRequestResponseValidator
                .createFor(readReader(restInterface.getReader()))
                .withLevelResolver(SpringMVCLevelResolverFactory.create())
                .build());
    }

    SwaggerRequestValidationService(final SwaggerRequestResponseValidator swaggerRequestResponseValidator) {
        Objects.requireNonNull(swaggerRequestResponseValidator, "A Swagger request response validator is required.");
        this.swaggerRequestResponseValidator = swaggerRequestResponseValidator;
    }

    /**
     * @param servletRequest the {@link HttpServletRequest}
     * @return the build {@link Request} created out of given {@link HttpServletRequest}
     * @throws IOException if the request body can't be read
     */
    Request buildRequest(final HttpServletRequest servletRequest) throws IOException {
        Objects.requireNonNull(servletRequest, "A request is required.");
        final Request.Method method = Request.Method.valueOf(servletRequest.getMethod());
        final String path = servletRequest.getServletPath();
        final SimpleRequest.Builder builder = new SimpleRequest.Builder(method, path);
        final String body = readReader(servletRequest.getReader());
        // The content length of a request does not need to be set. It might by "-1" and
        // there is still a body. Only in conjunction with an empty / unset body it was
        // really empty.
        if (servletRequest.getContentLength() >= 0 || (body != null && !body.isEmpty())) {
            builder.withBody(body);
        }
        for (final String queryParameterName : getQueryParameterNames(servletRequest)) {
            builder.withQueryParam(queryParameterName, servletRequest.getParameterValues(queryParameterName));
        }
        for (final String headerName : Collections.list(servletRequest.getHeaderNames())) {
            builder.withHeader(headerName, Collections.list(servletRequest.getHeaders(headerName)));
        }
        return builder.build();
    }

    /**
     * @param servletResponse the {@link javax.servlet.http.HttpServletResponse} whose body is cached
     * @return the build {@link Response} created out of given {@link ContentCachingResponseWrapper}
     * @throws IOException if the cached response body can't be read
     */
    Response buildResponse(final ContentCachingResponseWrapper servletResponse) throws IOException {
        final int statusCode = servletResponse.getStatusCode();
        final SimpleResponse.Builder builder = new SimpleResponse.Builder(statusCode)
                .withBody(new String(servletResponse.getContentAsByteArray(), servletResponse.getCharacterEncoding()));
        for (final String headerName : servletResponse.getHeaderNames()) {
            builder.withHeader(headerName, Lists.newArrayList(servletResponse.getHeaders(headerName)));
        }
        return builder.build();
    }

    /**
     * @param request the {@link Request} to validate against the Swagger schema
     * @return the {@link ValidationReport} for the validated {@link Request}
     */
    ValidationReport validateRequest(final Request request) {
        return swaggerRequestResponseValidator.validateRequest(request);
    }

    /**
     * @param servletRequest the {@link HttpServletRequest} to examine the api path to validate against
     * @param response       the {@link Response} to validate against the Swagger schema
     * @return the {@link ValidationReport} for the validated {@link Request}
     */
    ValidationReport validateResponse(final HttpServletRequest servletRequest, final Response response) {
        final Request.Method method = Request.Method.valueOf(servletRequest.getMethod());
        final String path = servletRequest.getServletPath();
        return swaggerRequestResponseValidator.validateResponse(path, method, response);
    }

    private static String readReader(final Reader reader) throws IOException {
        try (Reader reassignedReader = reader) {
            return IOUtils.toString(reassignedReader);
        }
    }

    private static Set<String> getQueryParameterNames(final HttpServletRequest servletRequest) {
        return Stream.of(StringUtils.split(StringUtils.defaultIfBlank(servletRequest.getQueryString(), ""), "&"))
                .map(str -> StringUtils.split(str, "=")[0])
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }
}
