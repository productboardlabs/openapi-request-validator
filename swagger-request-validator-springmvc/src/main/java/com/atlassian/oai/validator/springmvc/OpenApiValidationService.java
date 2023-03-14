package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Body;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;

public class OpenApiValidationService {

    private static final List<String> URI_CHARSETS = resolveAvailableCharsets();
    private final OpenApiInteractionValidator validator;
    private final UrlPathHelper urlPathHelper;

    public OpenApiValidationService(final EncodedResource specAsResource, final UrlPathHelper urlPathHelper) throws IOException {
        this(OpenApiInteractionValidator
                .createForInlineApiSpecification(IOUtils.toString(specAsResource.getReader()))
                .withLevelResolver(SpringMVCLevelResolverFactory.create())
                .build(), urlPathHelper);
    }

    public OpenApiValidationService(final OpenApiInteractionValidator validator, final UrlPathHelper urlPathHelper) {
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
     * @param bodySupplier the {@link Supplier} of a {@link Body} for the request validation
     *
     * @return the build {@link Request} created out of given {@link HttpServletRequest}
     */
    public Request buildRequest(final HttpServletRequest servletRequest, final Supplier<Body> bodySupplier) {
        requireNonNull(servletRequest, "A request is required.");
        requireNonNull(bodySupplier, "A body supplier is required.");

        final Request.Method method = Request.Method.valueOf(servletRequest.getMethod());
        final String path = resolveServletPath(servletRequest);
        final SimpleRequest.Builder builder = new SimpleRequest.Builder(method, path);
        // As a precaution read the query parameters before (!!) the body. The ContentCachingRequestWrapper,
        // used for form data validation, parses the parameters from the bodies input stream. They would be
        // indistinguishable from the form data.
        final Set<String> queryParameterNames = getQueryParameterNames(servletRequest);
        builder.withBody(bodySupplier.get());
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
     */
    public Response buildResponse(final ContentCachingResponseWrapper servletResponse) {
        final int statusCode = servletResponse.getStatusCode();
        final SimpleResponse.Builder builder =
                new SimpleResponse.Builder(statusCode)
                        .withBody(servletResponse.getContentAsByteArray())
                        .withContentType(servletResponse.getContentType());
        for (final String headerName : servletResponse.getHeaderNames()) {
            builder.withHeader(headerName, newArrayList(servletResponse.getHeaders(headerName)));
        }

        return builder.build();
    }

    /**
     * @param request the {@link Request} to validate against the OpenAPI / Swagger specification
     *
     * @return the {@link ValidationReport} for the validated {@link Request}
     */
    public ValidationReport validateRequest(final Request request) {
        return validator.validateRequest(request);
    }

    /**
     * @param servletRequest the {@link HttpServletRequest} to examine the api path to validate against
     * @param response the {@link Response} to validate against the OpenAPI / Swagger specification
     *
     * @return the {@link ValidationReport} for the validated {@link Request}
     */
    public ValidationReport validateResponse(final HttpServletRequest servletRequest,
                                             final Response response) {
        final Request.Method method = Request.Method.valueOf(servletRequest.getMethod());
        final String path = resolveServletPath(servletRequest);
        return validator.validateResponse(path, method, response);
    }

    /**
     * @param servletResponse the {@link HttpServletResponse}
     *
     * @return a map of the headers that are currently set on the response
     */
    Map<String, List<String>> resolveHeadersOnResponse(final HttpServletResponse servletResponse) {
        final Map<String, List<String>> headers = new HashMap<>();
        final Collection<String> headerNames = servletResponse.getHeaderNames();
        if (headerNames != null) {
            for (final String headerName : headerNames) {
                headers.put(headerName, newArrayList(servletResponse.getHeaders(headerName)));
            }
        }
        return Collections.unmodifiableMap(headers);
    }

    /**
     * Will set the given headers on the given response.
     *
     * @param servletResponse the {@link HttpServletResponse}
     * @param headers the headers to add to the response
     */
    void addHeadersToResponse(final HttpServletResponse servletResponse, final Map<String, List<String>> headers) {
        if (headers != null) {
            for (final Map.Entry<String, List<String>> header : headers.entrySet()) {
                final String name = header.getKey();
                for (final String value : header.getValue()) {
                    servletResponse.addHeader(name, value);
                }
            }
        }
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
