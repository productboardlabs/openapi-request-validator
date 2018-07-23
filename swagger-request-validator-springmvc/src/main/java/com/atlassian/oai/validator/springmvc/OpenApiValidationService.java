package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

class OpenApiValidationService {

    private final OpenApiInteractionValidator validator;

    OpenApiValidationService(final EncodedResource specUrlOrPayload) throws IOException {
        this(OpenApiInteractionValidator
                .createFor(readReader(specUrlOrPayload.getReader()))
                .withLevelResolver(SpringMVCLevelResolverFactory.create())
                .build());
    }

    OpenApiValidationService(final OpenApiInteractionValidator validator) {
        requireNonNull(validator, "An OpenAPI validator is required.");
        this.validator = validator;
    }

    /**
     * @param servletRequest the {@link HttpServletRequest}
     *
     * @return the build {@link Request} created out of given {@link HttpServletRequest}
     *
     * @throws IOException if the request body can't be read
     */
    Request buildRequest(final HttpServletRequest servletRequest) throws IOException {
        requireNonNull(servletRequest, "A request is required.");

        final Request.Method method = Request.Method.valueOf(servletRequest.getMethod());
        final UriComponents uriComponents = getUriComponents(servletRequest);
        final String path = uriComponents.getPath();
        final SimpleRequest.Builder builder = new SimpleRequest.Builder(method, path);
        final String body = readReader(servletRequest.getReader());
        // The content length of a request does not need to be set. It might by "-1" and
        // there is still a body. Only in conjunction with an empty / unset body it was
        // really empty.
        if (servletRequest.getContentLength() >= 0 || (body != null && !body.isEmpty())) {
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
     * @param servletResponse the {@link javax.servlet.http.HttpServletResponse} whose body is cached
     *
     * @return the build {@link Response} created out of given {@link ContentCachingResponseWrapper}
     *
     * @throws IOException if the cached response body can't be read
     */
    Response buildResponse(final ContentCachingResponseWrapper servletResponse) throws IOException {
        final int statusCode = servletResponse.getStatusCode();
        final SimpleResponse.Builder builder =
                new SimpleResponse.Builder(statusCode)
                        .withBody(new String(servletResponse.getContentAsByteArray(), servletResponse.getCharacterEncoding()))
                        .withContentType(servletResponse.getContentType());
        for (final String headerName : servletResponse.getHeaderNames()) {
            builder.withHeader(headerName, Lists.newArrayList(servletResponse.getHeaders(headerName)));
        }

        return builder.build();
    }

    /**
     * @param request the {@link Request} to validate against the OpenAPI / Swagger specification
     *
     * @return the {@link ValidationReport} for the validated {@link Request}
     */
    ValidationReport validateRequest(final Request request) {
        return validator.validateRequest(request);
    }

    /**
     * @param servletRequest the {@link HttpServletRequest} to examine the api path to validate against
     * @param response the {@link Response} to validate against the OpenAPI / Swagger specification
     *
     * @return the {@link ValidationReport} for the validated {@link Request}
     */
    ValidationReport validateResponse(final HttpServletRequest servletRequest,
                                      final Response response) {
        final Request.Method method = Request.Method.valueOf(servletRequest.getMethod());
        final String path = getUriComponents(servletRequest).getPath();
        return validator.validateResponse(path, method, response);
    }

    private static String readReader(final Reader reader) throws IOException {
        try (final Reader reassignedReader = reader) {
            return IOUtils.toString(reassignedReader);
        }
    }

    private static UriComponents getUriComponents(final HttpServletRequest servletRequest) {
        final String requestUri = getCompleteRequestUri(servletRequest);
        return UriComponentsBuilder
                .fromUriString(requestUri)
                .build();
    }

    private static String getCompleteRequestUri(final HttpServletRequest servletRequest) {
        if (isBlank(servletRequest.getQueryString())) {
            return servletRequest.getRequestURI();
        }
        return servletRequest.getRequestURI() + "?" + servletRequest.getQueryString();
    }
}
