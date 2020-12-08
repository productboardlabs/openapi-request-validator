package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

class OpenApiValidationService {

    private static final List<String> URI_CHARSETS = resolveAvailableCharsets();
    private final OpenApiInteractionValidator validator;
    private final UrlPathHelper urlPathHelper;

    OpenApiValidationService(final EncodedResource specAsResource, final UrlPathHelper urlPathHelper) throws IOException {
        this(OpenApiInteractionValidator
                .createForInlineApiSpecification(IOUtils.toString(specAsResource.getReader()))
                .withLevelResolver(SpringMVCLevelResolverFactory.create())
                .build(), urlPathHelper);
    }

    OpenApiValidationService(final OpenApiInteractionValidator validator, final UrlPathHelper urlPathHelper) {
        requireNonNull(validator, "An OpenAPI validator is required.");
        this.validator = validator;
        this.urlPathHelper = urlPathHelper;
    }

    private static List<String> resolveAvailableCharsets() {
        final List<String> uriCharsets = Charset.availableCharsets().values().stream()
                .map(Charset::name)
                .collect(Collectors.toList());
        // put UTF-8 to the front as it is the most probable charset for the URI encoding
        uriCharsets.remove(StandardCharsets.UTF_8.name());
        uriCharsets.add(0, StandardCharsets.UTF_8.name());
        return uriCharsets;
    }

    private String resolveServletPath(final HttpServletRequest servletRequest) {
        // The method HttpServletRequest#getServletPath might return NULL even in case there is an actual
        // servlet path. The UrlPathHelper is helping getting the servlet path.
        return urlPathHelper.getPathWithinApplication(servletRequest);
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
        final String path = resolveServletPath(servletRequest);
        final SimpleRequest.Builder builder = new SimpleRequest.Builder(method, path);
        final Set<String> queryParameterNames;
        if (servletRequest instanceof ContentCachingRequestWrapper) {
            // In case of form data - wrapped in a ContentCachingRequestWrapper - the parameters have to be
            // read before (!!) the body. The RequestFacade parses the parameters from the bodies input stream.
            final ContentCachingRequestWrapper wrappedRequest = (ContentCachingRequestWrapper) servletRequest;
            queryParameterNames = getQueryParameterNames(servletRequest);
            builder.withBody(wrappedRequest.getContentAsByteArray());
        } else {
            // In all other cases the parameters have to be read after (!!) the body.
            builder.withBody(servletRequest.getInputStream());
            queryParameterNames = getQueryParameterNames(servletRequest);
        }
        for (final String queryParameterName : queryParameterNames) {
            builder.withQueryParam(queryParameterName, servletRequest.getParameterValues(queryParameterName));
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
        final String path = resolveServletPath(servletRequest);
        return validator.validateResponse(path, method, response);
    }

    private static Set<String> getQueryParameterNames(final HttpServletRequest servletRequest) {
        final List<String> allAvailableNames = Collections.list(servletRequest.getParameterNames());
        return Stream.of(StringUtils.split(StringUtils.defaultIfBlank(servletRequest.getQueryString(), ""), "&"))
                .map(str -> uriDecodeParamName(allAvailableNames, StringUtils.split(str, "=")[0]))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    private static String uriDecodeParamName(final List<String> allParameterNames, final String paramName) {
        // It is difficult to get the correct uri encoding. Ideally it does not need decoding. And if it's encoded better
        // with UTF-8 as charset.
        // Beyond that it's guessing. It can be verified by checking the decoded name with all available parameter names.
        if (!allParameterNames.contains(paramName)) {
            for (final String charset : URI_CHARSETS) {
                final String decoded = uriDecode(paramName, charset);
                if (allParameterNames.contains(decoded)) {
                    return decoded;
                }
            }
        }
        return paramName;
    }

    private static String uriDecode(final String paramName, final String charset) {
        try {
            return URLDecoder.decode(paramName, charset);
        } catch (final UnsupportedEncodingException e) {
            // should not happen as only supported charsets will be used
            return paramName;
        }
    }
}
